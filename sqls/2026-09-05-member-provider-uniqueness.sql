-- Social-login member uniqueness migration (MySQL 8)
--
-- IMPORTANT
-- 1. Run each numbered section separately and inspect its result before continuing.
-- 2. Pause login traffic for sections 2-4 if possible.
-- 3. Section 4 (ALTER TABLE) implicitly commits in MySQL and cannot be rolled back.
-- 4. Production uses spring.jpa.hibernate.ddl-auto=none; this migration is required
--    before deploying the concurrency-safe application code.

-- -----------------------------------------------------------------------------
-- 1. Preflight: inspect every result and stop on duplicates/invalid identities.
-- -----------------------------------------------------------------------------

-- No active social identity may have more than one member.
SELECT provider, provider_id, COUNT(*) AS member_count
FROM member_entity
WHERE member_state <> 'DELETED'
GROUP BY provider, provider_id
HAVING COUNT(*) > 1;

-- Provider identity values must be present.
SELECT COUNT(*) AS invalid_provider_identity_count
FROM member_entity
WHERE provider IS NULL
   OR provider_id IS NULL
   OR TRIM(provider_id) = '';

-- Confirm the current schema and provider_id capacity before updating data.
SHOW FULL COLUMNS FROM member_entity LIKE 'provider';
SHOW FULL COLUMNS FROM member_entity LIKE 'provider_id';

-- The result must fit in the current provider_id column.
SELECT MAX(
    CHAR_LENGTH(
        CONCAT(
            CASE
                WHEN LOCATE('_deleted_member_', provider_id) > 0
                    THEN SUBSTRING_INDEX(provider_id, '_deleted_member_', 1)
                WHEN LOCATE('_deleted', provider_id) > 0
                    THEN SUBSTRING_INDEX(provider_id, '_deleted', 1)
                WHEN LOCATE('_duplicate_', provider_id) > 0
                    THEN SUBSTRING_INDEX(provider_id, '_duplicate_', 1)
                ELSE provider_id
            END,
            '_deleted_member_',
            member_id
        )
    )
) AS max_normalized_provider_id_length
FROM member_entity
WHERE member_state = 'DELETED';

-- -----------------------------------------------------------------------------
-- 2. Recoverable, minimal backup: only columns touched by this migration are
--    copied. This avoids duplicating emails and OAuth/refresh tokens.
--    Safe to rerun because the primary key is preserved.
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS member_provider_backup_20260905 (
    member_id BIGINT NOT NULL PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    provider_id VARCHAR(512) NOT NULL,
    member_state VARCHAR(32) NOT NULL,
    backed_up_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO member_provider_backup_20260905 (
    member_id,
    provider,
    provider_id,
    member_state
)
SELECT member_id, provider, provider_id, member_state
FROM member_entity;

SELECT
    (SELECT COUNT(*) FROM member_entity) AS source_count,
    (SELECT COUNT(*) FROM member_provider_backup_20260905) AS backup_count;

-- Stop here unless source_count equals backup_count.

-- -----------------------------------------------------------------------------
-- 3. Normalize deleted identities. This transaction is idempotent and recoverable
--    with ROLLBACK until COMMIT is executed.
-- -----------------------------------------------------------------------------

START TRANSACTION;

SELECT member_id, provider, provider_id
FROM member_entity
WHERE member_state = 'DELETED'
FOR UPDATE;

UPDATE member_entity
SET provider_id = CONCAT(
        CASE
            WHEN LOCATE('_deleted_member_', provider_id) > 0
                THEN SUBSTRING_INDEX(provider_id, '_deleted_member_', 1)
            WHEN LOCATE('_deleted', provider_id) > 0
                THEN SUBSTRING_INDEX(provider_id, '_deleted', 1)
            WHEN LOCATE('_duplicate_', provider_id) > 0
                THEN SUBSTRING_INDEX(provider_id, '_duplicate_', 1)
            ELSE provider_id
        END,
        '_deleted_member_',
        member_id
    )
WHERE member_state = 'DELETED';

-- Must return no rows before COMMIT.
SELECT provider, provider_id, COUNT(*) AS member_count
FROM member_entity
GROUP BY provider, provider_id
HAVING COUNT(*) > 1;

-- Use ROLLBACK instead if the verification above returns any row.
COMMIT;

-- -----------------------------------------------------------------------------
-- 4. Final database invariant. Run each ALTER once; MySQL DDL implicitly commits.
--    Keep login traffic paused until this finishes.
-- -----------------------------------------------------------------------------

-- Preserve the production column types and collation while rejecting null
-- identities. Run only after invalid_provider_identity_count is 0.
ALTER TABLE member_entity
    MODIFY COLUMN provider ENUM('APPLE', 'KAKAO')
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    MODIFY COLUMN provider_id VARCHAR(255)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;

ALTER TABLE member_entity
    ADD CONSTRAINT uq_member_provider_provider_id
    UNIQUE (provider, provider_id);

-- -----------------------------------------------------------------------------
-- 5. Postflight verification.
-- -----------------------------------------------------------------------------

SHOW INDEX FROM member_entity
WHERE Key_name = 'uq_member_provider_provider_id';

SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'member_entity'
  AND COLUMN_NAME IN ('provider', 'provider_id');

SELECT provider, provider_id, COUNT(*) AS member_count
FROM member_entity
GROUP BY provider, provider_id
HAVING COUNT(*) > 1;

-- Expected failure: verify in a non-production copy only, never in production.
-- INSERT INTO member_entity (...) VALUES (...); -- duplicate provider/provider_id

-- -----------------------------------------------------------------------------
-- 6. Emergency rollback. Use only if the migration must be reverted.
--    Dropping the index is required first because the historical backup may
--    contain duplicate deleted tombstones.
-- -----------------------------------------------------------------------------

-- ALTER TABLE member_entity DROP INDEX uq_member_provider_provider_id;
-- ALTER TABLE member_entity
--     MODIFY COLUMN provider ENUM('APPLE', 'KAKAO')
--         CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
--     MODIFY COLUMN provider_id VARCHAR(255)
--         CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL;
-- START TRANSACTION;
-- UPDATE member_entity AS member
-- JOIN member_provider_backup_20260905 AS backup
--   ON backup.member_id = member.member_id
-- SET member.provider_id = backup.provider_id;
-- SELECT COUNT(*) AS unrestored_provider_id_count
-- FROM member_entity AS member
-- JOIN member_provider_backup_20260905 AS backup
--   ON backup.member_id = member.member_id
-- WHERE NOT (member.provider_id <=> backup.provider_id);
-- Review the count, then run exactly one of the following:
-- COMMIT;   -- only when unrestored_provider_id_count = 0
-- ROLLBACK; -- when the count is not 0

-- After the agreed recovery-retention period, remove the backup explicitly:
-- DROP TABLE member_provider_backup_20260905;
