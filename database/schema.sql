-- ============================================================
-- Virtual Queue & Token System - Database Schema
-- Run this once in MySQL before starting the app:
--   mysql -u root -p < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS virtual_queue_db;
USE virtual_queue_db;

-- ------------------------------------------------------------
-- Shops / clinics / salons that run a queue
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS shops (
    shop_id               INT AUTO_INCREMENT PRIMARY KEY,
    shop_name             VARCHAR(100)  NOT NULL,
    category              VARCHAR(50)   DEFAULT 'General',
    username              VARCHAR(50)   NOT NULL UNIQUE,
    password_hash         VARCHAR(255)  NOT NULL,
    avg_service_time_mins INT           NOT NULL DEFAULT 10,
    is_open               BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- Tokens issued to customers. token_number resets per shop,
-- per day - queue_date is what makes that possible.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tokens (
    token_id        INT AUTO_INCREMENT PRIMARY KEY,
    shop_id         INT NOT NULL,
    token_number    INT NOT NULL,
    customer_name   VARCHAR(100) NOT NULL,
    customer_phone  VARCHAR(15),
    status          ENUM('WAITING','CALLED','SERVED','SKIPPED','CANCELLED') NOT NULL DEFAULT 'WAITING',
    queue_date      DATE NOT NULL,
    joined_at       TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    called_at       TIMESTAMP NULL,
    grace_deadline  TIMESTAMP NULL,
    served_at       TIMESTAMP NULL,
    CONSTRAINT fk_tokens_shop FOREIGN KEY (shop_id) REFERENCES shops(shop_id)
);

-- Speeds up the two queries that run on every AJAX poll:
-- "who's ahead of me" and "what's the current queue".
CREATE INDEX idx_tokens_shop_date_status ON tokens (shop_id, queue_date, status);
CREATE INDEX idx_tokens_grace_deadline   ON tokens (status, grace_deadline);

-- ------------------------------------------------------------
-- Optional: a demo shop so you have something to click on
-- immediately after setup. Username: demo_clinic / Password: demo123
-- (hash below is bcrypt for "demo123")
-- ------------------------------------------------------------
INSERT INTO shops (shop_name, category, username, password_hash, avg_service_time_mins, is_open)
VALUES (
    'Sunrise Family Clinic',
    'Clinic',
    'demo_clinic',
    '$2a$12$jZHfQCaJNCFjFAA9CRx4/.uHJTYnYYUoQCWMliFp6r8v8raXAegq.',
    12,
    TRUE
);
