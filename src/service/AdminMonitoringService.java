package service;

import config.AppConfig;
import config.DBConnectionPool;
import dao.FraudAlertDAO;
import dao.SystemConfigDAO;
import dao.TransactionDAO;
import exception.DatabaseException;
import exception.ThresholdOutOfBoundsException;
import model.FraudAlert;
import model.Transaction;
import thread.TransactionQueue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service providing Admin operations: dynamic configuration tuning,
 * detection oversight, transaction triage, and performance metrics analysis.
 */
public class AdminMonitoringService {

    private static final Logger LOGGER = Logger.getLogger(AdminMonitoringService.class.getName());

    private final TransactionDAO transactionDAO;
    private final FraudAlertDAO alertDAO;
    private final SystemConfigDAO configDAO;
    private final dao.AuditLogDAO auditLogDAO;
    private final AppConfig appConfig;
    private final TransactionQueue queue;
    private final DBConnectionPool pool;

    public AdminMonitoringService() {
        this.pool = DBConnectionPool.getInstance();
        this.transactionDAO = new TransactionDAO(pool);
        this.alertDAO = new FraudAlertDAO(pool);
        this.configDAO = new SystemConfigDAO(pool);
        this.auditLogDAO = new dao.AuditLogDAO(pool);
        this.appConfig = AppConfig.getInstance();
        this.queue = TransactionQueue.getInstance();
    }

    public AdminMonitoringService(TransactionDAO transactionDAO, FraudAlertDAO alertDAO,
                                  SystemConfigDAO configDAO, dao.AuditLogDAO auditLogDAO,
                                  AppConfig appConfig,
                                  TransactionQueue queue, DBConnectionPool pool) {
        this.transactionDAO = transactionDAO;
        this.alertDAO = alertDAO;
        this.configDAO = configDAO;
        this.auditLogDAO = auditLogDAO;
        this.appConfig = appConfig;
        this.queue = queue;
        this.pool = pool;
    }

    // --- System Configuration Methods ---

    public void updateRiskThresholds(double flagThreshold, double blockThreshold, String updatedBy)
            throws ThresholdOutOfBoundsException, DatabaseException {
        if (flagThreshold < 0.0 || flagThreshold > 1.0) {
            throw new ThresholdOutOfBoundsException("Flag threshold must be between 0.0 and 1.0", flagThreshold, 0.0, 1.0);
        }
        if (blockThreshold < 0.0 || blockThreshold > 1.0) {
            throw new ThresholdOutOfBoundsException("Block threshold must be between 0.0 and 1.0", blockThreshold, 0.0, 1.0);
        }
        if (flagThreshold >= blockThreshold) {
            throw new ThresholdOutOfBoundsException("Flag threshold cannot equal or exceed block threshold", flagThreshold, 0.0, blockThreshold);
        }

        appConfig.updateConfig("flag_threshold", String.format("%.2f", flagThreshold), updatedBy);
        appConfig.updateConfig("block_threshold", String.format("%.2f", blockThreshold), updatedBy);
        try {
            auditLogDAO.save(new model.AuditLog(null, "CONFIG_UPDATE", updatedBy, "THRESHOLDS",
                    String.format("Updated flagThreshold=%.2f, blockThreshold=%.2f", flagThreshold, blockThreshold)));
        } catch (Exception e) {
            LOGGER.warning("Failed to record audit log for threshold update: " + e.getMessage());
        }
    }

