-- ====================================================================
-- AI-Powered Fraud Detection System - Database Schema
-- Compatible with pure standard JDBC (H2 Embedded / PostgreSQL / MySQL)
-- ====================================================================

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    email VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. TRANSACTIONS TABLE (Handles both Credit Card and Wire Transfers polymorphically)
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'USD',
    transaction_type VARCHAR(30) NOT NULL CHECK (transaction_type IN ('CREDIT_CARD', 'WIRE_TRANSFER')),
    location VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    device_fingerprint VARCHAR(128),
    category VARCHAR(50) DEFAULT 'GENERAL',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    risk_score DOUBLE DEFAULT 0.0,
    status VARCHAR(20) NOT NULL CHECK (status IN ('APPROVED', 'REVIEW', 'BLOCKED')),
    
    -- Subclass specific attributes (Polymorphism persistence)
    card_number_last4 VARCHAR(4),
    card_network VARCHAR(30),
    cvv_verified BOOLEAN DEFAULT TRUE,
    billing_zip VARCHAR(20),
    
    recipient_bank VARCHAR(100),
    recipient_account VARCHAR(64),
    swift_code VARCHAR(20),
    wire_purpose VARCHAR(255),
    
    -- Diagnostic / Engine evaluation info
    evaluation_notes TEXT,
    decision_engine VARCHAR(50),
    
    CONSTRAINT fk_trans_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Indexes for performance & velocity queries
CREATE INDEX IF NOT EXISTS idx_trans_user_time ON transactions(user_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_trans_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_trans_risk ON transactions(risk_score);

-- 3. SYSTEM CONFIGS TABLE (Dynamic Risk Thresholds & ML Feature Weights)
CREATE TABLE IF NOT EXISTS system_configs (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value VARCHAR(255) NOT NULL,
    updated_by VARCHAR(50) DEFAULT 'SYSTEM',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. FRAUD ALERTS TABLE (High-risk notifications and investigation queue)
CREATE TABLE IF NOT EXISTS fraud_alerts (
    alert_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    alert_reason TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alert_trans FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_alert_user ON fraud_alerts(user_id);
CREATE INDEX IF NOT EXISTS idx_alert_unread ON fraud_alerts(is_read);

-- 5. AUDIT LOGS TABLE (Compliance, PCI-DSS / SOX Immutable Audit Trail)
CREATE TABLE IF NOT EXISTS audit_logs (
    audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action_type VARCHAR(50) NOT NULL,
    performed_by VARCHAR(50) NOT NULL,
    target_id VARCHAR(64),
    details TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_time ON audit_logs(timestamp);

-- ====================================================================
-- SEED DATA
-- Default Passwords:
-- admin / admin123 (SHA-256: 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9)
-- user  / password123 (SHA-256: ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f)
-- ====================================================================

MERGE INTO users (user_id, username, password_hash, role, email) KEY(username) VALUES
(1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN', 'admin@fraudshield.enterprise.com'),
(2, 'john_doe', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'USER', 'john.doe@example.com'),
(3, 'alice_smith', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'USER', 'alice.smith@example.com');

-- System Configuration Baseline
MERGE INTO system_configs (config_key, config_value, updated_by) KEY(config_key) VALUES
('flag_threshold', '0.70', 'ADMIN'),
('block_threshold', '0.90', 'ADMIN'),
('velocity_time_window_minutes', '10', 'ADMIN'),
('velocity_max_count', '3', 'ADMIN'),
('high_amount_threshold', '5000.00', 'ADMIN'),
('active_engine', 'ML_ANOMALY', 'ADMIN'),
('ml_weight_amount', '0.30', 'ADMIN'),
('ml_weight_location', '0.25', 'ADMIN'),
('ml_weight_device', '0.20', 'ADMIN'),
('ml_weight_velocity', '0.25', 'ADMIN');

-- Sample Initial Transactions for demonstration and testing
MERGE INTO transactions (transaction_id, user_id, amount, currency, transaction_type, location, ip_address, device_fingerprint, category, timestamp, risk_score, status, card_number_last4, card_network, cvv_verified, billing_zip, evaluation_notes, decision_engine) KEY(transaction_id) VALUES
('TX-CC-9801', 2, 85.50, 'USD', 'CREDIT_CARD', 'New York, US', '192.168.1.10', 'dev-fp-john-laptop', 'RETAIL', CURRENT_TIMESTAMP, 0.12, 'APPROVED', '4242', 'VISA', TRUE, '10001', 'Standard domestic transaction, matched typical profile', 'ML_ANOMALY'),
('TX-CC-9802', 2, 450.00, 'USD', 'CREDIT_CARD', 'New York, US', '192.168.1.10', 'dev-fp-john-laptop', 'ELECTRONICS', CURRENT_TIMESTAMP, 0.28, 'APPROVED', '4242', 'VISA', TRUE, '10001', 'Within baseline purchase limits', 'RULE_BASED'),
('TX-WT-9803', 2, 12500.00, 'USD', 'WIRE_TRANSFER', 'Lagos, NG', '102.89.23.44', 'dev-fp-unknown-778', 'BUSINESS', CURRENT_TIMESTAMP, 0.95, 'BLOCKED', NULL, NULL, NULL, NULL, 'BLOCKED: High wire transfer amount to high-risk foreign jurisdiction from unknown device fingerprint', 'ML_ANOMALY');

-- Initial Alert for the blocked transaction
MERGE INTO fraud_alerts (alert_id, transaction_id, user_id, alert_reason, severity, is_read) KEY(alert_id) VALUES
(1, 'TX-WT-9803', 2, 'CRITICAL: Wire transfer $12,500.00 to Lagos, NG from unrecognized IP/device exceeded block threshold (Score: 0.95)', 'CRITICAL', FALSE);
