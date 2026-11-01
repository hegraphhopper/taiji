-- 大模型解释层：相似对复核点（H2 2.1 MODE=MySQL / MySQL 8 兼容）

ALTER TABLE analysis_pair ADD COLUMN llm_overlaps TEXT;
ALTER TABLE analysis_pair ADD COLUMN llm_differences TEXT;
