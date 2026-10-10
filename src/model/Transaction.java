package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Abstract base class representing a financial transaction.
 * Demonstrates OOP Abstraction and Polymorphism.
 */
public abstract class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status {
        APPROVED,
        REVIEW,
        BLOCKED
    }

    public enum TransactionType {
        CREDIT_CARD,
        WIRE_TRANSFER
    }

    private String transactionId;
    private Long userId;
    private BigDecimal amount;
    private String currency;
    private TransactionType transactionType;
    private String location;
    private String ipAddress;
    private String deviceFingerprint;
    private String category;
    private Timestamp timestamp;
    private double riskScore;
    private Status status;
    private String evaluationNotes;
    private String decisionEngine;

    // Default constructor
    public Transaction() {
        this.timestamp = Timestamp.from(Instant.now());
        this.status = Status.REVIEW;
        this.currency = "USD";
        this.riskScore = 0.0;
    }

    // Parameterized constructor
    public Transaction(String transactionId, Long userId, BigDecimal amount, String currency,
                       TransactionType transactionType, String location, String ipAddress,
                       String deviceFingerprint, String category) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.currency = (currency != null && !currency.isBlank()) ? currency : "USD";
        this.transactionType = transactionType;
        this.location = location;
        this.ipAddress = ipAddress;
        this.deviceFingerprint = deviceFingerprint;
        this.category = (category != null && !category.isBlank()) ? category : "GENERAL";
        this.timestamp = Timestamp.from(Instant.now());
        this.status = Status.REVIEW;
        this.riskScore = 0.0;
    }

    /**
     * Abstract method enforcing specialized formatted details per transaction type.
     *
     * @return Formatted human-readable details of the transaction.
     */
    public abstract String getTransactionDetails();

    /**
     * Abstract method computing an intrinsic baseline risk factor based on payment rail specifics.
     *
     * @return baseline risk factor between 0.0 and 1.0.
     */
    public abstract double calculateBaseRiskFactor();

    // Standard Getters and Setters

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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getEvaluationNotes() {
        return evaluationNotes;
    }

    public void setEvaluationNotes(String evaluationNotes) {
        this.evaluationNotes = evaluationNotes;
    }

    public String getDecisionEngine() {
        return decisionEngine;
    }

    public void setDecisionEngine(String decisionEngine) {
        this.decisionEngine = decisionEngine;
    }

    @Override
    public String toString() {
        return String.format("[%s] ID: %s | User: %d | Amount: %s %s | Risk: %.2f | Status: %s",
                transactionType, transactionId, userId, amount, currency, riskScore, status);
    }
}
