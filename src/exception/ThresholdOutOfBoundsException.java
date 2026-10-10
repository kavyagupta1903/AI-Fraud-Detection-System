package exception;

/**
 * Custom checked exception thrown when an administrative user or service
 * provides risk threshold values or model weights that violate system constraints
 * (e.g., threshold < 0.0 or > 1.0, or flag_threshold >= block_threshold).
 */
public class ThresholdOutOfBoundsException extends FraudDetectionException {

    private final double attemptedValue;
    private final double minBound;
    private final double maxBound;

    public ThresholdOutOfBoundsException(String message, double attemptedValue, double minBound, double maxBound) {
        super("THRESHOLD_OUT_OF_BOUNDS", message);
        this.attemptedValue = attemptedValue;
        this.minBound = minBound;
        this.maxBound = maxBound;
    }

    public ThresholdOutOfBoundsException(String message) {
        super("THRESHOLD_OUT_OF_BOUNDS", message);
        this.attemptedValue = -1.0;
        this.minBound = 0.0;
        this.maxBound = 1.0;
    }

    public double getAttemptedValue() {
        return attemptedValue;
    }

    public double getMinBound() {
        return minBound;
    }

    public double getMaxBound() {
        return maxBound;
    }
}
