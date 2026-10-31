-- 新项目辅助决策 schema（H2 2.1 MODE=MySQL / MySQL 8 兼容）

CREATE TABLE decision_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    priority INT NOT NULL,
    min_score DECIMAL(5, 2) NOT NULL,
    max_score DECIMAL(5, 2) NOT NULL,
    relation VARCHAR(20),
    same_group TINYINT,
    action VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE INDEX idx_decision_rule_priority ON decision_rule (priority);

CREATE TABLE decision_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    run_id BIGINT,
    risk_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    level VARCHAR(10) NOT NULL,
    recommendation VARCHAR(20) NOT NULL,
    reason TEXT,
    similar_projects TEXT,
    savings_estimate DECIMAL(14, 2),
    input_hash VARCHAR(64),
    operator_id BIGINT,
    created_at DATETIME,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_decision_record_project ON decision_record (project_id);
CREATE INDEX idx_decision_record_created ON decision_record (created_at);
CREATE INDEX idx_decision_record_operator ON decision_record (operator_id);

-- 默认规则：priority 越小越先匹配；max_score 右开区间，100 表示包含 100。
INSERT INTO decision_rule (priority, min_score, max_score, relation, same_group,
                           action, description, enabled, created_at, updated_at) VALUES
(10, 90.00, 100.00, NULL, NULL, 'TERMINATE', '重复度 ≥90，核心内容高度重合，建议终止或转向', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(20, 80.00, 90.00, NULL, 1, 'MERGE', '重复度 ≥80 且同组申报，建议合并', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(21, 80.00, 90.00, 'PEER_EARLIER', NULL, 'MERGE', '重复度 ≥80 且他组在先，建议合并或说明差异', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(30, 60.00, 100.00, NULL, NULL, 'EXPLAIN', '重复度中等，建议补充差异说明后评审', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(40, 0.00, 60.00, NULL, NULL, 'APPROVE', '重复度较低，可正常立项', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
