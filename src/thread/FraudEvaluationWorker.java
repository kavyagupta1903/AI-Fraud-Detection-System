package thread;

import config.AppConfig;
import config.DBConnectionPool;
import dao.FraudAlertDAO;
import dao.TransactionDAO;
import engine.EngineFactory;
import engine.FraudDetectionEngine;
import model.FraudAlert;
import model.Transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Consumer worker thread executing continuous asynchronous fraud evaluations.
 * Implements pure JDBC manual transaction control (commit/rollback) and pushes
 * triage alerts into priority collections.
 */
public class FraudEvaluationWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(FraudEvaluationWorker.class.getName());

    private final TransactionQueue queue;
    private final TransactionDAO transactionDAO;
    private final FraudAlertDAO alertDAO;
    private final DBConnectionPool pool;

    public FraudEvaluationWorker(TransactionQueue queue) {
        this.queue = queue;
        this.pool = DBConnectionPool.getInstance();
        this.transactionDAO = new TransactionDAO(pool);
        this.alertDAO = new FraudAlertDAO(pool);
    }

    @Override
    public void run() {
        LOGGER.info("FraudEvaluationWorker [" + Thread.currentThread().getName() + "] started.");

        while (!Thread.currentThread().isInterrupted()) {
            Transaction tx = null;
            try {
                // Poll bounded queue with timeout to allow graceful thread termination checks
                tx = queue.poll(1, TimeUnit.SECONDS);
                if (tx == null) {
                    continue;
                }

                evaluateAndPersist(tx);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error in FraudEvaluationWorker for TX: " +
                        (tx != null ? tx.getTransactionId() : "N/A"), e);
            }
        }

        LOGGER.info("FraudEvaluationWorker [" + Thread.currentThread().getName() + "] stopped.");
    }

    private void evaluateAndPersist(Transaction tx) {
        long startTime = System.currentTimeMillis();
        AppConfig config = AppConfig.getInstance();

        // 1. Resolve Detection Engine dynamically via Factory Pattern
        FraudDetectionEngine engine = EngineFactory.getDefaultEngine();

        // 2. Prepare Context (Velocity, in-memory sliding window history)
        int velocityWindow = config.getVelocityTimeWindowMinutes();
        int recentCount = queue.getRecentTransactionCountForUser(tx.getUserId(), velocityWindow);

        Map<String, Object> context = new HashMap<>();
        context.put("recentTransactionCount", recentCount);
        context.put("velocityWindowMinutes", velocityWindow);

        Connection conn = null;
        try {
            // 3. Perform Engine Evaluation
            FraudDetectionEngine.FraudEvaluationResult result = engine.evaluate(tx, context);

            tx.setRiskScore(result.getRiskScore());
            tx.setStatus(result.getStatus());
            tx.setEvaluationNotes(result.getExplanation());
            tx.setDecisionEngine(engine.getEngineName());

            // 4. Pure JDBC Manual Transaction Management
            conn = pool.getConnection();
            conn.setAutoCommit(false); // Begin manual transaction

            transactionDAO.saveWithConnection(conn, tx);

            FraudAlert alert = null;
            // 5. Escalate to Alert if suspicious or blocked
            if (result.getStatus() == Transaction.Status.BLOCKED || result.getStatus() == Transaction.Status.REVIEW) {
                FraudAlert.Severity severity;
                if (result.getStatus() == Transaction.Status.BLOCKED) {
                    severity = FraudAlert.Severity.CRITICAL;
                } else if (result.getRiskScore() >= 0.80) {
                    severity = FraudAlert.Severity.HIGH;
                } else {
                    severity = FraudAlert.Severity.MEDIUM;
                }

                alert = new FraudAlert(null, tx.getTransactionId(), tx.getUserId(),
                        result.getExplanation(), severity, false);

                alertDAO.saveWithConnection(conn, alert);
            }

            // Commit atomic transaction
            conn.commit();

            // 6. Record Thread-safe Metrics
            queue.recordEvaluationResult(result.getStatus(), result.getRiskScore(), tx.getAmount());
            if (alert != null) {
                queue.pushAlert(alert);
            }

            long latency = System.currentTimeMillis() - startTime;
            LOGGER.info(String.format("Async Evaluation Finished for TX: %s in %dms | Engine: %s | Risk: %.2f | Status: %s",
                    tx.getTransactionId(), latency, engine.getEngineName(), tx.getRiskScore(), tx.getStatus()));

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on failure
                    LOGGER.log(Level.WARNING, "Rolled back transaction for TX: " + tx.getTransactionId());
                } catch (SQLException se) {
                    LOGGER.log(Level.SEVERE, "Failed rollback on connection", se);
                }
            }
            LOGGER.log(Level.SEVERE, "Evaluation and persistence failed for TX: " + tx.getTransactionId(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                pool.releaseConnection(conn);
            }
        }
    }
}
