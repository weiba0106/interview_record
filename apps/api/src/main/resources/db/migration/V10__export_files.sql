-- 一次性导出文件（V10）：CSV ZIP 与 JSON 备份的 30 分钟一次性下载
CREATE TABLE export_files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash BINARY(32) NOT NULL,
    file_name VARCHAR(120) NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    payload LONGBLOB NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    downloaded_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_export_token (token_hash),
    KEY ix_export_user (user_id),
    CONSTRAINT fk_export_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