    public void updateAlgorithmWeights(double weightAmount, double weightLocation,
                                        double weightDevice, double weightVelocity,
                                        String updatedBy) throws ThresholdOutOfBoundsException, DatabaseException {
        validateWeight("Amount", weightAmount);
        validateWeight("Location", weightLocation);
        validateWeight("Device", weightDevice);
        validateWeight("Velocity", weightVelocity);

        appConfig.updateConfig("ml_weight_amount", String.format("%.2f", weightAmount), updatedBy);
        appConfig.updateConfig("ml_weight_location", String.format("%.2f", weightLocation), updatedBy);
        appConfig.updateConfig("ml_weight_device", String.format("%.2f", weightDevice), updatedBy);
        appConfig.updateConfig("ml_weight_velocity", String.format("%.2f", weightVelocity), updatedBy);
        try {
            auditLogDAO.save(new model.AuditLog(null, "CONFIG_UPDATE", updatedBy, "ML_WEIGHTS",
                    String.format("Updated weights: amt=%.2f, loc=%.2f, dev=%.2f, vel=%.2f",
                            weightAmount, weightLocation, weightDevice, weightVelocity)));
        } catch (Exception e) {
            LOGGER.warning("Failed to record audit log for weights update: " + e.getMessage());
        }
    }

    public void setActiveEngine(String engineName, String updatedBy) throws DatabaseException, ThresholdOutOfBoundsException {
        if (engineName == null || (!engineName.equalsIgnoreCase("RULE_BASED") &&
                !engineName.equalsIgnoreCase("ML_ANOMALY") &&
                !engineName.equalsIgnoreCase("HYBRID"))) {
            throw new IllegalArgumentException("Unknown engine: " + engineName);
        }
        appConfig.updateConfig("active_engine", engineName.toUpperCase().trim(), updatedBy);
        try {
            auditLogDAO.save(new model.AuditLog(null, "ENGINE_SWITCH", updatedBy, "ENGINE",
                    "Switched active detection engine to " + engineName.toUpperCase()));
        } catch (Exception e) {
            LOGGER.warning("Failed to record audit log for engine switch: " + e.getMessage());
        }
    }

    private void validateWeight(String name, double weight) throws ThresholdOutOfBoundsException {
        if (weight < 0.0 || weight > 1.0) {
            throw new ThresholdOutOfBoundsException(name + " weight must be between 0.0 and 1.0", weight, 0.0, 1.0);
        }
    }

    // --- Detection Monitoring & Actions ---

    public List<Transaction> getTransactionsFiltered(String statusFilter) throws DatabaseException {
        if (statusFilter == null || statusFilter.equalsIgnoreCase("ALL") || statusFilter.isBlank()) {
            return transactionDAO.findRecentTransactions(200);
        }
        try {
            Transaction.Status status = Transaction.Status.valueOf(statusFilter.toUpperCase().trim());
            return transactionDAO.findByStatus(status);
        } catch (IllegalArgumentException e) {
            return transactionDAO.findRecentTransactions(200);
        }
    }

    public boolean reviewApproveTransaction(String transactionId, String adminUsername) throws DatabaseException {
        Optional<Transaction> opt = transactionDAO.findById(transactionId);
        if (opt.isEmpty()) return false;

        Transaction tx = opt.get();
        // If this transaction was previously marked as REVIEW or BLOCKED, approving counts as a false positive
        if (tx.getStatus() == Transaction.Status.REVIEW || tx.getStatus() == Transaction.Status.BLOCKED) {
            queue.incrementFalsePositive();
        }

        String notes = "Manually APPROVED by Admin: " + adminUsername + " (Overrode prior status: " + tx.getStatus() + ")";
        boolean success = transactionDAO.updateStatusAndRisk(transactionId, tx.getRiskScore(),
                Transaction.Status.APPROVED, notes, tx.getDecisionEngine());
        if (success) {
            try {
                auditLogDAO.save(new model.AuditLog(null, "MANUAL_APPROVE", adminUsername, transactionId,
                        "Overrode status " + tx.getStatus() + " -> APPROVED (Risk: " + tx.getRiskScore() + ")"));
            } catch (Exception e) {
                LOGGER.warning("Failed to record audit log for manual approval: " + e.getMessage());
            }
        }
        return success;
    }

