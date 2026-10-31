-- 系统参数与评审配置（H2 2.1 MODE=MySQL / MySQL 8 兼容）

CREATE TABLE system_config (
    config_key VARCHAR(64) PRIMARY KEY,
    config_value VARCHAR(500) NOT NULL,
    description VARCHAR(255),
    updated_by BIGINT,
    updated_at DATETIME
);

INSERT INTO system_config (config_key, config_value, description, updated_by, updated_at)
VALUES ('review.requiredPassCount', '2', '立项所需专家 PASS 人数（按专家去重，1-10）', NULL, CURRENT_TIMESTAMP);

ALTER TABLE project ADD COLUMN review_round INT NOT NULL DEFAULT 0;
UPDATE project SET review_round = 1 WHERE status = 'REVIEWING';
CREATE INDEX idx_project_review_round ON project_review (project_id, round);
