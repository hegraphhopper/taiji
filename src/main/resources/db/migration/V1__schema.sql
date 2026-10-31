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
