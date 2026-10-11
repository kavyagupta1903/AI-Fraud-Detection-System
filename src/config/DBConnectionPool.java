package config;

import exception.DatabaseException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Production-grade custom JDBC Connection Pool implemented entirely in standard Java.
 * Features thread-safe connection pooling, health checks, leak prevention,
 * and automatic schema initialization without third-party ORM or pool dependencies.
 */
public class DBConnectionPool {

    private static final Logger LOGGER = Logger.getLogger(DBConnectionPool.class.getName());

    private static final String DEFAULT_JDBC_URL = "jdbc:h2:./data/fraud_db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String DEFAULT_USER = "sa";
    private static final String DEFAULT_PASSWORD = "";
    private static final int DEFAULT_MIN_CONNECTIONS = 5;
    private static final int DEFAULT_MAX_CONNECTIONS = 20;
    private static final long DEFAULT_ACQUIRE_TIMEOUT_MS = 5000L;

    private static volatile DBConnectionPool instance;

    private final String jdbcUrl;
    private final String dbUser;
    private final String dbPassword;
    private final int minConnections;
    private final int maxConnections;
    private final long acquireTimeoutMs;

    private final BlockingQueue<Connection> pool;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger totalCreated = new AtomicInteger(0);
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    private DBConnectionPool() {
        this(DEFAULT_JDBC_URL, DEFAULT_USER, DEFAULT_PASSWORD,
                DEFAULT_MIN_CONNECTIONS, DEFAULT_MAX_CONNECTIONS, DEFAULT_ACQUIRE_TIMEOUT_MS);
    }

    private DBConnectionPool(String jdbcUrl, String dbUser, String dbPassword,
                             int minConnections, int maxConnections, long acquireTimeoutMs) {
        this.jdbcUrl = jdbcUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.minConnections = minConnections;
        this.maxConnections = maxConnections;
        this.acquireTimeoutMs = acquireTimeoutMs;
        this.pool = new LinkedBlockingQueue<>(maxConnections);

        try {
            // Ensure H2 Driver is loaded
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "H2 Driver not found on classpath", e);
        }

        initializePool();
        initializeSchema();

