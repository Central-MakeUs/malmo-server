-- MM-188 release migration
-- Target DB: MySQL 8.x
--
-- Applies the schema required by:
-- 1. Direct partner love type persistence on member_entity
-- 2. MBTI + love type prompt enrichment
-- 3. MBTI + love type detailed result lookup
--
-- NOTE:
-- - Run this once before deploying the application version that contains MM-188.
-- - This file intentionally replaces the split MM-180/MM-181 migration contents for release deployment.
-- - Production uses spring.jpa.hibernate.ddl-auto=none, so both tables below must exist before traffic reaches
--   /love-types/result or chat prompt enrichment paths.

ALTER TABLE member_entity
    ADD COLUMN partner_love_type_category VARCHAR(255) NULL
        COMMENT 'Partner love type category: STABLE_TYPE, ANXIETY_TYPE, AVOIDANCE_TYPE, CONFUSION_TYPE, UNKNOWN';

CREATE TABLE love_type_personality_type_prompt (
    personality_type VARCHAR(4) NOT NULL COMMENT 'MBTI in 4-letter uppercase format',
    lovetype VARCHAR(255) NOT NULL COMMENT 'Love type category: STABLE_TYPE, ANXIETY_TYPE, AVOIDANCE_TYPE, CONFUSION_TYPE',
    prompts TEXT NULL COMMENT 'Prompt content to inject into chat metadata',
    PRIMARY KEY (personality_type, lovetype)
) COMMENT='Personality type and love type specific prompt snippets for chat metadata';

CREATE TABLE love_type_personality_type_feature (
    personality_type VARCHAR(4) NOT NULL COMMENT 'MBTI in 4-letter uppercase format',
    lovetype VARCHAR(255) NOT NULL COMMENT 'Love type category: STABLE_TYPE, ANXIETY_TYPE, AVOIDANCE_TYPE, CONFUSION_TYPE',
    summary TEXT NULL,
    keyword1 VARCHAR(255) NULL,
    keyword2 VARCHAR(255) NULL,
    keyword3 VARCHAR(255) NULL,
    strength1 VARCHAR(255) NULL,
    strength2 VARCHAR(255) NULL,
    strength3 VARCHAR(255) NULL,
    weakness VARCHAR(255) NULL,
    strength_desc1 TEXT NULL,
    strength_desc2 TEXT NULL,
    strength_desc3 TEXT NULL,
    weakness_desc TEXT NULL,
    pattern_title1 VARCHAR(255) NULL,
    pattern_title2 VARCHAR(255) NULL,
    pattern_title3 VARCHAR(255) NULL,
    pattern_title4 VARCHAR(255) NULL,
    pattern1 TEXT NULL,
    pattern2 TEXT NULL,
    pattern3 TEXT NULL,
    pattern4 TEXT NULL,
    lovetype_feature_title1 VARCHAR(255) NULL,
    lovetype_feature_title2 VARCHAR(255) NULL,
    lovetype_feature_title3 VARCHAR(255) NULL,
    lovetype_feature_title4 VARCHAR(255) NULL,
    lovetype_feature1 TEXT NULL,
    lovetype_feature2 TEXT NULL,
    lovetype_feature3 TEXT NULL,
    lovetype_feature4 TEXT NULL,
    dating_guide1 TEXT NULL,
    dating_guide2 TEXT NULL,
    dating_guide3 TEXT NULL,
    best_personality_type1 VARCHAR(4) NULL,
    best_desc1 TEXT NULL,
    best_personality_type2 VARCHAR(4) NULL,
    best_desc2 TEXT NULL,
    worst_personality_type1 VARCHAR(4) NULL,
    worst_desc1 TEXT NULL,
    worst_personality_type2 VARCHAR(4) NULL,
    worst_desc2 TEXT NULL,
    PRIMARY KEY (personality_type, lovetype)
) COMMENT='Detailed result content for MBTI and love type combinations';

-- Required production seed data:
-- Insert rows for all supported personality_type/lovetype combinations before enabling the related features.
-- The repository currently contains only test sample content, so real production copy/data should be loaded from
-- the product-approved source of truth.
--
-- Minimum expected coverage:
-- - love_type_personality_type_prompt: rows used by ChatPromptBuilder for user/partner prompt enrichment.
-- - love_type_personality_type_feature: rows served by GET /love-types/result.
--
-- Verification queries:
-- SELECT COUNT(*) AS prompt_count FROM love_type_personality_type_prompt;
-- SELECT COUNT(*) AS feature_count FROM love_type_personality_type_feature;
