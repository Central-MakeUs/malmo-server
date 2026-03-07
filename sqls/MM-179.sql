-- MM-179: Add relationship_status, personality_type, and other_personality_type to member_entity
-- Author: Claude Code
-- Date: 2026-02-09

-- Add relationship_status column (ENUM stored as VARCHAR)
-- Possible values: 'IN_RELATIONSHIP', 'SEEING_SOMEONE', 'BREAKUP'
ALTER TABLE member_entity
ADD COLUMN relationship_status VARCHAR(255);

-- Add personality_type column
ALTER TABLE member_entity
ADD COLUMN personality_type VARCHAR(255);

-- Add other_personality_type column
ALTER TABLE member_entity
ADD COLUMN other_personality_type VARCHAR(255);

-- Add comment for documentation
COMMENT ON COLUMN member_entity.relationship_status IS 'Relationship status: IN_RELATIONSHIP, SEEING_SOMEONE, BREAKUP';
COMMENT ON COLUMN member_entity.personality_type IS 'Member personality type';
COMMENT ON COLUMN member_entity.other_personality_type IS 'Partner personality type';

-- Optional: Add check constraint to ensure valid enum values
-- Uncomment if you want to enforce enum values at database level
-- ALTER TABLE member_entity
-- ADD CONSTRAINT chk_relationship_status
-- CHECK (relationship_status IN ('IN_RELATIONSHIP', 'SEEING_SOMEONE', 'BREAKUP') OR relationship_status IS NULL);
