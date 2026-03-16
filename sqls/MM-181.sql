-- MM-181: Add love_type_mbti_prompt table for chat prompt enrichment
-- Author: Codex
-- Date: 2026-03-16

CREATE TABLE love_type_mbti_prompt (
    mbti VARCHAR(4) NOT NULL,
    lovetype VARCHAR(255) NOT NULL,
    prompts TEXT,
    PRIMARY KEY (mbti, lovetype)
);

COMMENT ON TABLE love_type_mbti_prompt IS 'MBTI and love type specific prompt snippets for chat metadata';
COMMENT ON COLUMN love_type_mbti_prompt.mbti IS 'MBTI in 4-letter uppercase format';
COMMENT ON COLUMN love_type_mbti_prompt.lovetype IS 'Love type category: STABLE_TYPE, ANXIETY_TYPE, AVOIDANCE_TYPE, CONFUSION_TYPE';
COMMENT ON COLUMN love_type_mbti_prompt.prompts IS 'Prompt content to inject into chat metadata';
