-- MM-182: Create structured weekly analysis report tables
-- Author: Codex
-- Date: 2026-04-02

CREATE TABLE weekly_analysis_report (
    weekly_analysis_report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    status VARCHAR(64) NOT NULL,
    source_chat_room_count INT NOT NULL DEFAULT 0,
    eligible_chat_room_count INT NOT NULL DEFAULT 0,
    source_user_message_count INT NOT NULL DEFAULT 0,
    schema_version VARCHAR(32) NULL,
    timezone VARCHAR(64) NULL,
    overview_title TEXT NULL,
    overview_summary TEXT NULL,
    mood_dominant_period VARCHAR(32) NULL,
    mood_ratio_morning DOUBLE NULL,
    mood_ratio_afternoon DOUBLE NULL,
    mood_ratio_evening DOUBLE NULL,
    mood_ratio_late_night DOUBLE NULL,
    mood_description TEXT NULL,
    conflict_score INT NULL,
    conflict_description TEXT NULL,
    behavior_pattern_one_line_summary TEXT NULL,
    behavior_pattern_trigger_situation TEXT NULL,
    behavior_pattern_belief TEXT NULL,
    behavior_pattern_response_type TEXT NULL,
    solution_title TEXT NULL,
    solution_content TEXT NULL,
    generated_at DATETIME NULL,
    failed_reason VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    modified_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    CONSTRAINT uq_weekly_analysis_report_member_week UNIQUE (member_id, week_start_date)
);

CREATE TABLE weekly_analysis_report_top_topic (
    weekly_analysis_report_id BIGINT NOT NULL,
    topic_rank INT NOT NULL,
    keyword TEXT NOT NULL,
    weight DOUBLE NOT NULL,
    description TEXT NOT NULL,
    CONSTRAINT fk_weekly_analysis_report_top_topic_report
        FOREIGN KEY (weekly_analysis_report_id)
        REFERENCES weekly_analysis_report (weekly_analysis_report_id)
);

CREATE INDEX idx_weekly_analysis_report_top_topic_report_rank
    ON weekly_analysis_report_top_topic (weekly_analysis_report_id, topic_rank);