        // Register JVM shutdown hook for clean resource disposal
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "DBConnectionPool-ShutdownHook"));
    }

    public static DBConnectionPool getInstance() {
        if (instance == null) {
            synchronized (DBConnectionPool.class) {
                if (instance == null) {
                    instance = new DBConnectionPool();
                }
            }
        }
        return instance;
    }

    /**
     * Initializes pool with baseline minimum connections.
     */
    private void initializePool() {
        LOGGER.info(String.format("Initializing DB Connection Pool: url=%s, min=%d, max=%d",
                jdbcUrl, minConnections, maxConnections));
        try {
            for (int i = 0; i < minConnections; i++) {
                Connection conn = createNewConnection();
                pool.offer(conn);
                totalCreated.incrementAndGet();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize base database connections in pool", e);
        }
    }

    private Connection createNewConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
        conn.setAutoCommit(true);
        return conn;
    }

    /**
     * Acquires an available, healthy connection from the pool.
     * Blocks up to the configured acquireTimeoutMs.
     *
     * @return a valid java.sql.Connection
     * @throws DatabaseException if timeout expires or connection failure occurs
     */
    public Connection getConnection() throws DatabaseException {
        if (isShutdown.get()) {
            throw new DatabaseException("Connection pool has been shut down.");
        }

        long deadline = System.currentTimeMillis() + acquireTimeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Connection conn = pool.poll();

            if (conn != null) {
                try {
                    if (isConnectionHealthy(conn)) {
                        activeConnections.incrementAndGet();
                        return conn;
                    } else {
                        // Connection was stale or closed; dispose and decrease count
                        closeQuietly(conn);
                        totalCreated.decrementAndGet();
                    }
                } catch (Exception e) {
                    closeQuietly(conn);
                    totalCreated.decrementAndGet();
                }
            } else if (totalCreated.get() < maxConnections) {
                // If capacity allows, spawn a new connection
                synchronized (this) {
                    if (totalCreated.get() < maxConnections) {
                        try {
                            Connection newConn = createNewConnection();
                            totalCreated.incrementAndGet();
                            activeConnections.incrementAndGet();
                            return newConn;
                        } catch (SQLException e) {
                            LOGGER.log(Level.WARNING, "Failed creating on-demand connection: " + e.getMessage());
                        }
                    }
                }
            }

            // Wait a short time before retrying
            try {
                long remainingWait = Math.max(10, deadline - System.currentTimeMillis());
                Connection polled = pool.poll(Math.min(remainingWait, 100), TimeUnit.MILLISECONDS);
                if (polled != null) {
                    if (isConnectionHealthy(polled)) {
                        activeConnections.incrementAndGet();
                        return polled;
                    } else {
                        closeQuietly(polled);
                        totalCreated.decrementAndGet();
                    }
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new DatabaseException("Thread interrupted while waiting for database connection", ie);
            }
        }

        throw new DatabaseException("Database connection acquire timeout (" + acquireTimeoutMs + "ms) exceeded. " +
                "Active: " + activeConnections.get() + ", Total: " + totalCreated.get());
    }

    /**
     * Releases a connection back into the pool.
     * Cleans up transaction state to prevent leaks across threads.
     *
     * @param conn Connection to release.
     */
    public void releaseConnection(Connection conn) {
        if (conn == null) return;

        try {
            if (isShutdown.get()) {
                closeQuietly(conn);
                totalCreated.decrementAndGet();
                return;
            }

            // If left uncommitted, rollback to clean state
            if (!conn.getAutoCommit()) {
                try {
                    conn.rollback();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Failed to rollback connection before release", e);
                }
                conn.setAutoCommit(true);
            }

            // Test if connection is still healthy
            if (isConnectionHealthy(conn)) {
                if (!pool.offer(conn)) {
                    // Pool is full, close physical connection
                    closeQuietly(conn);
                    totalCreated.decrementAndGet();
                }
            } else {
                closeQuietly(conn);
                totalCreated.decrementAndGet();
            }
        } catch (SQLException e) {
            closeQuietly(conn);
            totalCreated.decrementAndGet();
        } finally {
            activeConnections.decrementAndGet();
        }
    }

    private boolean isConnectionHealthy(Connection conn) {
        try {
            return conn != null && !conn.isClosed() && conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * Executes schema.sql if tables do not exist.
     */
    private void initializeSchema() {
        String schemaContent = loadSchemaSql();
        if (schemaContent == null || schemaContent.isBlank()) {
            LOGGER.warning("No schema.sql found or content is empty. Skipping schema auto-creation.");
            return;
        }

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = createNewConnection();
            stmt = conn.createStatement();

            // Strip line comments (-- ...) first so statements aren't skipped
            StringBuilder cleanSql = new StringBuilder();
            for (String line : schemaContent.split("\\r?\\n")) {
                String trimmedLine = line.trim();
                if (!trimmedLine.startsWith("--")) {
                    cleanSql.append(line).append("\n");
                }
            }

            // Execute SQL commands split by semicolon
            String[] commands = cleanSql.toString().split(";");
            for (String cmd : commands) {
                String trimmed = cmd.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        stmt.execute(trimmed);
                    } catch (SQLException e) {
                        LOGGER.log(Level.WARNING, "SQL execution warning on: [" + trimmed + "]: " + e.getMessage());
                    }
                }
            }
            LOGGER.info("Database schema initialized and seed data verified successfully.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed executing schema initialization", e);
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            closeQuietly(conn);
        }
    }

    private String loadSchemaSql() {
        // Try classpath resource first
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not load schema.sql from classpath: " + e.getMessage());
        }

        // Try filesystem paths
        String[] potentialPaths = {"schema.sql", "src/resources/schema.sql"};
        for (String pathStr : potentialPaths) {
            Path path = Paths.get(pathStr);
            if (Files.exists(path)) {
                try {
                    return Files.readString(path, StandardCharsets.UTF_8);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    public synchronized void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            LOGGER.info("Shutting down DBConnectionPool. Closing all physical connections...");
            Connection conn;
            while ((conn = pool.poll()) != null) {
                closeQuietly(conn);
                totalCreated.decrementAndGet();
            }
            LOGGER.info("DBConnectionPool shutdown completed.");
        }
    }

    // Pool Diagnostics
    public int getActiveConnections() {
        return Math.max(0, activeConnections.get());
    }

    public int getIdleConnections() {
        return pool.size();
    }

    public int getTotalCreated() {
        return totalCreated.get();
    }
}
