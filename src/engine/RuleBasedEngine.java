package engine;

import config.AppConfig;
import exception.FraudDetectionException;
import model.CreditCardTransaction;
import model.Transaction;
import model.WireTransaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic Rule-Based Fraud Detection Engine.
 * Evaluates velocity anomalies, geographic discrepancies, amount deviations,
 * and payment rail heuristics against deterministic boolean rules.
 */
public class RuleBasedEngine implements FraudDetectionEngine {

    private static final String ENGINE_NAME = "RULE_BASED";

    private static final Set<String> HIGH_RISK_REGIONS = Set.of(
            "NG", "Lagos, NG", "KP", "IR", "SY", "RU", "KY", "VG", "SOMALIA", "UNKNOWN"
    );

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public FraudEvaluationResult evaluate(Transaction tx, Map<String, Object> context) throws FraudDetectionException {
        if (tx == null) {
            throw new FraudDetectionException("Transaction cannot be null for rule evaluation");
        }

        AppConfig config = AppConfig.getInstance();
        double flagThreshold = config.getFlagThreshold();
        double blockThreshold = config.getBlockThreshold();
        double highAmountThreshold = config.getHighAmountThreshold();
        int maxVelocityAllowed = config.getVelocityMaxCount();

        List<String> triggeredRules = new ArrayList<>();
        Map<String, Double> breakdown = new HashMap<>();

        double accumulatedRisk = 0.0;

        // 1. Base intrinsic payment rail risk (Polymorphic method)
        double baseRailRisk = tx.calculateBaseRiskFactor();
        accumulatedRisk += baseRailRisk;
        breakdown.put("Payment Rail Base Factor", baseRailRisk);
        if (baseRailRisk > 0.30) {
            triggeredRules.add("HIGH_BASE_RAIL_RISK");
        }

        // 2. High-Amount Deviation Check
        BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
        double amountRisk = 0.0;
        if (amount.compareTo(new BigDecimal(String.valueOf(highAmountThreshold))) > 0) {
            amountRisk = 0.35;
            triggeredRules.add("HIGH_AMOUNT_DEVIATION (>" + highAmountThreshold + ")");
        } else if (amount.compareTo(new BigDecimal(String.valueOf(highAmountThreshold * 0.5))) > 0) {
            amountRisk = 0.15;
            triggeredRules.add("ELEVATED_AMOUNT_DEVIATION");
        }
        accumulatedRisk += amountRisk;
        breakdown.put("Amount Deviation", amountRisk);

        // 3. Geographic Anomaly Check
        double geoRisk = 0.0;
        String location = tx.getLocation() != null ? tx.getLocation() : "";
        boolean isHighRiskGeo = false;
        for (String region : HIGH_RISK_REGIONS) {
            if (location.toUpperCase().contains(region)) {
                isHighRiskGeo = true;
                break;
            }
        }

        if (isHighRiskGeo) {
            geoRisk = 0.40;
            triggeredRules.add("HIGH_RISK_GEOGRAPHIC_LOCATION (" + location + ")");
        } else if (location.isBlank() || "UNKNOWN".equalsIgnoreCase(location)) {
            geoRisk = 0.20;
            triggeredRules.add("UNRESOLVED_GEOGRAPHIC_LOCATION");
        }
        accumulatedRisk += geoRisk;
        breakdown.put("Geographic Anomaly", geoRisk);

        // 4. Velocity Check (Recent transactions within time window)
        double velocityRisk = 0.0;
        int recentCount = 0;
        if (context != null && context.containsKey("recentTransactionCount")) {
            Object cntObj = context.get("recentTransactionCount");
            if (cntObj instanceof Number) {
                recentCount = ((Number) cntObj).intValue();
            }
        }

        if (recentCount >= maxVelocityAllowed) {
            velocityRisk = 0.45;
            triggeredRules.add("VELOCITY_SPIKE (" + recentCount + " transactions in recent window)");
        } else if (recentCount >= (maxVelocityAllowed - 1) && maxVelocityAllowed > 1) {
            velocityRisk = 0.20;
            triggeredRules.add("MODERATE_VELOCITY_ACTIVITY");
        }
        accumulatedRisk += velocityRisk;
        breakdown.put("Velocity Anomaly", velocityRisk);

        // 5. Transaction-type specific rule validations
        if (tx instanceof CreditCardTransaction) {
            CreditCardTransaction cc = (CreditCardTransaction) tx;
            if (!cc.isCvvVerified()) {
                double cvvPenalty = 0.30;
                accumulatedRisk += cvvPenalty;
                breakdown.put("CVV Verification Failure", cvvPenalty);
                triggeredRules.add("CVV_NOT_VERIFIED");
            }
        } else if (tx instanceof WireTransaction) {
            WireTransaction wt = (WireTransaction) tx;
            if (wt.getSwiftCode() == null || wt.getSwiftCode().length() < 8) {
                double swiftPenalty = 0.25;
                accumulatedRisk += swiftPenalty;
                breakdown.put("Malformed SWIFT Identifier", swiftPenalty);
                triggeredRules.add("INVALID_SWIFT_CODE");
            }
        }

        // Clamp final composite risk between 0.0 and 1.0
        double finalScore = Math.min(1.0, Math.max(0.0, accumulatedRisk));

        // Assign status according to threshold constraints
        Transaction.Status status;
        String explanation;
        if (finalScore >= blockThreshold) {
            status = Transaction.Status.BLOCKED;
            explanation = String.format("BLOCKED by Rule Engine (Score: %.2f >= %.2f). Triggered rules: %s",
                    finalScore, blockThreshold, String.join(", ", triggeredRules));
        } else if (finalScore >= flagThreshold) {
            status = Transaction.Status.REVIEW;
            explanation = String.format("FLAGGED FOR REVIEW by Rule Engine (Score: %.2f >= %.2f). Triggered rules: %s",
                    finalScore, flagThreshold, String.join(", ", triggeredRules));
        } else {
            status = Transaction.Status.APPROVED;
            explanation = String.format("APPROVED by Rule Engine (Score: %.2f < %.2f). All standard rules passed.",
                    finalScore, flagThreshold);
        }

        return new FraudEvaluationResult(finalScore, status, breakdown, triggeredRules, explanation);
    }
}
