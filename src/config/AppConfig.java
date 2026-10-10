package config;

import exception.DatabaseException;
import exception.ThresholdOutOfBoundsException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe Application Configuration Manager.
 * Uses ConcurrentHashMap for lock-free read performance and synchronizes
 * configuration parameters with the persistent system_configs database table.
 */
public class AppConfig {

    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());

    private static volatile AppConfig instance;

    // Thread-safe in-memory cache of system configurations
    private final ConcurrentHashMap<String, String> configCache = new ConcurrentHashMap<>();

    private AppConfig() {
        loadDefaultConfigurations();
        refreshFromDatabase();
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {
                    instance = new AppConfig();
                }
            }
        }
        return instance;
    }

    private void loadDefaultConfigurations() {
        configCache.put("flag_threshold", "0.70");
        configCache.put("block_threshold", "0.90");
        configCache.put("velocity_time_window_minutes", "10");
        configCache.put("velocity_max_count", "3");
        configCache.put("high_amount_threshold", "5000.00");
        configCache.put("active_engine", "ML_ANOMALY");
        configCache.put("ml_weight_amount", "0.30");
        configCache.put("ml_weight_location", "0.25");
        configCache.put("ml_weight_device", "0.20");
        configCache.put("ml_weight_velocity", "0.25");
    }

    /**
     * Refreshes the cache by reading from the system_configs table via pure JDBC.
     */
    public void refreshFromDatabase() {
        DBConnectionPool pool = DBConnectionPool.getInstance();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = pool.getConnection();
            String sql = "SELECT config_key, config_value FROM system_configs";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                String key = rs.getString("config_key");
                String value = rs.getString("config_value");
                if (key != null && value != null) {
                    configCache.put(key, value);
                }
            }
            LOGGER.info("AppConfig refreshed from database: " + configCache.size() + " parameters loaded.");
        } catch (DatabaseException | SQLException e) {
            LOGGER.log(Level.WARNING, "Unable to load configs from database. Operating with in-memory defaults.", e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) pool.releaseConnection(conn);
        }
    }

    /**
     * Updates a configuration parameter with rigorous boundary validation and persists it.
     */
    public synchronized void updateConfig(String key, String value, String updatedBy)
            throws ThresholdOutOfBoundsException, DatabaseException {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Configuration key and value cannot be null");
        }

        // Validate threshold boundaries
        if ("flag_threshold".equals(key) || "block_threshold".equals(key) || key.startsWith("ml_weight_")) {
            try {
                double doubleVal = Double.parseDouble(value);
                if (doubleVal < 0.0 || doubleVal > 1.0) {
                    throw new ThresholdOutOfBoundsException(
                            "Threshold/Weight for '" + key + "' must be between 0.0 and 1.0. Attempted: " + value,
                            doubleVal, 0.0, 1.0
                    );
                }
            } catch (NumberFormatException nfe) {
                throw new ThresholdOutOfBoundsException("Invalid numeric value for " + key + ": " + value);
            }
        }

        // Validate relationship between flag and block threshold
        if ("flag_threshold".equals(key)) {
            double flag = Double.parseDouble(value);
            double currentBlock = getBlockThreshold();
            if (flag >= currentBlock) {
                throw new ThresholdOutOfBoundsException(
                        "Flag threshold (" + flag + ") cannot be greater than or equal to block threshold (" + currentBlock + ")",
                        flag, 0.0, currentBlock
                );
            }
        } else if ("block_threshold".equals(key)) {
            double block = Double.parseDouble(value);
            double currentFlag = getFlagThreshold();
            if (block <= currentFlag) {
                throw new ThresholdOutOfBoundsException(
                        "Block threshold (" + block + ") cannot be less than or equal to flag threshold (" + currentFlag + ")",
                        block, currentFlag, 1.0
                );
            }
        }

        // Update in DB with manual transaction commit/rollback
        DBConnectionPool pool = DBConnectionPool.getInstance();
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = pool.getConnection();
            conn.setAutoCommit(false); // Manual transaction boundary

            String sql = "MERGE INTO system_configs (config_key, config_value, updated_by, updated_at) " +
                         "KEY(config_key) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
            ps = conn.prepareStatement(sql);
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, updatedBy != null ? updatedBy : "ADMIN");
            ps.executeUpdate();

            conn.commit(); // Transaction commit
            configCache.put(key, value); // Update cache only upon successful commit
            LOGGER.info(String.format("Config '%s' successfully updated to '%s' by '%s'", key, value, updatedBy));
        } catch (SQLException | DatabaseException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Transaction rollback on failure
                } catch (SQLException re) {
                    LOGGER.log(Level.SEVERE, "Rollback failed during config update", re);
                }
            }
            throw new DatabaseException("Failed to persist configuration update for: " + key, e);
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                pool.releaseConnection(conn);
            }
        }
    }

    // Typed configuration accessors

    public double getFlagThreshold() {
        return getDouble("flag_threshold", 0.70);
    }

    public double getBlockThreshold() {
        return getDouble("block_threshold", 0.90);
    }

    public int getVelocityTimeWindowMinutes() {
        return getInt("velocity_time_window_minutes", 10);
    }

    public int getVelocityMaxCount() {
        return getInt("velocity_max_count", 3);
    }

    public double getHighAmountThreshold() {
        return getDouble("high_amount_threshold", 5000.00);
    }

    public String getActiveEngine() {
        return getString("active_engine", "ML_ANOMALY");
    }

    public double getMLWeightAmount() {
        return getDouble("ml_weight_amount", 0.30);
    }

    public double getMLWeightLocation() {
        return getDouble("ml_weight_location", 0.25);
    }

    public double getMLWeightDevice() {
        return getDouble("ml_weight_device", 0.20);
    }

    public double getMLWeightVelocity() {
        return getDouble("ml_weight_velocity", 0.25);
    }

    public String getString(String key, String defaultValue) {
        return configCache.getOrDefault(key, defaultValue);
    }

    public double getDouble(String key, double defaultValue) {
        String val = configCache.get(key);
        if (val == null) return defaultValue;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getInt(String key, int defaultValue) {
        String val = configCache.get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public Map<String, String> getAllConfigs() {
        return Collections.unmodifiableMap(configCache);
    }
}
