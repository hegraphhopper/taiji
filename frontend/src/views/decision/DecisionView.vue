<template>
  <div class="page decision-page">
    <div class="page-heading">
      <div>
        <h1>新项目辅助决策</h1>
        <p>申报前看清重复度、重复点与差异点，给出建议动作与预计重复投入</p>
      </div>
      <div class="heading-actions">
        <el-button text @click="goRecords">决策记录</el-button>
      </div>
    </div>

    <div class="decision-grid">
      <section class="card form-card">
        <el-tabs v-model="mode" class="mode-tabs">
          <el-tab-pane label="选择项目" name="PROJECT">
            <el-form label-position="top" class="precheck-form">
              <el-form-item label="草稿或已提交项目">
                <el-select
                  v-model="projectId"
                  filterable
                  clearable
                  placeholder="按名称或编码搜索"
                  :loading="projectsLoading"
                  class="full"
                >
                  <el-option
                    v-for="item in projects"
                    :key="item.id"
                    :label="`${item.name}（${item.code}）`"
                    :value="item.id"
                  >
                    <span>{{ item.name }}</span>
                    <span class="option-meta">{{ item.code }} · {{ statusLabel(item.status) }}</span>
                  </el-option>
                </el-select>
              </el-form-item>
              <p class="form-hint">预检不会修改项目状态与重复度快照。</p>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="粘贴文本" name="TEXT">
            <el-form label-position="top" class="precheck-form">
              <el-form-item label="项目目标">
                <el-input v-model="textForm.goalText" type="textarea" :rows="2" maxlength="2000" show-word-limit />
              </el-form-item>
              <el-form-item label="研制内容">
                <el-input v-model="textForm.contentText" type="textarea" :rows="3" maxlength="4000" show-word-limit />
              </el-form-item>
              <el-form-item label="关键技术">
                <el-input v-model="textForm.techText" type="textarea" :rows="2" maxlength="2000" show-word-limit />
              </el-form-item>
              <div class="two-fields">
                <el-form-item label="技术方向">
                  <el-select v-model="textForm.techDirectionId" clearable placeholder="请选择" class="full">
                    <el-option v-for="item in dict.techDirections" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="预算（元，可选）">
                  <el-input-number v-model="textForm.budget" :min="0" :step="10000" controls-position="right" class="full" />
                </el-form-item>
              </div>
            </el-form>
          </el-tab-pane>
        </el-tabs>

        <div class="form-actions">
          <el-button type="primary" :loading="loading" @click="runPrecheck">开始预检</el-button>
          <el-button text @click="resetResult">清空结果</el-button>
        </div>
      </section>

      <section v-if="!result" class="card placeholder-card">
        <el-empty description="选择项目或粘贴文本后开始预检" :image-size="92" />
      </section>

      <section v-else class="result-column">
        <article class="card result-head">
          <RiskGauge :score="result.riskScore" :level="result.level" />
          <div class="result-summary">
            <div class="summary-top">
              <el-tag :type="levelType(result.level)" effect="dark" size="large">{{ levelLabel(result.level) }}</el-tag>
              <el-tag :type="actionType(result.recommendation)" effect="plain" size="large">
                {{ actionLabel(result.recommendation) }}
              </el-tag>
              <span v-if="result.llmUsed" class="llm-tag">大模型参与</span>
              <span v-else class="llm-tag">本地算法</span>
            </div>
            <p class="reason">{{ result.reason }}</p>
        <p v-if="topLlmReason" class="llm-reason">模型复核：{{ topLlmReason }}</p>
            <dl class="metrics">
              <div>
                <dt>预计重复投入</dt>
                <dd>{{ money(result.savingsEstimate) }}</dd>
              </div>
              <div>
                <dt>项目预算</dt>
                <dd>{{ money(result.budget) }}</dd>
              </div>
              <div>
                <dt>命中规则</dt>
                <dd>{{ result.ruleDescription || '—' }}</dd>
              </div>
            </dl>
            <div class="result-actions">
              <el-button type="primary" :loading="saving" @click="saveRecord">保存决策</el-button>
              <el-button text @click="goRecords">查看记录</el-button>
            </div>
          </div>
        </article>

        <article class="card section-card">
          <header class="section-head">
            <h2>Top 相似项目</h2>
            <p>按总分降序，展开可查看重复点与差异点</p>
          </header>
          <el-table :data="result.topPeers" size="small" border>
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="peer-detail">
                  <div>
                    <h4>重复点</h4>
                    <p v-if="!row.matchedPoints.length" class="empty-text">无明显重复片段</p>
                    <ul v-else>
                      <li v-for="(point, index) in row.matchedPoints" :key="`m-${index}`">
                        <strong>{{ dimensionLabel(point.dimension) }}</strong>
                        <span>{{ point.terms?.join('、') }}</span>
                        <em>{{ point.snippet }}</em>
                      </li>
                    </ul>
                  </div>
                  <div>
                    <h4>差异点</h4>
                    <p v-if="!row.diffPoints.length" class="empty-text">无差异片段</p>
                    <ul v-else>
                      <li v-for="(point, index) in row.diffPoints" :key="`d-${index}`">
                        <strong>{{ dimensionLabel(point.dimension) }}</strong>
                        <span>{{ point.terms?.join('、') }}</span>
                        <em>{{ point.snippet }}</em>
                      </li>
                    </ul>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="项目" min-width="220">
              <template #default="{ row }">{{ row.peer.name }}</template>
            </el-table-column>
            <el-table-column label="项目组" width="140">
              <template #default="{ row }">{{ row.peer.groupName || '—' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">{{ statusLabel(row.peer.status) }}</template>
            </el-table-column>
            <el-table-column label="总分" width="90" align="right">
              <template #default="{ row }">{{ Number(row.totalScore).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="等级" width="90">
              <template #default="{ row }">
                <el-tag size="small" effect="light" :type="levelType(row.level)">{{ row.level }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="归因" width="110">
              <template #default="{ row }">{{ directionLabel(row.direction) }}</template>
            </el-table-column>
          </el-table>
        </article>

        <article class="card section-card">
          <header class="section-head">
            <h2>建议评审专家</h2>
            <p>同技术方向优先，进行中评审负载低者靠前</p>
          </header>
          <p v-if="!result.suggestedExperts.length" class="empty-text">暂无可推荐专家，请联系基金办。</p>
          <div v-else class="expert-list">
            <el-tag v-for="expert in result.suggestedExperts" :key="expert.id" size="large" effect="plain">
              {{ expert.realName }}
              <span class="expert-meta">{{ expert.groupName || '未分组' }} · 在评 {{ expert.ongoingReviews }}</span>
            </el-tag>
          </div>
        </article>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProjects } from '@/api/project'
