-- MM-181: Add love_type_personality_type_prompt table for chat prompt enrichment
-- Author: Codex
-- Date: 2026-03-16

CREATE TABLE love_type_personality_type_prompt (
    personality_type VARCHAR(4) NOT NULL,
    lovetype VARCHAR(255) NOT NULL,
    prompts TEXT,
    PRIMARY KEY (personality_type, lovetype)
);

COMMENT ON TABLE love_type_personality_type_prompt IS 'Personality type and love type specific prompt snippets for chat metadata';
COMMENT ON COLUMN love_type_personality_type_prompt.personality_type IS 'MBTI in 4-letter uppercase format';
COMMENT ON COLUMN love_type_personality_type_prompt.lovetype IS 'Love type category: STABLE_TYPE, ANXIETY_TYPE, AVOIDANCE_TYPE, CONFUSION_TYPE';
COMMENT ON COLUMN love_type_personality_type_prompt.prompts IS 'Prompt content to inject into chat metadata';
