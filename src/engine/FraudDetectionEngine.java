package engine;

import exception.FraudDetectionException;
import model.Transaction;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Core interface for fraud detection engines.
 * Enables polymorphic evaluation strategies and interchangeable algorithms.
 */
public interface FraudDetectionEngine {

    /**
     * Evaluates a financial transaction against detection criteria.
     *
     * @param transaction the transaction to evaluate
     * @param context     optional contextual metadata (e.g., historical user transactions, IP lookup)
     * @return FraudEvaluationResult containing risk score, status, risk breakdown, and explanation
     * @throws FraudDetectionException if evaluation fails
     */
    FraudEvaluationResult evaluate(Transaction transaction, Map<String, Object> context) throws FraudDetectionException;

    /**
     * Convenience overload evaluating transaction with empty context.
     */
    default FraudEvaluationResult evaluate(Transaction transaction) throws FraudDetectionException {
        return evaluate(transaction, Collections.emptyMap());
    }

    /**
     * Name identifier for this engine strategy.
     */
    String getEngineName();

    /**
     * Encapsulates the output of a fraud evaluation.
     */
    class FraudEvaluationResult implements Serializable {

        private static final long serialVersionUID = 1L;

        private final double riskScore;
        private final Transaction.Status status;
        private final Map<String, Double> riskBreakdown;
        private final List<String> triggeredRules;
        private final String explanation;

        public FraudEvaluationResult(double riskScore, Transaction.Status status,
                                     Map<String, Double> riskBreakdown,
                                     List<String> triggeredRules, String explanation) {
            this.riskScore = Math.max(0.0, Math.min(1.0, riskScore)); // Clamp between 0.0 and 1.0
            this.status = status;
            this.riskBreakdown = riskBreakdown != null ? Collections.unmodifiableMap(riskBreakdown) : Collections.emptyMap();
            this.triggeredRules = triggeredRules != null ? Collections.unmodifiableList(triggeredRules) : Collections.emptyList();
            this.explanation = explanation;
        }

        public double getRiskScore() {
            return riskScore;
        }

        public Transaction.Status getStatus() {
            return status;
        }

        public Map<String, Double> getRiskBreakdown() {
            return riskBreakdown;
        }

        public List<String> getTriggeredRules() {
            return triggeredRules;
        }

        public String getExplanation() {
            return explanation;
        }

        @Override
        public String toString() {
            return String.format("FraudEvaluationResult[score=%.2f, status=%s, rules=%s, explanation='%s']",
                    riskScore, status, triggeredRules, explanation);
        }
    }
}
