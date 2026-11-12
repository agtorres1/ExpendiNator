-- Migración: registro de escaneos de tickets (extracción del modelo + confirmación del usuario)
USE expendinatordb;

CREATE TABLE IF NOT EXISTS receipt_scans (
  id VARCHAR(36) PRIMARY KEY,
  user_id INT NOT NULL,
  model VARCHAR(100) NOT NULL,
  prompt_version VARCHAR(100) NOT NULL,
  latency_ms INT NOT NULL,
  prompt_tokens INT NULL,
  completion_tokens INT NULL,
  extraction_json JSON NOT NULL,
  confirmed_json JSON NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  confirmed_at TIMESTAMP NULL,
  INDEX ix_scans_user (user_id),
  CONSTRAINT fk_scans_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
