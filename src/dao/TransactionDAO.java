package dao;

import config.DBConnectionPool;
import exception.DatabaseException;
import model.CreditCardTransaction;
import model.Transaction;
import model.WireTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pure JDBC Implementation of Transaction DAO supporting polymorphic persistence
 * for CreditCardTransaction and WireTransaction.
 */
public class TransactionDAO implements GenericDAO<Transaction, String> {

    private final DBConnectionPool connectionPool;

    public TransactionDAO() {
        this.connectionPool = DBConnectionPool.getInstance();
    }

    public TransactionDAO(DBConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    public Transaction save(Transaction tx) throws DatabaseException {
        Connection conn = null;
        try {
            conn = connectionPool.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            saveWithConnection(conn, tx);

            conn.commit(); // Commit transaction
            return tx;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) { /* ignored */ }
            }
            throw new DatabaseException("Failed to persist transaction: " + tx.getTransactionId(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                connectionPool.releaseConnection(conn);
            }
        }
    }

    /**
     * Persists transaction within an existing caller-managed JDBC transaction.
     */
    public void saveWithConnection(Connection conn, Transaction tx) throws SQLException {
        String sql = "MERGE INTO transactions (" +
                "transaction_id, user_id, amount, currency, transaction_type, location, ip_address, " +
                "device_fingerprint, category, timestamp, risk_score, status, " +
                "card_number_last4, card_network, cvv_verified, billing_zip, " +
                "recipient_bank, recipient_account, swift_code, wire_purpose, " +
                "evaluation_notes, decision_engine) KEY(transaction_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tx.getTransactionId());
            ps.setLong(2, tx.getUserId());
            ps.setBigDecimal(3, tx.getAmount());
            ps.setString(4, tx.getCurrency());
            ps.setString(5, tx.getTransactionType().name());
            ps.setString(6, tx.getLocation());
            ps.setString(7, tx.getIpAddress());
            ps.setString(8, tx.getDeviceFingerprint());
            ps.setString(9, tx.getCategory());
            ps.setTimestamp(10, tx.getTimestamp());
            ps.setDouble(11, tx.getRiskScore());
            ps.setString(12, tx.getStatus().name());

            // Polymorphic field handling
            if (tx instanceof CreditCardTransaction) {
                CreditCardTransaction cc = (CreditCardTransaction) tx;
                ps.setString(13, cc.getCardNumberLast4());
                ps.setString(14, cc.getCardNetwork());
                ps.setBoolean(15, cc.isCvvVerified());
                ps.setString(16, cc.getBillingZip());

                ps.setNull(17, java.sql.Types.VARCHAR);
                ps.setNull(18, java.sql.Types.VARCHAR);
                ps.setNull(19, java.sql.Types.VARCHAR);
                ps.setNull(20, java.sql.Types.VARCHAR);
            } else if (tx instanceof WireTransaction) {
                WireTransaction wt = (WireTransaction) tx;
                ps.setNull(13, java.sql.Types.VARCHAR);
                ps.setNull(14, java.sql.Types.VARCHAR);
                ps.setNull(15, java.sql.Types.BOOLEAN);
                ps.setNull(16, java.sql.Types.VARCHAR);

                ps.setString(17, wt.getRecipientBank());
                ps.setString(18, wt.getRecipientAccount());
                ps.setString(19, wt.getSwiftCode());
                ps.setString(20, wt.getWirePurpose());
            } else {
                ps.setNull(13, java.sql.Types.VARCHAR);
                ps.setNull(14, java.sql.Types.VARCHAR);
                ps.setNull(15, java.sql.Types.BOOLEAN);
                ps.setNull(16, java.sql.Types.VARCHAR);
                ps.setNull(17, java.sql.Types.VARCHAR);
                ps.setNull(18, java.sql.Types.VARCHAR);
                ps.setNull(19, java.sql.Types.VARCHAR);
                ps.setNull(20, java.sql.Types.VARCHAR);
            }

            ps.setString(21, tx.getEvaluationNotes());
            ps.setString(22, tx.getDecisionEngine());

            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Transaction> findById(String transactionId) throws DatabaseException {
        String sql = "SELECT * FROM transactions WHERE transaction_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = connectionPool.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, transactionId);
            rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToTransaction(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query transaction by ID: " + transactionId, e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) connectionPool.releaseConnection(conn);
        }
    }

    @Override
    public List<Transaction> findAll() throws DatabaseException {
        return findRecentTransactions(500);
    }

    public List<Transaction> findRecentTransactions(int limit) throws DatabaseException {
        String sql = "SELECT * FROM transactions ORDER BY timestamp DESC LIMIT ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Transaction> list = new ArrayList<>();

        try {
            conn = connectionPool.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, limit);
            rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapResultSetToTransaction(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query recent transactions", e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) connectionPool.releaseConnection(conn);
        }
    }

    public List<Transaction> findByUserId(Long userId) throws DatabaseException {
        String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY timestamp DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Transaction> list = new ArrayList<>();

        try {
            conn = connectionPool.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapResultSetToTransaction(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query transactions for user: " + userId, e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) connectionPool.releaseConnection(conn);
        }
    }

    public List<Transaction> findByStatus(Transaction.Status status) throws DatabaseException {
        String sql = "SELECT * FROM transactions WHERE status = ? ORDER BY timestamp DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Transaction> list = new ArrayList<>();

        try {
            conn = connectionPool.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status.name());
            rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapResultSetToTransaction(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query transactions by status: " + status, e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) connectionPool.releaseConnection(conn);
        }
    }

    /**
     * Counts how many transactions a user performed since a given timestamp (used in velocity checks).
     */
    public int countTransactionsByUserIdSince(Long userId, Timestamp since) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM transactions WHERE user_id = ? AND timestamp >= ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = connectionPool.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            ps.setTimestamp(2, since);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count user transaction velocity", e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) connectionPool.releaseConnection(conn);
        }
    }