import { runDecisionPrecheck, saveDecision } from '@/api/decision'
import { useDictStore } from '@/stores/dict'
import type {
  DecisionAction,
  DecisionMode,
  DecisionPrecheckPayload,
  DecisionPrecheckResult,
  DecisionSavePayload,
  DuplicateLevel,
  ProjectListItem
} from '@/types'
import RiskGauge from '@/components/decision/RiskGauge.vue'

const router = useRouter()
const dict = useDictStore()

const mode = ref<DecisionMode>('PROJECT')
const projectId = ref<number | null>(null)
const projects = ref<ProjectListItem[]>([])
const projectsLoading = ref(false)
const textForm = reactive({
  goalText: '',
  contentText: '',
  techText: '',
  milestoneText: '',
  techDirectionId: null as number | null,
  budget: null as number | null
})
const result = ref<DecisionPrecheckResult | null>(null)
const topLlmReason = computed(() => result.value?.topPeers?.[0]?.llmReason || '')
const loading = ref(false)
const saving = ref(false)

const statusMap: Record<string, string> = {
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  REVIEWING: '评审中',
  APPROVED: '已批准',
  REJECTED: '已驳回',
  IN_PROGRESS: '进行中',
  COMPLETED: '已结项',
  TERMINATED: '已终止'
}

const dimensionMap: Record<string, string> = {
  goal: '项目目标',
  content: '研制内容',
  tech: '关键技术',
  milestone: '节点计划',
  tag: '技术标签'
}

function statusLabel(status: string): string {
  return statusMap[status] || status
}

function dimensionLabel(dimension: string): string {
  return dimensionMap[dimension] || dimension
}

function directionLabel(direction: string): string {
  if (direction === 'SELF_GROUP') return '本组内部'
  if (direction === 'PEER_EARLIER') return '他组在先'
  if (direction === 'OUR_EARLIER') return '我方在先'
  if (direction === 'SIMULTANEOUS') return '同期并行'
  return direction
}

function levelType(level: DuplicateLevel | string): 'danger' | 'warning' | 'info' | 'success' {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  if (level === 'LOW') return 'info'
  return 'success'
}

function levelLabel(level: DuplicateLevel | string): string {
  if (level === 'HIGH') return '高风险'
  if (level === 'MEDIUM') return '中风险'
  if (level === 'LOW') return '低风险'
  return '无明显重复'
}

function actionLabel(action: DecisionAction | string): string {
  if (action === 'APPROVE') return '可立项'
  if (action === 'EXPLAIN') return '补充差异说明'
  if (action === 'MERGE') return '建议合并'
  if (action === 'TERMINATE') return '建议终止/转向'
  return action
}

