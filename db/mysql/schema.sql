-- MySQL 8 schema for 自投研发项目重复投入治理平台
-- Generated from Flyway V1-V8; demo data is created by DemoDataInitializer.
-- Usage: mysql -u root -p < db/mysql/schema.sql

-- >>> V1__schema.sql
CREATE TABLE sys_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    parent_id BIGINT,
    leader VARCHAR(50),
    sort INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE UNIQUE INDEX uk_sys_group_code ON sys_group (code);
CREATE INDEX idx_sys_group_parent ON sys_group (parent_id);

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    group_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    fail_count INT NOT NULL DEFAULT 0,
    locked_until DATETIME,
    last_login_at DATETIME,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE UNIQUE INDEX uk_sys_user_username ON sys_user (username);
CREATE INDEX idx_sys_user_group_status ON sys_user (group_id, status);

CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    created_at DATETIME,
    updated_at DATETIME
);

CREATE UNIQUE INDEX uk_sys_role_code ON sys_role (code);

CREATE TABLE sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE UNIQUE INDEX uk_sys_user_role ON sys_user_role (user_id, role_id);
CREATE INDEX idx_sys_user_role_role ON sys_user_role (role_id);

CREATE TABLE tech_direction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    keywords VARCHAR(500),
    sort INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE UNIQUE INDEX uk_tech_direction_code ON tech_direction (code);
CREATE INDEX idx_tech_direction_parent ON tech_direction (parent_id);

CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50),
    resource_id VARCHAR(100),
    detail VARCHAR(2000),
    ip VARCHAR(64),
    result VARCHAR(20),
    cost_ms BIGINT,
    created_at DATETIME
);

CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);
CREATE INDEX idx_audit_log_user_id ON audit_log (user_id);
CREATE INDEX idx_audit_log_action ON audit_log (action);


