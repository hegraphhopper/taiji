<template>
  <div class="page rules-page">
    <div class="page-heading">
      <div>
        <h1>决策规则配置</h1>
        <p>优先级越小越先匹配；分数区间左闭右开，100 为闭区间；保存后立即生效</p>
      </div>
      <div class="heading-actions">
        <el-button text @click="goRecords">返回决策记录</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存规则</el-button>
      </div>
    </div>

    <section class="card rules-card">
      <div class="rules-toolbar">
        <el-button plain @click="addRule">新增规则</el-button>
        <span class="toolbar-hint">必须覆盖 0–100 全部分数区间，否则保存会被拒绝。</span>
      </div>

      <el-table :data="rules" size="small" border row-key="key">
        <el-table-column label="优先级" width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.priority" :min="1" :step="1" controls-position="right" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="最低分" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.minScore" :min="0" :max="100" :step="1" controls-position="right" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="最高分" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.maxScore" :min="0" :max="100" :step="1" controls-position="right" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="归因条件" width="150">
          <template #default="{ row }">
            <el-select v-model="row.relation" clearable placeholder="任意" size="small">
              <el-option label="他组在先" value="PEER_EARLIER" />
              <el-option label="我方在先" value="OUR_EARLIER" />
              <el-option label="同期并行" value="SIMULTANEOUS" />
              <el-option label="本组内部" value="SELF_GROUP" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="同组条件" width="120">
          <template #default="{ row }">
            <el-select v-model="row.sameGroup" clearable placeholder="任意" size="small">
              <el-option label="仅同组" :value="1" />
              <el-option label="仅跨组" :value="0" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="建议动作" width="150">
          <template #default="{ row }">
            <el-select v-model="row.action" size="small">
              <el-option label="可立项" value="APPROVE" />
              <el-option label="补充差异说明" value="EXPLAIN" />
              <el-option label="建议合并" value="MERGE" />
              <el-option label="建议终止/转向" value="TERMINATE" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="220">
          <template #default="{ row }">
            <el-input v-model="row.description" size="small" maxlength="200" />
          </template>
        </el-table-column>
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" :active-value="1" :inactive-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeRule($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDecisionRules, saveDecisionRules } from '@/api/decision'
import type { DecisionAction, DecisionRuleItem } from '@/types'

interface EditableRule extends DecisionRuleItem {
  key: number
}

const router = useRouter()
const rules = ref<EditableRule[]>([])
const saving = ref(false)
let keySeed = 1

function toEditable(item: DecisionRuleItem): EditableRule {
  return {
    key: keySeed++,
    id: item.id,
    priority: item.priority,
    minScore: Number(item.minScore),
    maxScore: Number(item.maxScore),
    relation: item.relation || null,
    sameGroup: item.sameGroup ?? null,
    action: item.action,
    description: item.description || '',
    enabled: item.enabled ?? 1
  }
}

async function load(): Promise<void> {
  try {
    const data = await getDecisionRules()
    rules.value = data.map(toEditable)
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '规则加载失败')
  }
}

function addRule(): void {
  rules.value.push({
    key: keySeed++,
    priority: 50,
    minScore: 0,
    maxScore: 100,
    relation: null,
    sameGroup: null,
    action: 'EXPLAIN' as DecisionAction,
    description: '新增规则',
    enabled: 1
  })
}

function removeRule(index: number): void {
  rules.value.splice(index, 1)
}

async function save(): Promise<void> {
  if (!rules.value.length) {
    ElMessage.warning('至少保留一条规则')
    return
  }
  saving.value = true
  try {
    const payload: DecisionRuleItem[] = rules.value.map((item) => ({
      id: item.id,
      priority: item.priority,
      minScore: item.minScore,
      maxScore: item.maxScore,
      relation: item.relation || null,
      sameGroup: item.sameGroup ?? null,
      action: item.action,
      description: item.description,
      enabled: item.enabled
    }))
    const saved = await saveDecisionRules(payload)
    rules.value = saved.map(toEditable)
    ElMessage.success('规则已保存并生效')
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '规则保存失败')
  } finally {
    saving.value = false
  }
}

function goRecords(): void {
  void router.push('/decision/records')
}

onMounted(load)
</script>

<style scoped>
.rules-card {
  padding: 14px 16px 18px;
}

.rules-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.toolbar-hint {
  font-size: 12px;
  color: var(--ink-faint);
}
</style>