function actionType(action: DecisionAction | string): 'danger' | 'warning' | 'info' | 'success' {
  if (action === 'TERMINATE') return 'danger'
  if (action === 'MERGE') return 'warning'
  if (action === 'EXPLAIN') return 'info'
  return 'success'
}

function money(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '—'
  }
  return `${(Number(value) / 10000).toFixed(2)} 万元`
}

function buildPayload(): DecisionPrecheckPayload | null {
  if (mode.value === 'PROJECT') {
    if (!projectId.value) {
      ElMessage.warning('请选择需要预检的项目')
      return null
    }
    return { mode: 'PROJECT', projectId: projectId.value }
  }
  if (!textForm.goalText.trim() && !textForm.contentText.trim() && !textForm.techText.trim()) {
    ElMessage.warning('请至少填写目标、研制内容或关键技术')
    return null
  }
  return {
    mode: 'TEXT',
    goalText: textForm.goalText,
    contentText: textForm.contentText,
    techText: textForm.techText,
    milestoneText: textForm.milestoneText,
    techDirectionId: textForm.techDirectionId,
    budget: textForm.budget
  }
}

async function runPrecheck(): Promise<void> {
  const payload = buildPayload()
  if (!payload) {
    return
  }
  loading.value = true
  try {
    result.value = await runDecisionPrecheck(payload)
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '预检失败')
  } finally {
    loading.value = false
  }
}

async function saveRecord(): Promise<void> {
  if (!result.value) {
    return
  }
  const current = result.value
  const payload: DecisionSavePayload = {
    projectId: current.projectId,
    runId: current.runId,
    riskScore: current.riskScore,
    level: current.level,
    recommendation: current.recommendation,
    reason: current.reason,
    similarProjects: JSON.stringify(current.topPeers),
    savingsEstimate: current.savingsEstimate,
    inputHash: current.inputHash
  }
  saving.value = true
  try {
    await saveDecision(payload)
    ElMessage.success('决策记录已保存')
    await router.push('/decision/records')
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function resetResult(): void {
  result.value = null
}

function goRecords(): void {
  void router.push('/decision/records')
}

onMounted(async () => {
  projectsLoading.value = true
  try {
    const page = await getProjects({
      page: 1,
      size: 200,
      sortBy: 'updatedAt',
      sortOrder: 'desc'
    })
    projects.value = page.records
    dict.load().catch(() => undefined)
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '项目列表加载失败')
  } finally {
    projectsLoading.value = false
  }
})
</script>

<style scoped>
.decision-grid {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.form-card {
  padding: 18px;
  position: sticky;
  top: 16px;
}

.mode-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}

.precheck-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.form-hint {
  margin: 0;
  font-size: 12px;
  color: var(--ink-faint);
}

.full {
  width: 100%;
}

.two-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.form-actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.placeholder-card {
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.result-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-head {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 8px;
  padding: 12px 20px 18px;
  align-items: center;
}

.result-summary {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.summary-top {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.llm-tag {
  font-size: 12px;
  color: var(--ink-faint);
}

.llm-reason {
  margin: 6px 0 0;
  color: var(--ink-muted);
  font-size: 13px;
}

.reason {
  margin: 0;
  line-height: 1.7;
  color: var(--ink);
  font-size: 13px;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 0;
}

.metrics dt {
  font-size: 12px;
  color: var(--ink-faint);
}

.metrics dd {
  margin: 4px 0 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-strong);
}

.result-actions {
  display: flex;
  gap: 8px;
}

.section-card {
  padding: 16px 18px 20px;
}

.section-head h2 {
  margin: 0;
  font-size: 15px;
  color: var(--ink-strong);
}

.section-head p {
  margin: 2px 0 10px;
  font-size: 12px;
  color: var(--ink-muted);
}

.peer-detail {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  padding: 8px 20px 12px;
}

.peer-detail h4 {
  margin: 0 0 6px;
  font-size: 13px;
  color: var(--ink-strong);
}

.peer-detail ul {
  margin: 0;
  padding-left: 18px;
  color: var(--ink-muted);
  font-size: 12px;
  line-height: 1.7;
}

.peer-detail li span {
  margin-left: 8px;
  color: var(--ink);
}

.peer-detail li em {
  display: block;
  font-style: normal;
  color: var(--ink-faint);
}

.empty-text {
  margin: 0;
  color: var(--ink-faint);
  font-size: 12px;
}

.expert-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.expert-meta {
  margin-left: 6px;
  font-size: 11px;
  color: var(--ink-faint);
}

.option-meta {
  float: right;
  margin-left: 12px;
  font-size: 11px;
  color: var(--ink-faint);
}

@media (max-width: 1180px) {
  .decision-grid {
    grid-template-columns: 1fr;
  }

  .form-card {
    position: static;
  }

  .result-head {
    grid-template-columns: 1fr;
  }
}
</style>