    /**
     * Atomic status and risk update for review decisions.
     */
    public boolean updateStatusAndRisk(String transactionId, double riskScore,
                                       Transaction.Status status, String notes, String engine)
            throws DatabaseException {
        String sql = "UPDATE transactions SET risk_score = ?, status = ?, evaluation_notes = ?, decision_engine = ? " +
                     "WHERE transaction_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = connectionPool.getConnection();
            conn.setAutoCommit(false);

            ps = conn.prepareStatement(sql);
            ps.setDouble(1, riskScore);
            ps.setString(2, status.name());
            ps.setString(3, notes);
            ps.setString(4, engine);
            ps.setString(5, transactionId);

            int updated = ps.executeUpdate();
            conn.commit();
            return updated > 0;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) { /* ignored */ }
            }
            throw new DatabaseException("Failed to update transaction status: " + transactionId, e);
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                connectionPool.releaseConnection(conn);
            }
        }
    }

    @Override
    public boolean update(Transaction tx) throws DatabaseException {
        return updateStatusAndRisk(tx.getTransactionId(), tx.getRiskScore(), tx.getStatus(),
                tx.getEvaluationNotes(), tx.getDecisionEngine());
    }

    @Override
    public boolean delete(String transactionId) throws DatabaseException {
        String sql = "DELETE FROM transactions WHERE transaction_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = connectionPool.getConnection();
            conn.setAutoCommit(false);

            ps = conn.prepareStatement(sql);
            ps.setString(1, transactionId);

            int deleted = ps.executeUpdate();
            conn.commit();
            return deleted > 0;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) { /* ignored */ }
            }
            throw new DatabaseException("Failed to delete transaction: " + transactionId, e);
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                connectionPool.releaseConnection(conn);
            }
        }
    }

    /**
     * Polymorphic factory mapping ResultSet row to concrete Transaction subclass.
     */
    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        String typeStr = rs.getString("transaction_type");
        Transaction.TransactionType type = Transaction.TransactionType.valueOf(typeStr);

        Transaction tx;
        if (type == Transaction.TransactionType.CREDIT_CARD) {
            CreditCardTransaction cc = new CreditCardTransaction();
            cc.setCardNumberLast4(rs.getString("card_number_last4"));
            cc.setCardNetwork(rs.getString("card_network"));
            cc.setCvvVerified(rs.getBoolean("cvv_verified"));
            cc.setBillingZip(rs.getString("billing_zip"));
            tx = cc;
        } else {
            WireTransaction wt = new WireTransaction();
            wt.setRecipientBank(rs.getString("recipient_bank"));
            wt.setRecipientAccount(rs.getString("recipient_account"));
            wt.setSwiftCode(rs.getString("swift_code"));
            wt.setWirePurpose(rs.getString("wire_purpose"));
            tx = wt;
        }

        tx.setTransactionId(rs.getString("transaction_id"));
        tx.setUserId(rs.getLong("user_id"));
        tx.setAmount(rs.getBigDecimal("amount"));
        tx.setCurrency(rs.getString("currency"));
        tx.setTransactionType(type);
        tx.setLocation(rs.getString("location"));
        tx.setIpAddress(rs.getString("ip_address"));
        tx.setDeviceFingerprint(rs.getString("device_fingerprint"));
        tx.setCategory(rs.getString("category"));
        tx.setTimestamp(rs.getTimestamp("timestamp"));
        tx.setRiskScore(rs.getDouble("risk_score"));
        tx.setStatus(Transaction.Status.valueOf(rs.getString("status")));
        tx.setEvaluationNotes(rs.getString("evaluation_notes"));
        tx.setDecisionEngine(rs.getString("decision_engine"));

        return tx;
    }
}
