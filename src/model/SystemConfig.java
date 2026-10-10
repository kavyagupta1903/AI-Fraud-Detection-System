package model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Domain model representing a system configuration key-value pair.
 */
public class SystemConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String configKey;
    private String configValue;
    private String updatedBy;
    private Timestamp updatedAt;

    public SystemConfig() {
        this.updatedAt = Timestamp.from(Instant.now());
    }

    public SystemConfig(String configKey, String configValue, String updatedBy) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.updatedBy = updatedBy;
        this.updatedAt = Timestamp.from(Instant.now());
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return String.format("SystemConfig[%s=%s, updatedBy=%s]", configKey, configValue, updatedBy);
    }
}
