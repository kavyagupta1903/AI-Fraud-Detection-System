package model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Domain model representing an automated fraud alert or manual review escalation.
 * Implements Comparable to support priority triage in a PriorityQueue.
 */
public class FraudAlert implements Serializable, Comparable<FraudAlert> {

    private static final long serialVersionUID = 1L;

    public enum Severity {
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        CRITICAL(4);

        private final int weight;

        Severity(int weight) {
            this.weight = weight;
        }

        public int getWeight() {
            return weight;
        }
    }

    private Long alertId;
    private String transactionId;
    private Long userId;
    private String alertReason;
    private Severity severity;
    private boolean isRead;
    private Timestamp createdAt;

    public FraudAlert() {
        this.createdAt = Timestamp.from(Instant.now());
        this.isRead = false;
        this.severity = Severity.MEDIUM;
    }

    public FraudAlert(Long alertId, String transactionId, Long userId, String alertReason,
                      Severity severity, boolean isRead) {
        this.alertId = alertId;
        this.transactionId = transactionId;
        this.userId = userId;
        this.alertReason = alertReason;
        this.severity = severity != null ? severity : Severity.MEDIUM;
        this.isRead = isRead;
        this.createdAt = Timestamp.from(Instant.now());
    }

    @Override
    public int compareTo(FraudAlert other) {
        if (other == null) return -1;
        // Higher severity first
        int severityComparison = Integer.compare(other.severity.getWeight(), this.severity.getWeight());
        if (severityComparison != 0) {
            return severityComparison;
        }
        // Then oldest first for equal severity (FIFO triage)
        if (this.createdAt != null && other.createdAt != null) {
            return this.createdAt.compareTo(other.createdAt);
        }
        return 0;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAlertReason() {
        return alertReason;
    }

    public void setAlertReason(String alertReason) {
        this.alertReason = alertReason;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("FraudAlert[id=%d, tx=%s, severity=%s, reason='%s']",
                alertId, transactionId, severity, alertReason);
    }
}