-- >>> V2__seed_core.sql
INSERT INTO sys_role (code, name, description, created_at, updated_at) VALUES
('ADMIN', '系统管理员', '用户、角色、项目组、技术方向、分析权重与审计管理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PMO', '基金办', '全所项目查看、形式审查、重复度分析、趋势归因与报告导出', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('GROUP_USER', '项目组用户', '维护本组项目与申报书、上传附件、发起预检、查看本组分析结果', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EXPERT', '评审专家', '查看被指派项目的重复度报告并填写评审意见', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sys_group (name, code, parent_id, leader, sort, enabled, deleted, created_at, updated_at) VALUES
('智能计算项目组', 'GRP-INTELLIGENCE', NULL, '张伟', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('数据治理项目组', 'GRP-DATA', NULL, '李静', 2, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('网络安全项目组', 'GRP-SECURITY', NULL, '王强', 3, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('嵌入式软件项目组', 'GRP-EMBEDDED', NULL, '刘洋', 4, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('工业软件项目组', 'GRP-INDUSTRY', NULL, '陈晨', 5, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('通信网络项目组', 'GRP-NETWORK', NULL, '赵雷', 6, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('系统集成项目组', 'GRP-INTEGRATION', NULL, '孙丽', 7, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('前沿技术项目组', 'GRP-FRONTIER', NULL, '周涛', 8, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO tech_direction (code, name, parent_id, keywords, sort, enabled, deleted, created_at, updated_at) VALUES
('TD-001', '人工智能', NULL, '机器学习,深度学习,大模型,算法模型,智能推理', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TD-002', '大数据', NULL, '数据湖,数据治理,数据仓库,流式计算,数据分析', 2, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TD-003', '云计算', NULL, '容器,微服务,云原生,虚拟化,资源调度', 3, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TD-004', '边缘计算', NULL, '边缘节点,端侧推理,低时延,协同计算', 4, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TD-005', '网络安全', NULL, '态势感知,入侵检测,零信任,漏洞治理,安全审计', 5, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TD-006', '密码技术', NULL, '密码算法,密钥管理,国密,身份认证,数据加密', 6, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TD-007', '嵌入式', NULL, '实时系统,嵌入式软件,驱动开发,硬件适配', 7, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TD-008', '工业软件', NULL, '仿真建模,工业控制,数字工厂,工艺优化', 8, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TD-009', '数字孪生', NULL, '三维建模,实时映射,仿真推演,状态监测', 9, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TD-010', '通信协议', NULL, '协议栈,信号处理,组网,传输优化', 10, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TD-011', '系统集成', NULL, '系统联调,接口集成,集成测试,工程实施', 11, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TD-012', '可靠性工程', NULL, '可靠性设计,故障诊断,容错,寿命评估', 12, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sys_user (username, password_hash, real_name, email, phone, group_id, status, fail_count, deleted, created_at, updated_at) VALUES
('admin', 'INIT', '系统管理员', 'admin@taiji.example', NULL, NULL, 'ENABLED', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('pmo01', 'INIT', '基金办专员', 'pmo01@taiji.example', NULL, NULL, 'ENABLED', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user01', 'INIT', '项目组用户', 'user01@taiji.example', NULL, (SELECT id FROM sys_group WHERE code = 'GRP-INTELLIGENCE'), 'ENABLED', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('expert01', 'INIT', '评审专家', 'expert01@taiji.example', NULL, NULL, 'ENABLED', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sys_user_role (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.code = 'ADMIN';

INSERT INTO sys_user_role (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM sys_user u, sys_role r
WHERE u.username = 'pmo01' AND r.code = 'PMO';

INSERT INTO sys_user_role (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM sys_user u, sys_role r
WHERE u.username = 'user01' AND r.code = 'GROUP_USER';

INSERT INTO sys_user_role (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM sys_user u, sys_role r
WHERE u.username = 'expert01' AND r.code = 'EXPERT';


-- >>> V3__project_schema.sql
CREATE TABLE project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    fund_type VARCHAR(20) NOT NULL,
    group_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    tech_direction_id BIGINT,
    budget DECIMAL(14, 2) NOT NULL,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    summary TEXT,
    expected_outcome TEXT,
    duplicate_score DECIMAL(5, 2),
    risk_level VARCHAR(20),
    submit_time DATETIME,
    created_by BIGINT NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_project_code ON project (code);
CREATE INDEX idx_project_group_status ON project (group_id, status);
CREATE INDEX idx_project_tech_direction ON project (tech_direction_id);
CREATE INDEX idx_project_submit_time ON project (submit_time);

CREATE TABLE project_goal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    sort INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_project_goal_project ON project_goal (project_id);

CREATE TABLE project_content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    sort INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_project_content_project ON project_content (project_id);

CREATE TABLE project_key_tech (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    difficulty VARCHAR(20),
    sort INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_project_key_tech_project ON project_key_tech (project_id);

CREATE TABLE project_milestone (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    plan_start DATE,
    plan_end DATE,
    deliverable TEXT,
    weight INT NOT NULL DEFAULT 0,
    sort INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_project_milestone_project ON project_milestone (project_id);

CREATE TABLE project_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    member_role VARCHAR(20) NOT NULL,
    workload DECIMAL(6, 2) NOT NULL
);

CREATE INDEX idx_project_member_project ON project_member (project_id);
CREATE INDEX idx_project_member_user ON project_member (user_id);

CREATE TABLE project_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(100) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    text_content LONGTEXT,
    parse_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    parse_message VARCHAR(500),
    uploaded_by BIGINT,
    created_at DATETIME
);

CREATE INDEX idx_project_document_project ON project_document (project_id);

CREATE TABLE project_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    round INT NOT NULL DEFAULT 1,
    score DECIMAL(5, 2),
    opinion TEXT,
    result VARCHAR(20),
    created_at DATETIME
);

CREATE INDEX idx_project_review_project ON project_review (project_id);
CREATE INDEX idx_project_review_reviewer ON project_review (reviewer_id);

CREATE TABLE project_approval_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    action VARCHAR(30) NOT NULL,
    operator_id BIGINT NOT NULL,
    comment VARCHAR(1000),
    created_at DATETIME
);

CREATE INDEX idx_project_approval_log_project ON project_approval_log (project_id);


-- >>> V4__analysis_schema.sql
-- 重复度分析引擎 schema（H2 2.1 MODE=MySQL / MySQL 8 兼容）
-- 字段权重与阈值以 JSON 字符串存储，避免 JSON 列类型；时间统一 DATETIME。

CREATE TABLE analysis_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version INT NOT NULL,
    field_weights TEXT NOT NULL,
    thresholds TEXT NOT NULL,
    top_k INT NOT NULL DEFAULT 5,
    simultaneous_days INT NOT NULL DEFAULT 30,
    llm_enabled TINYINT NOT NULL DEFAULT 0,
    local_weight INT NOT NULL DEFAULT 70,
    llm_weight INT NOT NULL DEFAULT 30,
    llm_top_n INT NOT NULL DEFAULT 5,
    llm_timeout_ms INT NOT NULL DEFAULT 8000,
    active TINYINT NOT NULL DEFAULT 1,
    created_by BIGINT,
    created_at DATETIME
);

CREATE UNIQUE INDEX uk_analysis_config_version ON analysis_config (version);
CREATE INDEX idx_analysis_config_active ON analysis_config (active);

INSERT INTO analysis_config (
    version, field_weights, thresholds, top_k, simultaneous_days,
    llm_enabled, local_weight, llm_weight, llm_top_n, llm_timeout_ms,
    active, created_by, created_at
) VALUES (
    1,
    '{"goal":15,"content":35,"tech":35,"milestone":5,"tag":10}',
    '{"high":80,"medium":60,"low":40}',
    5, 30, 0, 70, 30, 5, 8000, 1, NULL, CURRENT_TIMESTAMP
);

CREATE TABLE analysis_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trigger_type VARCHAR(20) NOT NULL,
    project_id BIGINT,
    config_version INT NOT NULL,
    analysis_date DATETIME,
    status VARCHAR(20) NOT NULL,
    project_count INT NOT NULL DEFAULT 0,
    pair_count INT NOT NULL DEFAULT 0,
    llm_used TINYINT NOT NULL DEFAULT 0,
    duration_ms BIGINT,
    message TEXT,
    operator_id BIGINT,
    created_at DATETIME
);

CREATE INDEX idx_analysis_run_created ON analysis_run (created_at);
CREATE INDEX idx_analysis_run_status ON analysis_run (status);
CREATE INDEX idx_analysis_run_project ON analysis_run (project_id);

CREATE TABLE analysis_pair (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    peer_project_id BIGINT NOT NULL,
    goal_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    content_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    tech_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    milestone_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    tag_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    local_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    llm_score DECIMAL(5, 2),
    total_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    level VARCHAR(20) NOT NULL,
    relation VARCHAR(20) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    earlier_project_id BIGINT,
    matched_points TEXT,
    diff_points TEXT,
    llm_reason TEXT,
    created_at DATETIME
);

CREATE INDEX idx_analysis_pair_run_project ON analysis_pair (run_id, project_id, total_score);
CREATE INDEX idx_analysis_pair_peer ON analysis_pair (peer_project_id);

CREATE TABLE duplicate_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    group_id BIGINT,
    analysis_date DATE NOT NULL,
    max_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    avg_top_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    high_pair_count INT NOT NULL DEFAULT 0,
    cross_group_count INT NOT NULL DEFAULT 0,
    self_group_count INT NOT NULL DEFAULT 0,
    our_earlier_count INT NOT NULL DEFAULT 0,
    peer_earlier_count INT NOT NULL DEFAULT 0,
    simultaneous_count INT NOT NULL DEFAULT 0,
    risk_exposure DECIMAL(14, 2),
    created_at DATETIME
);

CREATE INDEX idx_snapshot_project_date ON duplicate_snapshot (project_id, analysis_date);
CREATE INDEX idx_snapshot_group_date ON duplicate_snapshot (group_id, analysis_date);
CREATE INDEX idx_snapshot_run ON duplicate_snapshot (run_id);

CREATE TABLE llm_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT,
    project_id BIGINT,
    peer_project_id BIGINT,
    model VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    latency_ms BIGINT,
    prompt_tokens INT,
    completion_tokens INT,
    error VARCHAR(500),
    created_at DATETIME
);

CREATE INDEX idx_llm_call_log_run ON llm_call_log (run_id);
CREATE INDEX idx_llm_call_log_pair ON llm_call_log (project_id, peer_project_id);


-- >>> V5__decision_schema.sql
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


-- >>> V6__system_config.sql
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


-- >>> V7__llm_pair_detail.sql
-- 大模型解释层：相似对复核点（H2 2.1 MODE=MySQL / MySQL 8 兼容）

ALTER TABLE analysis_pair ADD COLUMN llm_overlaps TEXT;
ALTER TABLE analysis_pair ADD COLUMN llm_differences TEXT;


-- >>> V8__llm_explanation_default.sql
-- 大模型解释层默认口径：本地 100 / 大模型 0；仅迁移未启用大模型的旧默认配置。
UPDATE analysis_config
SET local_weight = 100,
    llm_weight = 0
WHERE local_weight = 70
  AND llm_weight = 30
  AND llm_enabled = 0;


