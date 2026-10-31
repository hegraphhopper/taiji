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
