-- MM-180: Add direct partner profile fields to member_entity
-- Author: Codex
-- Date: 2026-03-16

ALTER TABLE member_entity
ADD COLUMN mbti VARCHAR(4);

ALTER TABLE member_entity
ADD COLUMN partner_mbti VARCHAR(4);

ALTER TABLE member_entity
ADD COLUMN partner_love_type_category VARCHAR(255);

UPDATE member_entity
SET mbti = UPPER(personality_type)
WHERE personality_type IS NOT NULL
  AND mbti IS NULL;

UPDATE member_entity
SET partner_mbti = UPPER(other_personality_type)
WHERE other_personality_type IS NOT NULL
  AND partner_mbti IS NULL;

COMMENT ON COLUMN member_entity.mbti IS 'Member MBTI';
COMMENT ON COLUMN member_entity.partner_mbti IS 'Partner MBTI entered directly by member';
COMMENT ON COLUMN member_entity.partner_love_type_category IS 'Partner love type category: STABLE_TYPE, ANXIETY_TYPE, AVOIDANCE_TYPE, CONFUSION_TYPE, UNKNOWN';
