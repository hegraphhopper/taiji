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
