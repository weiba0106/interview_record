CREATE TABLE share_links (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    position_id BIGINT NOT NULL,
    token_hash BINARY(32) NOT NULL,
    position_fields VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NULL,
    revoked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_share_links_token_hash (token_hash),
    KEY ix_share_links_user_position (user_id, position_id, created_at),
    KEY ix_share_links_position (position_id),
    CONSTRAINT fk_share_links_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_share_links_position FOREIGN KEY (position_id) REFERENCES positions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE share_rounds (
    id BIGINT NOT NULL AUTO_INCREMENT,
    share_id BIGINT NOT NULL,
    round_id BIGINT NOT NULL,
    visible_fields VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_share_rounds_share_round (share_id, round_id),
    KEY ix_share_rounds_round (round_id),
    CONSTRAINT fk_share_rounds_share FOREIGN KEY (share_id) REFERENCES share_links(id) ON DELETE CASCADE,
    CONSTRAINT fk_share_rounds_round FOREIGN KEY (round_id) REFERENCES interview_rounds(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
