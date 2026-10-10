package exception;

/**
 * Custom checked exception representing database connectivity, transaction,
 * or JDBC execution failures within the pure JDBC persistence layer.
 */
public class DatabaseException extends Exception {

    private final String sqlState;
    private final int vendorCode;

    public DatabaseException(String message) {
        super(message);
        this.sqlState = null;
        this.vendorCode = 0;
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
        this.sqlState = null;
        this.vendorCode = 0;
    }

    public DatabaseException(String message, String sqlState, int vendorCode, Throwable cause) {
        super(message, cause);
        this.sqlState = sqlState;
        this.vendorCode = vendorCode;
    }

    public String getSqlState() {
        return sqlState;
    }

    public int getVendorCode() {
        return vendorCode;
    }
}
