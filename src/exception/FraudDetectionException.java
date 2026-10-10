package exception;

/**
 * Root checked exception for domain-level fraud detection and evaluation errors.
 */
public class FraudDetectionException extends Exception {

    private final String errorCode;

    public FraudDetectionException(String message) {
        super(message);
        this.errorCode = "FRAUD_EVAL_ERROR";
    }

    public FraudDetectionException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "FRAUD_EVAL_ERROR";
    }

    public FraudDetectionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public FraudDetectionException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
