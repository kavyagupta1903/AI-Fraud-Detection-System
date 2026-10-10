package model;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Concrete subclass of Transaction representing high-value wire transfers.
 */
public class WireTransaction extends Transaction {

    private static final long serialVersionUID = 1L;

    private static final Set<String> HIGH_RISK_JURISDICTIONS = Set.of(
            "NG", "Lagos, NG", "KP", "IR", "SY", "RU", "KY", "VG"
    );

    private String recipientBank;
    private String recipientAccount;
    private String swiftCode;
    private String wirePurpose;

    public WireTransaction() {
        super();
        setTransactionType(TransactionType.WIRE_TRANSFER);
    }

    public WireTransaction(String transactionId, Long userId, BigDecimal amount, String currency,
                           String location, String ipAddress, String deviceFingerprint, String category,
                           String recipientBank, String recipientAccount, String swiftCode, String wirePurpose) {
        super(transactionId, userId, amount, currency, TransactionType.WIRE_TRANSFER, location, ipAddress, deviceFingerprint, category);
        this.recipientBank = recipientBank;
        this.recipientAccount = recipientAccount;
        this.swiftCode = (swiftCode != null) ? swiftCode.toUpperCase() : "";
        this.wirePurpose = wirePurpose;
    }

    @Override
    public String getTransactionDetails() {
        return String.format("Wire Transfer | Bank: %s | SWIFT: %s | Beneficiary: %s | Purpose: %s",
                recipientBank != null ? recipientBank : "N/A",
                swiftCode != null ? swiftCode : "N/A",
                recipientAccount != null ? maskAccount(recipientAccount) : "N/A",
                wirePurpose != null ? wirePurpose : "General Payment");
    }

    @Override
    public double calculateBaseRiskFactor() {
        double baseRisk = 0.20; // Wires are irreversible and carry higher baseline risk

        if (getAmount() != null && getAmount().compareTo(new BigDecimal("10000.00")) > 0) {
            baseRisk += 0.25; // Large wire risk
        }

        if (getLocation() != null) {
            for (String highRisk : HIGH_RISK_JURISDICTIONS) {
                if (getLocation().toUpperCase().contains(highRisk.toUpperCase())) {
                    baseRisk += 0.40;
                    break;
                }
            }
        }

        if (swiftCode == null || swiftCode.trim().length() < 8) {
            baseRisk += 0.20; // Invalid/missing SWIFT formatting
        }

        return Math.min(1.0, baseRisk);
    }

    private String maskAccount(String account) {
        if (account == null || account.length() <= 4) return "****";
        return "****" + account.substring(account.length() - 4);
    }

    public String getRecipientBank() {
        return recipientBank;
    }

    public void setRecipientBank(String recipientBank) {
        this.recipientBank = recipientBank;
    }

    public String getRecipientAccount() {
        return recipientAccount;
    }

    public void setRecipientAccount(String recipientAccount) {
        this.recipientAccount = recipientAccount;
    }

    public String getSwiftCode() {
        return swiftCode;
    }

    public void setSwiftCode(String swiftCode) {
        this.swiftCode = swiftCode;
    }

    public String getWirePurpose() {
        return wirePurpose;
    }

    public void setWirePurpose(String wirePurpose) {
        this.wirePurpose = wirePurpose;
    }
}
