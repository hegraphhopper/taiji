-- 大模型解释层默认口径：本地 100 / 大模型 0，模型分不参与总分。
-- 仅迁移仍为旧默认值（70/30）且未启用大模型的配置，避免覆盖显式配置。
UPDATE analysis_config
SET local_weight = 100,
    llm_weight = 0
WHERE local_weight = 70
  AND llm_weight = 30
  AND llm_enabled = 0;
