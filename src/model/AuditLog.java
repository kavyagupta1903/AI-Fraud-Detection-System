package model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Immutable Audit Log entity capturing compliance events, administrative overrides,
 * and security modifications in accordance with banking regulatory standards.
 */
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long auditId;
    private String actionType; // CONFIG_UPDATE, TRANSACTION_OVERRIDE, ENGINE_SWITCH, MANUAL_BLOCK, MANUAL_APPROVE
    private String performedBy;
    private String targetId;
    private String details;
    private Timestamp timestamp;

    public AuditLog() {
        this.timestamp = Timestamp.from(Instant.now());
    }

    public AuditLog(Long auditId, String actionType, String performedBy, String targetId, String details) {
        this.auditId = auditId;
        this.actionType = actionType;
        this.performedBy = performedBy;
        this.targetId = targetId;
        this.details = details;
        this.timestamp = Timestamp.from(Instant.now());
    }

    public Long getAuditId() {
        return auditId;
    }

    public void setAuditId(Long auditId) {
        this.auditId = auditId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return String.format("AuditLog[id=%d, action=%s, user=%s, target=%s]",
                auditId, actionType, performedBy, targetId);
    }
}
