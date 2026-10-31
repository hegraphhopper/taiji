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
