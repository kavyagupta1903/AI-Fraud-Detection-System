package service;

import config.AppConfig;
import config.DBConnectionPool;
import dao.FraudAlertDAO;
import dao.TransactionDAO;
import engine.EngineFactory;
import engine.FraudDetectionEngine;
import exception.DatabaseException;
import exception.FraudDetectionException;
import model.CreditCardTransaction;
import model.FraudAlert;
import model.Transaction;
import model.WireTransaction;
import thread.TransactionQueue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Service orchestrating financial transaction lifecycle, validation,
 * synchronous direct evaluations, and asynchronous pipeline dispatch.
 */
public class TransactionService {

    private static final Logger LOGGER = Logger.getLogger(TransactionService.class.getName());

    private final TransactionDAO transactionDAO;
    private final FraudAlertDAO alertDAO;
    private final TransactionQueue queue;
    private final DBConnectionPool pool;

    public TransactionService() {
        this.pool = DBConnectionPool.getInstance();
        this.transactionDAO = new TransactionDAO(pool);
        this.alertDAO = new FraudAlertDAO(pool);
        this.queue = TransactionQueue.getInstance();
    }

    public TransactionService(TransactionDAO transactionDAO, FraudAlertDAO alertDAO,
                              TransactionQueue queue, DBConnectionPool pool) {
        this.transactionDAO = transactionDAO;
        this.alertDAO = alertDAO;
        this.queue = queue;
        this.pool = pool;
    }

    /**
     * Submits a transaction synchronously: evaluates immediately with active engine,
     * persists with manual transaction management, and returns complete evaluation result.
     */
    public Transaction processTransactionSync(Transaction tx) throws FraudDetectionException, DatabaseException {
        validateTransaction(tx);

        if (tx.getTransactionId() == null || tx.getTransactionId().isBlank()) {
            tx.setTransactionId(generateTransactionId(tx.getTransactionType()));
        }

        AppConfig config = AppConfig.getInstance();
        FraudDetectionEngine engine = EngineFactory.getDefaultEngine();

        // Prepare context
        int velocityWindow = config.getVelocityTimeWindowMinutes();
        int recentCount = queue.getRecentTransactionCountForUser(tx.getUserId(), velocityWindow);
        if (recentCount == 0) {
            recentCount = transactionDAO.countTransactionsByUserIdSince(
                    tx.getUserId(),
                    new java.sql.Timestamp(System.currentTimeMillis() - (velocityWindow * 60L * 1000L))
            );
        }

        Map<String, Object> context = new HashMap<>();
        context.put("recentTransactionCount", recentCount);
        context.put("velocityWindowMinutes", velocityWindow);

        // Evaluate
        FraudDetectionEngine.FraudEvaluationResult result = engine.evaluate(tx, context);
        tx.setRiskScore(result.getRiskScore());
        tx.setStatus(result.getStatus());
        tx.setEvaluationNotes(result.getExplanation());
        tx.setDecisionEngine(engine.getEngineName());

        // Pure JDBC Manual Transaction Management
        Connection conn = null;
        try {
            conn = pool.getConnection();
            conn.setAutoCommit(false); // Manual transaction boundary

            transactionDAO.saveWithConnection(conn, tx);

            FraudAlert alert = null;
            if (result.getStatus() == Transaction.Status.BLOCKED || result.getStatus() == Transaction.Status.REVIEW) {
                FraudAlert.Severity severity = (result.getStatus() == Transaction.Status.BLOCKED)
                        ? FraudAlert.Severity.CRITICAL
                        : (result.getRiskScore() >= 0.80 ? FraudAlert.Severity.HIGH : FraudAlert.Severity.MEDIUM);

                alert = new FraudAlert(null, tx.getTransactionId(), tx.getUserId(),
                        result.getExplanation(), severity, false);
                alertDAO.saveWithConnection(conn, alert);
            }

            conn.commit(); // Atomic commit

            // Update thread-safe metrics & cache
            queue.recordEvaluationResult(result.getStatus(), result.getRiskScore(), tx.getAmount());
            if (alert != null) {
                queue.pushAlert(alert);
            }

            return tx;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) { /* ignored */ }
            }
            throw new DatabaseException("Failed to commit transaction processing for TX: " + tx.getTransactionId(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                pool.releaseConnection(conn);
            }
        }
    }

    /**
     * Submits a transaction asynchronously into the producer-consumer pipeline.
     */
    public boolean submitTransactionAsync(Transaction tx) throws FraudDetectionException {
        validateTransaction(tx);
        if (tx.getTransactionId() == null || tx.getTransactionId().isBlank()) {
            tx.setTransactionId(generateTransactionId(tx.getTransactionType()));
        }
        return queue.enqueue(tx);
    }

    public List<Transaction> getUserTransactions(Long userId) throws DatabaseException {
        return transactionDAO.findByUserId(userId);
    }

    public Optional<Transaction> getTransactionById(String transactionId) throws DatabaseException {
        return transactionDAO.findById(transactionId);
    }

    public List<Transaction> getRecentTransactions(int limit) throws DatabaseException {
        return transactionDAO.findRecentTransactions(limit);
    }

    public List<FraudAlert> getUserAlerts(Long userId) throws DatabaseException {
        return alertDAO.findByUserId(userId);
    }

    private void validateTransaction(Transaction tx) throws FraudDetectionException {
        if (tx == null) {
            throw new FraudDetectionException("INVALID_INPUT", "Transaction cannot be null");
        }
        if (tx.getUserId() == null || tx.getUserId() <= 0) {
            throw new FraudDetectionException("INVALID_USER", "Valid User ID is required");
        }
        if (tx.getAmount() == null || tx.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new FraudDetectionException("INVALID_AMOUNT", "Transaction amount must be strictly positive");
        }
        if (tx.getLocation() == null || tx.getLocation().isBlank()) {
            throw new FraudDetectionException("INVALID_LOCATION", "Transaction location is required");
        }

        if (tx instanceof CreditCardTransaction cc) {
            if (cc.getCardNumberLast4() == null || cc.getCardNumberLast4().length() != 4) {
                throw new FraudDetectionException("INVALID_CARD", "Credit card last 4 digits required");
            }
        } else if (tx instanceof WireTransaction wt) {
            if (wt.getRecipientAccount() == null || wt.getRecipientAccount().isBlank()) {
                throw new FraudDetectionException("INVALID_ACCOUNT", "Recipient account is required for wire transfers");
            }
        }
    }

    private String generateTransactionId(Transaction.TransactionType type) {
        String prefix = (type == Transaction.TransactionType.CREDIT_CARD) ? "TX-CC-" : "TX-WT-";
        String uuidPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return prefix + uuidPart;
    }
}
