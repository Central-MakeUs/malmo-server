-- Run this migration before deploying the always-on Kakao web login.
-- It only adds web-specific tables and does not modify member_entity.

CREATE TABLE web_session (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    refresh_token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    modified_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_web_session_refresh_token_hash UNIQUE (refresh_token_hash),
    INDEX idx_web_session_member_id (member_id),
    INDEX idx_web_session_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
