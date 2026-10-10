package dao;

import config.DBConnectionPool;
import exception.DatabaseException;
import model.FraudAlert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pure JDBC Implementation of FraudAlert DAO.
 */
public class FraudAlertDAO implements GenericDAO<FraudAlert, Long> {

    private final DBConnectionPool connectionPool;

    public FraudAlertDAO() {
        this.connectionPool = DBConnectionPool.getInstance();
    }

    public FraudAlertDAO(DBConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    public FraudAlert save(FraudAlert alert) throws DatabaseException {
        Connection conn = null;
        try {
            conn = connectionPool.getConnection();
            conn.setAutoCommit(false);

            saveWithConnection(conn, alert);

            conn.commit();
            return alert;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) { /* ignored */ }
            }
            throw new DatabaseException("Failed to persist fraud alert", e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                connectionPool.releaseConnection(conn);
            }
        }
    }

    public void saveWithConnection(Connection conn, FraudAlert alert) throws SQLException {
        String sql = "INSERT INTO fraud_alerts (transaction_id, user_id, alert_reason, severity, is_read, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, alert.getTransactionId());
            ps.setLong(2, alert.getUserId());
            ps.setString(3, alert.getAlertReason());
            ps.setString(4, alert.getSeverity().name());
            ps.setBoolean(5, alert.isRead());
            ps.setTimestamp(6, alert.getCreatedAt());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    alert.setAlertId(rs.getLong(1));
                }
            }
        }
    }

    @Override
    public Optional<FraudAlert> findById(Long alertId) throws DatabaseException {
        String sql = "SELECT * FROM fraud_alerts WHERE alert_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = connectionPool.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, alertId);
            rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToAlert(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query alert: " + alertId, e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) connectionPool.releaseConnection(conn);
        }
    }

    @Override
    public List<FraudAlert> findAll() throws DatabaseException {
        return findRecentAlerts(200);
    }

    public List<FraudAlert> findRecentAlerts(int limit) throws DatabaseException {
        String sql = "SELECT * FROM fraud_alerts ORDER BY created_at DESC LIMIT ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<FraudAlert> list = new ArrayList<>();

        try {
            conn = connectionPool.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, limit);
            rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapResultSetToAlert(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query recent alerts", e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) connectionPool.releaseConnection(conn);
        }
    }

    public List<FraudAlert> findByUserId(Long userId) throws DatabaseException {
        String sql = "SELECT * FROM fraud_alerts WHERE user_id = ? ORDER BY created_at DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<FraudAlert> list = new ArrayList<>();

        try {
            conn = connectionPool.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapResultSetToAlert(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query alerts for user: " + userId, e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) connectionPool.releaseConnection(conn);
        }
    }

    public List<FraudAlert> findUnread() throws DatabaseException {
        String sql = "SELECT * FROM fraud_alerts WHERE is_read = FALSE ORDER BY created_at DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<FraudAlert> list = new ArrayList<>();

        try {
            conn = connectionPool.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapResultSetToAlert(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query unread alerts", e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) connectionPool.releaseConnection(conn);
        }
    }

    public boolean markAsRead(Long alertId) throws DatabaseException {
        String sql = "UPDATE fraud_alerts SET is_read = TRUE WHERE alert_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = connectionPool.getConnection();
            conn.setAutoCommit(false);

            ps = conn.prepareStatement(sql);
            ps.setLong(1, alertId);
            int updated = ps.executeUpdate();
            conn.commit();
            return updated > 0;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) { /* ignored */ }
            }
            throw new DatabaseException("Failed to mark alert as read: " + alertId, e);
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                connectionPool.releaseConnection(conn);
            }
        }
    }

    @Override
    public boolean update(FraudAlert alert) throws DatabaseException {
        String sql = "UPDATE fraud_alerts SET alert_reason = ?, severity = ?, is_read = ? WHERE alert_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = connectionPool.getConnection();
            conn.setAutoCommit(false);

            ps = conn.prepareStatement(sql);
            ps.setString(1, alert.getAlertReason());
            ps.setString(2, alert.getSeverity().name());
            ps.setBoolean(3, alert.isRead());
            ps.setLong(4, alert.getAlertId());

            int updated = ps.executeUpdate();
            conn.commit();
            return updated > 0;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) { /* ignored */ }
            }
            throw new DatabaseException("Failed to update alert: " + alert.getAlertId(), e);
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                connectionPool.releaseConnection(conn);
            }
        }
    }

    @Override
    public boolean delete(Long alertId) throws DatabaseException {
        String sql = "DELETE FROM fraud_alerts WHERE alert_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = connectionPool.getConnection();
            conn.setAutoCommit(false);

            ps = conn.prepareStatement(sql);
            ps.setLong(1, alertId);
            int deleted = ps.executeUpdate();
            conn.commit();
            return deleted > 0;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) { /* ignored */ }
            }
            throw new DatabaseException("Failed to delete alert: " + alertId, e);
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                connectionPool.releaseConnection(conn);
            }
        }
    }

    private FraudAlert mapResultSetToAlert(ResultSet rs) throws SQLException {
        FraudAlert alert = new FraudAlert();
        alert.setAlertId(rs.getLong("alert_id"));
        alert.setTransactionId(rs.getString("transaction_id"));
        alert.setUserId(rs.getLong("user_id"));
        alert.setAlertReason(rs.getString("alert_reason"));
        alert.setSeverity(FraudAlert.Severity.valueOf(rs.getString("severity")));
        alert.setRead(rs.getBoolean("is_read"));
        alert.setCreatedAt(rs.getTimestamp("created_at"));
        return alert;
    }
}
