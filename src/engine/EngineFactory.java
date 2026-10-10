package engine;

import config.AppConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory Pattern implementation supplying and caching FraudDetectionEngine strategies.
 * Enables dynamic runtime switching of evaluation engines without restarting the system.
 */
public class EngineFactory {

    private static final Map<String, FraudDetectionEngine> ENGINE_REGISTRY = new ConcurrentHashMap<>();

    static {
        // Register default concrete engines
        registerEngine("RULE_BASED", new RuleBasedEngine());
        registerEngine("ML_ANOMALY", new MLAnomalyEngine());
        registerEngine("HYBRID", new HybridEngine());
    }

    /**
     * Registers a new or custom engine implementation into the factory.
     */
    public static void registerEngine(String name, FraudDetectionEngine engine) {
        if (name != null && engine != null) {
            ENGINE_REGISTRY.put(name.toUpperCase().trim(), engine);
        }
    }

    /**
     * Retrieves an engine instance by name identifier.
     * Defaults to MLAnomalyEngine if not found.
     *
     * @param engineType name ("RULE_BASED", "ML_ANOMALY", "HYBRID")
     * @return concrete FraudDetectionEngine implementation
     */
    public static FraudDetectionEngine getEngine(String engineType) {
        if (engineType == null || engineType.isBlank()) {
            return getDefaultEngine();
        }
        FraudDetectionEngine engine = ENGINE_REGISTRY.get(engineType.toUpperCase().trim());
        if (engine == null) {
            return getDefaultEngine();
        }
        return engine;
    }

    /**
     * Returns the engine currently designated as active in AppConfig.
     */
    public static FraudDetectionEngine getDefaultEngine() {
        String activeType = AppConfig.getInstance().getActiveEngine();
        FraudDetectionEngine engine = ENGINE_REGISTRY.get(activeType != null ? activeType.toUpperCase().trim() : "ML_ANOMALY");
        if (engine == null) {
            return ENGINE_REGISTRY.get("ML_ANOMALY");
        }
        return engine;
    }

    /**
     * Hybrid Ensemble Engine combining deterministic rules with ML isolation scoring.
     */
    public static class HybridEngine implements FraudDetectionEngine {

        private final RuleBasedEngine ruleEngine = new RuleBasedEngine();
        private final MLAnomalyEngine mlEngine = new MLAnomalyEngine();

        @Override
        public String getEngineName() {
            return "HYBRID";
        }

        @Override
        public FraudEvaluationResult evaluate(model.Transaction transaction, Map<String, Object> context)
                throws exception.FraudDetectionException {
            FraudEvaluationResult ruleRes = ruleEngine.evaluate(transaction, context);
            FraudEvaluationResult mlRes = mlEngine.evaluate(transaction, context);

            // Ensemble blend: 40% rule-based weight, 60% ML anomaly weight
            double ensembleScore = (ruleRes.getRiskScore() * 0.40) + (mlRes.getRiskScore() * 0.60);

            // Conservative policy: if either strictly blocked, result is blocked
            model.Transaction.Status finalStatus;
            AppConfig config = AppConfig.getInstance();
            if (ruleRes.getStatus() == model.Transaction.Status.BLOCKED ||
                    mlRes.getStatus() == model.Transaction.Status.BLOCKED ||
                    ensembleScore >= config.getBlockThreshold()) {
                finalStatus = model.Transaction.Status.BLOCKED;
            } else if (ensembleScore >= config.getFlagThreshold() ||
                    ruleRes.getStatus() == model.Transaction.Status.REVIEW ||
                    mlRes.getStatus() == model.Transaction.Status.REVIEW) {
                finalStatus = model.Transaction.Status.REVIEW;
            } else {
                finalStatus = model.Transaction.Status.APPROVED;
            }

            Map<String, Double> combinedBreakdown = new java.util.HashMap<>(ruleRes.getRiskBreakdown());
            combinedBreakdown.putAll(mlRes.getRiskBreakdown());

            java.util.List<String> combinedRules = new java.util.ArrayList<>(ruleRes.getTriggeredRules());
            combinedRules.addAll(mlRes.getTriggeredRules());

            String combinedExplanation = String.format("HYBRID ENSEMBLE (Score: %.2f | Status: %s) [Rule: %.2f, ML: %.2f]",
                    ensembleScore, finalStatus, ruleRes.getRiskScore(), mlRes.getRiskScore());

            return new FraudEvaluationResult(ensembleScore, finalStatus, combinedBreakdown, combinedRules, combinedExplanation);
        }
    }
}