    public boolean reviewBlockTransaction(String transactionId, String adminUsername, String reason) throws DatabaseException {
        Optional<Transaction> opt = transactionDAO.findById(transactionId);
        if (opt.isEmpty()) return false;

        Transaction tx = opt.get();
        String notes = "Manually BLOCKED by Admin: " + adminUsername + ". Reason: " + (reason != null ? reason : "Confirmed Fraud");
        boolean success = transactionDAO.updateStatusAndRisk(transactionId, 1.0,
                Transaction.Status.BLOCKED, notes, tx.getDecisionEngine());
        if (success) {
            try {
                auditLogDAO.save(new model.AuditLog(null, "MANUAL_BLOCK", adminUsername, transactionId,
                        "Overrode status " + tx.getStatus() + " -> BLOCKED. Reason: " + (reason != null ? reason : "Fraud Confirmed")));
            } catch (Exception e) {
                LOGGER.warning("Failed to record audit log for manual block: " + e.getMessage());
            }
        }
        return success;
    }

    public boolean reviewInvestigateTransaction(String transactionId, String adminUsername) throws DatabaseException {
        Optional<Transaction> opt = transactionDAO.findById(transactionId);
        if (opt.isEmpty()) return false;

        Transaction tx = opt.get();
        String notes = "Under Investigation by Admin: " + adminUsername;
        boolean success = transactionDAO.updateStatusAndRisk(transactionId, tx.getRiskScore(),
                Transaction.Status.REVIEW, notes, tx.getDecisionEngine());
        if (success) {
            try {
                auditLogDAO.save(new model.AuditLog(null, "INVESTIGATION_FLAG", adminUsername, transactionId,
                        "Flagged under investigation. Current risk: " + tx.getRiskScore()));
            } catch (Exception e) {
                LOGGER.warning("Failed to record audit log for investigation flag: " + e.getMessage());
            }
        }
        return success;
    }

    public List<model.AuditLog> getRecentAuditLogs(int limit) throws DatabaseException {
        return auditLogDAO.findRecent(limit);
    }

    public List<Transaction> getAllTransactions() throws DatabaseException {
        return transactionDAO.findAll();
    }

    public List<FraudAlert> getAllAlerts() throws DatabaseException {
        return alertDAO.findRecentAlerts(200);
    }

    public boolean markAlertAsRead(Long alertId) throws DatabaseException {
        return alertDAO.markAsRead(alertId);
    }

    // --- Performance Metrics & Diagnostics ---

    public Map<String, Object> getSystemMetricsSnapshot() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalProcessed", queue.getTotalProcessed());
        metrics.put("totalApproved", queue.getTotalApproved());
        metrics.put("totalFlagged", queue.getTotalFlagged());
        metrics.put("totalBlocked", queue.getTotalBlocked());
        metrics.put("falsePositives", queue.getFalsePositives());
        metrics.put("accuracyRate", String.format("%.2f%%", queue.getAccuracyRate()));
        metrics.put("averageRiskScore", String.format("%.2f", queue.getAverageRiskScore()));
        metrics.put("totalVolumeUSD", String.format("$%,.2f", queue.getTotalVolumeUSD()));
        metrics.put("queueDepth", queue.getQueueDepth());
        metrics.put("priorityAlertsCount", queue.getPriorityAlertCount());
        metrics.put("activeDBConnections", pool.getActiveConnections());
        metrics.put("idleDBConnections", pool.getIdleConnections());
        metrics.put("activeEngine", appConfig.getActiveEngine());
        metrics.put("flagThreshold", appConfig.getFlagThreshold());
        metrics.put("blockThreshold", appConfig.getBlockThreshold());
        metrics.put("weightAmount", appConfig.getMLWeightAmount());
        metrics.put("weightLocation", appConfig.getMLWeightLocation());
        metrics.put("weightDevice", appConfig.getMLWeightDevice());
        metrics.put("weightVelocity", appConfig.getMLWeightVelocity());
        return metrics;
    }
}
