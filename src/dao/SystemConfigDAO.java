package dao;

import config.DBConnectionPool;
import exception.DatabaseException;
import model.SystemConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pure JDBC Implementation of SystemConfig DAO for threshold and weight management.
 */
public class SystemConfigDAO implements GenericDAO<SystemConfig, String> {

    private final DBConnectionPool connectionPool;

    public SystemConfigDAO() {
        this.connectionPool = DBConnectionPool.getInstance();
    }

    public SystemConfigDAO(DBConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    public SystemConfig save(SystemConfig config) throws DatabaseException {
        upsert(config.getConfigKey(), config.getConfigValue(), config.getUpdatedBy());
        return config;
    }

    public void upsert(String key, String value, String updatedBy) throws DatabaseException {
        String sql = "MERGE INTO system_configs (config_key, config_value, updated_by, updated_at) " +
                     "KEY(config_key) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = connectionPool.getConnection();
            conn.setAutoCommit(false);

            ps = conn.prepareStatement(sql);
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, updatedBy != null ? updatedBy : "SYSTEM");

            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) { /* ignored */ }
            }
            throw new DatabaseException("Failed to upsert system config for key: " + key, e);
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                connectionPool.releaseConnection(conn);
            }
        }
    }

    @Override
    public Optional<SystemConfig> findById(String key) throws DatabaseException {
        String sql = "SELECT config_key, config_value, updated_by, updated_at FROM system_configs WHERE config_key = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = connectionPool.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, key);
            rs = ps.executeQuery();

            if (rs.next()) {
                SystemConfig sc = new SystemConfig();
                sc.setConfigKey(rs.getString("config_key"));
                sc.setConfigValue(rs.getString("config_value"));
                sc.setUpdatedBy(rs.getString("updated_by"));
                sc.setUpdatedAt(rs.getTimestamp("updated_at"));
                return Optional.of(sc);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find config key: " + key, e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) connectionPool.releaseConnection(conn);
        }
    }

    @Override
    public List<SystemConfig> findAll() throws DatabaseException {
        String sql = "SELECT config_key, config_value, updated_by, updated_at FROM system_configs ORDER BY config_key ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<SystemConfig> list = new ArrayList<>();

        try {
            conn = connectionPool.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                SystemConfig sc = new SystemConfig();
                sc.setConfigKey(rs.getString("config_key"));
                sc.setConfigValue(rs.getString("config_value"));
                sc.setUpdatedBy(rs.getString("updated_by"));
                sc.setUpdatedAt(rs.getTimestamp("updated_at"));
                list.add(sc);
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find all system configs", e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) connectionPool.releaseConnection(conn);
        }
    }

    public Map<String, String> getAllAsMap() throws DatabaseException {
        List<SystemConfig> all = findAll();
        Map<String, String> map = new HashMap<>();
        for (SystemConfig sc : all) {
            map.put(sc.getConfigKey(), sc.getConfigValue());
        }
        return map;
    }

    @Override
    public boolean update(SystemConfig entity) throws DatabaseException {
        upsert(entity.getConfigKey(), entity.getConfigValue(), entity.getUpdatedBy());
        return true;
    }

    @Override
    public boolean delete(String key) throws DatabaseException {
        String sql = "DELETE FROM system_configs WHERE config_key = ?";
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = connectionPool.getConnection();
            conn.setAutoCommit(false);

            ps = conn.prepareStatement(sql);
            ps.setString(1, key);
            int deleted = ps.executeUpdate();
            conn.commit();
            return deleted > 0;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) { /* ignored */ }
            }
            throw new DatabaseException("Failed to delete config: " + key, e);
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                connectionPool.releaseConnection(conn);
            }
        }
    }
}
