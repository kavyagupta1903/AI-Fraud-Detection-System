package dao;

import config.DBConnectionPool;
import exception.DatabaseException;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pure JDBC implementation of User DAO.
 * Uses PreparedStatements exclusively to eliminate SQL injection vulnerabilities.
 */
public class UserDAO implements GenericDAO<User, Long> {

    private final DBConnectionPool connectionPool;

    public UserDAO() {
        this.connectionPool = DBConnectionPool.getInstance();
    }

    public UserDAO(DBConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    public User save(User user) throws DatabaseException {
        String sql = "INSERT INTO users (username, password_hash, role, email, created_at) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = connectionPool.getConnection();
            conn.setAutoCommit(false); // Manual transaction management

            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());
            ps.setString(4, user.getEmail());
            ps.setTimestamp(5, user.getCreatedAt());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                conn.rollback();
                throw new DatabaseException("Creating user failed, no rows affected.");
            }

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                user.setUserId(rs.getLong(1));
            }
            conn.commit(); // Transaction committed
            return user;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) { /* ignored */ }
            }
            throw new DatabaseException("Failed to persist user: " + user.getUsername(), e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                connectionPool.releaseConnection(conn);
            }
        }
    }

    @Override
    public Optional<User> findById(Long userId) throws DatabaseException {
        String sql = "SELECT user_id, username, password_hash, role, email, created_at FROM users WHERE user_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = connectionPool.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query user by ID: " + userId, e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) connectionPool.releaseConnection(conn);
        }
    }

    public Optional<User> findByUsername(String username) throws DatabaseException {
        String sql = "SELECT user_id, username, password_hash, role, email, created_at FROM users WHERE username = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = connectionPool.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query user by username: " + username, e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) connectionPool.releaseConnection(conn);
        }
    }

    @Override
    public List<User> findAll() throws DatabaseException {
        String sql = "SELECT user_id, username, password_hash, role, email, created_at FROM users ORDER BY user_id ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<User> users = new ArrayList<>();

        try {
            conn = connectionPool.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query all users", e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) connectionPool.releaseConnection(conn);
        }
    }

    @Override
    public boolean update(User user) throws DatabaseException {
        String sql = "UPDATE users SET username = ?, password_hash = ?, role = ?, email = ? WHERE user_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = connectionPool.getConnection();
            conn.setAutoCommit(false);

            ps = conn.prepareStatement(sql);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());
            ps.setString(4, user.getEmail());
            ps.setLong(5, user.getUserId());

            int updated = ps.executeUpdate();
            conn.commit();
            return updated > 0;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) { /* ignored */ }
            }
            throw new DatabaseException("Failed to update user: " + user.getUserId(), e);
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                connectionPool.releaseConnection(conn);
            }
        }
    }

    @Override
    public boolean delete(Long userId) throws DatabaseException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = connectionPool.getConnection();
            conn.setAutoCommit(false);

            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);

            int deleted = ps.executeUpdate();
            conn.commit();
            return deleted > 0;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) { /* ignored */ }
            }
            throw new DatabaseException("Failed to delete user: " + userId, e);
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                connectionPool.releaseConnection(conn);
            }
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getLong("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(User.Role.valueOf(rs.getString("role")));
        user.setEmail(rs.getString("email"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }
}
