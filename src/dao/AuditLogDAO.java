package dao;

import config.DBConnectionPool;
import exception.DatabaseException;
import model.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pure JDBC Data Access Object for regulatory Audit Logs.
 */
public class AuditLogDAO implements GenericDAO<AuditLog, Long> {

    private final DBConnectionPool pool;

    public AuditLogDAO() {
        this.pool = DBConnectionPool.getInstance();
    }

    public AuditLogDAO(DBConnectionPool pool) {
        this.pool = pool;
    }

    @Override
    public AuditLog save(AuditLog log) throws DatabaseException {
        Connection conn = null;
        try {
            conn = pool.getConnection();
            conn.setAutoCommit(false);
            saveWithConnection(conn, log);
            conn.commit();
            return log;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new DatabaseException("Failed to persist audit log entry", e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                pool.releaseConnection(conn);
            }
        }
    }

    public void saveWithConnection(Connection conn, AuditLog log) throws SQLException {
        String sql = "INSERT INTO audit_logs (action_type, performed_by, target_id, details, timestamp) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, log.getActionType());
            ps.setString(2, log.getPerformedBy());
            ps.setString(3, log.getTargetId());
            ps.setString(4, log.getDetails());
            ps.setTimestamp(5, log.getTimestamp());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    log.setAuditId(rs.getLong(1));
                }
            }
        }
    }

    @Override
    public Optional<AuditLog> findById(Long auditId) throws DatabaseException {
        String sql = "SELECT * FROM audit_logs WHERE audit_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = pool.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, auditId);
            rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToAuditLog(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query audit log by ID: " + auditId, e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) pool.releaseConnection(conn);
        }
    }

    @Override
    public List<AuditLog> findAll() throws DatabaseException {
        return findRecent(200);
    }

    public List<AuditLog> findRecent(int limit) throws DatabaseException {
        String sql = "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<AuditLog> list = new ArrayList<>();

        try {
            conn = pool.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, limit);
            rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapResultSetToAuditLog(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query recent audit logs", e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) pool.releaseConnection(conn);
        }
    }

    @Override
    public boolean update(AuditLog entity) throws DatabaseException {
        // Audit logs are strictly immutable for regulatory compliance
        throw new UnsupportedOperationException("Audit logs are append-only and cannot be modified.");
    }

    @Override
    public boolean delete(Long id) throws DatabaseException {
        // Audit logs are append-only
        throw new UnsupportedOperationException("Audit logs are append-only and cannot be deleted.");
    }

    private AuditLog mapResultSetToAuditLog(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setAuditId(rs.getLong("audit_id"));
        log.setActionType(rs.getString("action_type"));
        log.setPerformedBy(rs.getString("performed_by"));
        log.setTargetId(rs.getString("target_id"));
        log.setDetails(rs.getString("details"));
        log.setTimestamp(rs.getTimestamp("timestamp"));
        return log;
    }
}
