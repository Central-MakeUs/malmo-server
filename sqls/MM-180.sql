-- MM-180: Add direct partner love type field to member_entity
-- Author: Codex
-- Date: 2026-03-16

ALTER TABLE member_entity
ADD COLUMN partner_love_type_category VARCHAR(255);
COMMENT ON COLUMN member_entity.partner_love_type_category IS 'Partner love type category: STABLE_TYPE, ANXIETY_TYPE, AVOIDANCE_TYPE, CONFUSION_TYPE, UNKNOWN';
