package model;

import java.math.BigDecimal;

/**
 * Concrete subclass of Transaction representing card-based purchases.
 */
public class CreditCardTransaction extends Transaction {

    private static final long serialVersionUID = 1L;

    private String cardNumberLast4;
    private String cardNetwork; // VISA, MASTERCARD, AMEX, DISCOVER
    private boolean cvvVerified;
    private String billingZip;

    public CreditCardTransaction() {
        super();
        setTransactionType(TransactionType.CREDIT_CARD);
        this.cvvVerified = true;
    }

    public CreditCardTransaction(String transactionId, Long userId, BigDecimal amount, String currency,
                                 String location, String ipAddress, String deviceFingerprint, String category,
                                 String cardNumberLast4, String cardNetwork, boolean cvvVerified, String billingZip) {
        super(transactionId, userId, amount, currency, TransactionType.CREDIT_CARD, location, ipAddress, deviceFingerprint, category);
        this.cardNumberLast4 = cardNumberLast4;
        this.cardNetwork = (cardNetwork != null) ? cardNetwork.toUpperCase() : "VISA";
        this.cvvVerified = cvvVerified;
        this.billingZip = billingZip;
    }

    @Override
    public String getTransactionDetails() {
        return String.format("Credit Card (%s ending in %s) | CVV Verified: %s | Billing Zip: %s",
                cardNetwork != null ? cardNetwork : "CARD",
                cardNumberLast4 != null ? cardNumberLast4 : "****",
                cvvVerified ? "YES" : "NO",
                billingZip != null ? billingZip : "N/A");
    }

    @Override
    public double calculateBaseRiskFactor() {
        double baseRisk = 0.05; // standard low baseline for card purchases
        if (!cvvVerified) {
            baseRisk += 0.45; // significant penalty for unverified CVV
        }
        if (getAmount() != null && getAmount().compareTo(new BigDecimal("2500.00")) > 0) {
            baseRisk += 0.15; // elevated risk for large credit card swipes
        }
        return Math.min(1.0, baseRisk);
    }

    public String getCardNumberLast4() {
        return cardNumberLast4;
    }

    public void setCardNumberLast4(String cardNumberLast4) {
        this.cardNumberLast4 = cardNumberLast4;
    }

    public String getCardNetwork() {
        return cardNetwork;
    }

    public void setCardNetwork(String cardNetwork) {
        this.cardNetwork = cardNetwork;
    }

    public boolean isCvvVerified() {
        return cvvVerified;
    }

    public void setCvvVerified(boolean cvvVerified) {
        this.cvvVerified = cvvVerified;
    }

    public String getBillingZip() {
        return billingZip;
    }

    public void setBillingZip(String billingZip) {
        this.billingZip = billingZip;
    }
}
