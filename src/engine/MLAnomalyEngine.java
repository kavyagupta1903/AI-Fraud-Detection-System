package engine;

import config.AppConfig;
import exception.FraudDetectionException;
import model.Transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Heuristic Machine Learning Anomaly Detection Engine.
 * Simulates multivariate statistical classification and isolation forest scoring
 * by transforming transaction attributes into normalized feature vectors [0.0, 1.0]
 * and applying dynamic, configurable feature weights.
 */
public class MLAnomalyEngine implements FraudDetectionEngine {

    private static final String ENGINE_NAME = "ML_ANOMALY";

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public FraudEvaluationResult evaluate(Transaction tx, Map<String, Object> context) throws FraudDetectionException {
        if (tx == null) {
            throw new FraudDetectionException("Transaction cannot be null for ML anomaly evaluation");
        }

        AppConfig config = AppConfig.getInstance();
        double flagThreshold = config.getFlagThreshold();
        double blockThreshold = config.getBlockThreshold();

        // 1. Dynamic Weights from AppConfig (Tuned via Admin Dashboard)
        double weightAmount = config.getMLWeightAmount();
        double weightLocation = config.getMLWeightLocation();
        double weightDevice = config.getMLWeightDevice();
        double weightVelocity = config.getMLWeightVelocity();
        double weightBaseRail = 0.15; // baseline payment rail weight

        // 2. Feature Extraction & Normalization into [0.0, 1.0]

        // Feature 1: Amount Anomaly Feature (Sigmoid & Z-Score Anomaly)
        double rawAmount = (tx.getAmount() != null) ? tx.getAmount().doubleValue() : 0.0;
        double baselineAmount = config.getHighAmountThreshold();
        double amountRatio = rawAmount / Math.max(1.0, baselineAmount);
        double fAmount = sigmoid((amountRatio - 1.0) * 2.5);

        // Advanced Statistical Z-Score Outlier
        double zScore = 0.0;
        if (context != null && context.containsKey("userMeanAmount") && context.containsKey("userStdDevAmount")) {
            double mean = ((Number) context.get("userMeanAmount")).doubleValue();
            double stdDev = ((Number) context.get("userStdDevAmount")).doubleValue();
            if (stdDev > 0.0) {
                zScore = (rawAmount - mean) / stdDev;
                if (zScore > 3.0) {
                    fAmount = Math.max(fAmount, sigmoid(zScore - 2.0));
                }
            }
        }

        // Feature 2: Velocity Anomaly Feature
        int recentCount = 0;
        if (context != null && context.containsKey("recentTransactionCount")) {
            Object cntObj = context.get("recentTransactionCount");
            if (cntObj instanceof Number) {
                recentCount = ((Number) cntObj).intValue();
            }
        }
        int maxAllowed = config.getVelocityMaxCount();
        double velocityRatio = (double) recentCount / Math.max(1, maxAllowed);
        double fVelocity = Math.min(1.0, Math.pow(velocityRatio, 1.5));

        // Feature 3: Geographic & Impossible Travel Speed Anomaly
        String location = (tx.getLocation() != null) ? tx.getLocation().toUpperCase() : "UNKNOWN";
        double fLocation = 0.10; // baseline domestic location noise
        boolean impossibleTravel = false;
        double travelSpeedKmh = 0.0;

        if (context != null && context.containsKey("prevLocation") && context.containsKey("prevTimestamp")) {
            String prevLoc = (String) context.get("prevLocation");
            long prevTime = ((Number) context.get("prevTimestamp")).longValue();
            long currTime = (tx.getTimestamp() != null) ? tx.getTimestamp().getTime() : System.currentTimeMillis();

            GeoLocationService.TravelFeasibility feasibility =
                    GeoLocationService.evaluateTravelVelocity(prevLoc, prevTime, location, currTime);

            if (feasibility.isImpossible) {
                impossibleTravel = true;
                travelSpeedKmh = feasibility.speedKmh;
                fLocation = 0.98; // Critical impossible travel penalty
            }
        }

        if (!impossibleTravel) {
            if (location.contains("NG") || location.contains("LAGOS") || location.contains("KP") ||
                    location.contains("IR") || location.contains("RU") || location.contains("KY")) {
                fLocation = 0.92; // High-risk foreign jurisdiction anomaly
            } else if (location.contains("UNKNOWN") || location.isBlank()) {
                fLocation = 0.55;
            } else if (!location.contains("US") && !location.contains("CA") && !location.contains("UK")) {
                fLocation = 0.40; // General international cross-border transfer
            }
        }

        // Feature 4: Device Fingerprint & IP Reputation Feature
        String deviceFp = (tx.getDeviceFingerprint() != null) ? tx.getDeviceFingerprint() : "";
        double fDevice = 0.15;
        if (deviceFp.isBlank() || deviceFp.startsWith("dev-fp-unknown")) {
            fDevice = 0.85; // Unrecognized/spoofed hardware fingerprint
        } else if (deviceFp.contains("bot") || deviceFp.contains("proxy") || deviceFp.contains("vpn")) {
            fDevice = 0.95;
        }

        // Feature 5: Intrinsic Payment Rail Risk Factor
        double fRail = tx.calculateBaseRiskFactor();

        // 3. Composite Weighted Risk Aggregation
        double totalWeight = weightAmount + weightLocation + weightDevice + weightVelocity + weightBaseRail;
        if (totalWeight <= 0.0) totalWeight = 1.0;

        double weightedScore = (fAmount * weightAmount) +
                               (fLocation * weightLocation) +
                               (fDevice * weightDevice) +
                               (fVelocity * weightVelocity) +
                               (fRail * weightBaseRail);

        double compositeRisk = weightedScore / totalWeight;

        // Apply slight non-linear boost for multi-feature correlations (isolation anomaly)
        if (fAmount > 0.7 && fLocation > 0.7) {
            compositeRisk = Math.min(1.0, compositeRisk * 1.15); // Compound risk amplifier
        }
        if (impossibleTravel) {
            compositeRisk = Math.max(compositeRisk, 0.95); // Impossible physical travel hard stop
        }

        // 4. Feature Attribution & Explainability Breakdown
        Map<String, Double> breakdown = new HashMap<>();
        breakdown.put("Amount Anomaly Feature", Math.round(fAmount * 100.0) / 100.0);
        breakdown.put("Velocity Spike Feature", Math.round(fVelocity * 100.0) / 100.0);
        breakdown.put("Geographic Entropy Feature", Math.round(fLocation * 100.0) / 100.0);
        breakdown.put("Device Signature Feature", Math.round(fDevice * 100.0) / 100.0);
        breakdown.put("Payment Rail Base Feature", Math.round(fRail * 100.0) / 100.0);

        List<String> triggeredRules = new ArrayList<>();
        if (impossibleTravel) {
            triggeredRules.add(String.format("IMPOSSIBLE_TRAVEL_SPEED (Speed: %,.0f km/h > 900 km/h)", travelSpeedKmh));
        }
        if (zScore > 3.0) {
            triggeredRules.add(String.format("STATISTICAL_ZSCORE_OUTLIER (Z=%.2f > 3.0σ)", zScore));
        }
        if (fAmount > 0.6) triggeredRules.add(String.format("ML_AMOUNT_ANOMALY (Score: %.2f)", fAmount));
        if (fVelocity > 0.6) triggeredRules.add(String.format("ML_VELOCITY_SPIKE (Score: %.2f)", fVelocity));
        if (fLocation > 0.6 && !impossibleTravel) triggeredRules.add(String.format("ML_LOCATION_ANOMALY (Score: %.2f)", fLocation));
        if (fDevice > 0.6) triggeredRules.add(String.format("ML_UNRECOGNIZED_DEVICE (Score: %.2f)", fDevice));
        if (fRail > 0.4) triggeredRules.add(String.format("ML_PAYMENT_RAIL_RISK (Score: %.2f)", fRail));

        // 5. Decision Assignment
        Transaction.Status status;
        String explanation;
        if (compositeRisk >= blockThreshold) {
            status = Transaction.Status.BLOCKED;
            explanation = String.format("BLOCKED by AI ML Anomaly Model (Confidence: %.1f%% >= %.1f%%). Top anomaly features: %s",
                    compositeRisk * 100.0, blockThreshold * 100.0,
                    triggeredRules.isEmpty() ? "Multivariate aggregate" : String.join(", ", triggeredRules));
        } else if (compositeRisk >= flagThreshold) {
            status = Transaction.Status.REVIEW;
            explanation = String.format("FLAGGED FOR REVIEW by AI ML Anomaly Model (Confidence: %.1f%% >= %.1f%%). Anomalies detected: %s",
                    compositeRisk * 100.0, flagThreshold * 100.0,
                    triggeredRules.isEmpty() ? "Borderline statistical deviation" : String.join(", ", triggeredRules));
        } else {
            status = Transaction.Status.APPROVED;
            explanation = String.format("APPROVED by AI ML Anomaly Model (Risk: %.1f%% < %.1f%%). Normal transactional pattern.",
                    compositeRisk * 100.0, flagThreshold * 100.0);
        }

        return new FraudEvaluationResult(compositeRisk, status, breakdown, triggeredRules, explanation);
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }
}
