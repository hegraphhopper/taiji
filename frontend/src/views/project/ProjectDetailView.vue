<template>
  <div class="page detail-page" v-loading="loading">
    <div class="page-heading">
      <div class="title-block">
        <el-button text :icon="ArrowLeft" @click="router.push('/projects')">返回列表</el-button>
        <div class="title-row">
          <h1>{{ project?.name || '项目详情' }}</h1>
          <el-tag v-if="project" :type="statusTagType(project.status)" effect="light">
            {{ statusLabel(project.status) }}
          </el-tag>
          <span v-if="project" class="project-code">{{ project.code }}</span>
        </div>
        <p v-if="project" class="subline">
          {{ project.groupName || '未归属' }} · {{ project.techDirectionName || '未选方向' }} ·
          预算 {{ formatMoney(project.budget) }} 元
        </p>
      </div>
      <div class="heading-actions" v-if="project">
        <el-button v-if="canEdit" :icon="Edit" @click="router.push(`/projects/${project.id}/edit`)">编辑</el-button>
        <el-button v-if="canSubmit" type="success" @click="doAction('SUBMIT', '提交')">提交</el-button>
        <el-button v-if="canWithdraw" type="warning" @click="doAction('WITHDRAW', '撤回')">撤回</el-button>
        <el-button v-if="canApprove" type="primary" @click="doAction('APPROVE', '确认立项')">确认立项</el-button>
        <el-button v-if="canApprove" type="danger" plain @click="rejectVisible = true">驳回</el-button>
      </div>
    </div>

    <template v-if="project">
      <section class="card section-card">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="概览" name="overview">
            <el-descriptions :column="3" border size="small">
              <el-descriptions-item label="项目编码">{{ project.code }}</el-descriptions-item>
              <el-descriptions-item label="基金类型">{{ fundLabel(project.fundType) }}</el-descriptions-item>
              <el-descriptions-item label="负责人">{{ project.ownerName || '—' }}</el-descriptions-item>
              <el-descriptions-item label="项目组">{{ project.groupName || '—' }}</el-descriptions-item>
              <el-descriptions-item label="技术方向">{{ project.techDirectionName || '—' }}</el-descriptions-item>
              <el-descriptions-item label="预算">{{ formatMoney(project.budget) }} 元</el-descriptions-item>
              <el-descriptions-item label="开始日期">{{ project.startDate || '—' }}</el-descriptions-item>
              <el-descriptions-item label="结束日期">{{ project.endDate || '—' }}</el-descriptions-item>
              <el-descriptions-item label="提交时间">{{ formatDateTime(project.submitTime) }}</el-descriptions-item>
              <el-descriptions-item label="最高重复度">
                {{ project.duplicateScore ?? '—' }}
              </el-descriptions-item>
              <el-descriptions-item label="风险等级">{{ project.riskLevel || '—' }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ formatDateTime(project.updatedAt) }}</el-descriptions-item>
            </el-descriptions>
            <div class="text-block" v-if="project.summary">
              <h3>项目摘要</h3>
              <p>{{ project.summary }}</p>
            </div>
            <div class="text-block" v-if="project.expectedOutcome">
              <h3>预期成果</h3>
              <p>{{ project.expectedOutcome }}</p>
            </div>
          </el-tab-pane>

          <el-tab-pane label="申报内容" name="sections">
            <div class="section-list">
              <h3>项目目标</h3>
              <ul><li v-for="(item, index) in project.goals" :key="index">{{ item.content }}</li></ul>
              <el-empty v-if="project.goals.length === 0" description="暂无目标" :image-size="56" />
            </div>
            <div class="section-list">
              <h3>研制内容</h3>
              <ul><li v-for="(item, index) in project.contents" :key="index">{{ item.content }}</li></ul>
              <el-empty v-if="project.contents.length === 0" description="暂无研制内容" :image-size="56" />
            </div>
            <div class="section-list">
              <h3>关键技术</h3>
              <el-table :data="project.keyTechs" size="small" border>
                <el-table-column prop="name" label="技术名称" width="220" />
                <el-table-column prop="description" label="说明" />
                <el-table-column label="难度" width="90">
                  <template #default="{ row }">{{ difficultyLabel(row.difficulty) }}</template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

          <el-tab-pane label="节点与成员" name="plan">
            <div class="section-list">
              <h3>节点计划</h3>
              <el-table :data="project.milestones" size="small" border>
                <el-table-column prop="name" label="阶段" width="200" />
                <el-table-column prop="planStart" label="计划开始" width="120" />
                <el-table-column prop="planEnd" label="计划结束" width="120" />
                <el-table-column prop="deliverable" label="交付物" />
                <el-table-column prop="weight" label="权重" width="80" align="center" />
              </el-table>
            </div>
            <div class="section-list">
              <h3>项目成员</h3>
              <el-table :data="project.members" size="small" border>
                <el-table-column prop="userId" label="用户 ID" width="120" />
                <el-table-column label="角色" width="140">
                  <template #default="{ row }">{{ memberRoleLabel(row.memberRole) }}</template>
                </el-table-column>
                <el-table-column prop="workload" label="投入人月" width="120" />
              </el-table>
            </div>
          </el-tab-pane>

          <el-tab-pane label="附件" name="documents">
            <div class="doc-toolbar">
              <div>
                <h3>申报书附件</h3>
                <p>支持 docx / pdf / txt / md，单文件不超过 20MB，最多 10 个</p>
              </div>
              <el-button v-if="canEdit" type="primary" plain :icon="Upload" @click="pickFile">上传附件</el-button>
              <input ref="fileInput" type="file" accept=".docx,.pdf,.txt,.md" hidden @change="onFileChange" />
            </div>
            <el-table :data="project.documents" size="small" border>
              <el-table-column prop="fileName" label="文件名" min-width="220" />
              <el-table-column prop="fileType" label="类型" width="80" />
              <el-table-column label="大小" width="100">
                <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
              </el-table-column>
              <el-table-column label="解析" width="150">
                <template #default="{ row }">
                  <el-tag size="small" :type="parseTagType(row.parseStatus)">{{ parseLabel(row.parseStatus) }}</el-tag>
                  <span v-if="row.parseMessage" class="faint parse-message">{{ row.parseMessage }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="230">
                <template #default="{ row }">
                  <el-button link type="primary" @click="previewDoc(row)">预览回填</el-button>
                  <el-button link type="primary" @click="downloadDoc(row)">下载</el-button>
                  <el-button v-if="canEdit" link type="warning" @click="reparseDoc(row)">重解析</el-button>
                  <el-button v-if="canEdit" link type="danger" @click="removeDoc(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="审批轨迹" name="logs">
            <el-timeline v-if="project.approvalLogs.length">
              <el-timeline-item
                v-for="log in project.approvalLogs"
                :key="log.id"
                :timestamp="formatDateTime(log.createdAt)"
                placement="top"
              >
                <div class="log-title">
                  <strong>{{ actionLabel(log.action) }}</strong>
                  <span>{{ statusLabel(log.fromStatus || '-') }} → {{ statusLabel(log.toStatus) }}</span>
                </div>
                <p v-if="log.comment" class="log-comment">{{ log.comment }}</p>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无审批记录" :image-size="72" />
          </el-tab-pane>

          <el-tab-pane label="重复度" name="duplicate">
            <div v-loading="analysisLoading" class="duplicate-pane">
              <el-empty
                v-if="!analysisLoading && (!analysis || !analysis.analyzed)"
                description="尚未分析"
                :image-size="88"
              >
                <p class="empty-hint">
                  该项目尚未参与重复度分析。PMO 可在「分析中心 → 分析运行」触发全量或单项目分析。
                </p>
                <el-button type="primary" plain :loading="analysisLoading" @click="loadAnalysis">
                  重新查询
                </el-button>
              </el-empty>

              <template v-else-if="analysis">
                <div class="analysis-head">
                  <div>
                    <h3>重复度快照</h3>
                    <p class="faint">
                      分析日期 {{ analysis.snapshot?.analysisDate || '—' }} · 运行 #{{ analysis.snapshot?.runId ?? '—' }}
                    </p>
                  </div>
                  <el-button text :icon="Refresh" :loading="analysisLoading" @click="loadAnalysis">刷新</el-button>
                </div>

                <div class="kpi-grid">
                  <article class="kpi-card">
                    <span class="kpi-label">最高重复度</span>
                    <strong class="kpi-value">{{ formatScore(analysis.snapshot?.maxScore) }}</strong>
                    <el-tag size="small" :type="levelTagType(analysis.riskLevel)" effect="light">
                      {{ levelLabel(analysis.riskLevel) }}
                    </el-tag>
                  </article>
                  <article class="kpi-card">
                    <span class="kpi-label">Top-K 平均分</span>
                    <strong class="kpi-value">{{ formatScore(analysis.snapshot?.avgTopScore) }}</strong>
                    <span class="kpi-hint">按本项目最相似项目计算</span>
                  </article>
                  <article class="kpi-card">
                    <span class="kpi-label">高风险对数</span>
                    <strong class="kpi-value">{{ analysis.snapshot?.highPairCount ?? 0 }}</strong>
                    <span class="kpi-hint">总分 ≥ {{ analysis.thresholds?.high ?? 80 }}</span>
                  </article>
                  <article class="kpi-card">
                    <span class="kpi-label">风险敞口</span>
                    <strong class="kpi-value">{{ formatMoney(analysis.snapshot?.riskExposure) }}</strong>
                    <span class="kpi-hint">预算 × 最高分 / 100</span>
                  </article>
                </div>

                <div class="attr-line">
                  <span>归因分布：</span>
                  <el-tag size="small" type="info" effect="plain">同组 {{ analysis.snapshot?.selfGroupCount ?? 0 }}</el-tag>
                  <el-tag size="small" type="warning" effect="plain">跨组 {{ analysis.snapshot?.crossGroupCount ?? 0 }}</el-tag>
                  <el-tag size="small" type="danger" effect="plain">我方在先 {{ analysis.snapshot?.ourEarlierCount ?? 0 }}</el-tag>
                  <el-tag size="small" type="primary" effect="plain">他方在先 {{ analysis.snapshot?.peerEarlierCount ?? 0 }}</el-tag>
                  <el-tag size="small" type="success" effect="plain">同期并行 {{ analysis.snapshot?.simultaneousCount ?? 0 }}</el-tag>
                </div>

                <h3 class="block-title">Top-{{ analysis.topPeers.length }} 相似项目</h3>
                <el-table :data="analysis.topPeers" size="small" border>
                  <el-table-column label="相似项目" min-width="220">
                    <template #default="{ row }">
                      <div class="peer-name">{{ row.peer.name }}</div>
                      <div class="faint peer-meta">{{ row.peer.code }} · {{ row.peer.groupName || '未归组' }}</div>
                    </template>
                  </el-table-column>
                  <el-table-column label="总分" width="90" align="right">
                    <template #default="{ row }">
                      <strong>{{ formatScore(row.totalScore) }}</strong>
                    </template>
                  </el-table-column>
                  <el-table-column label="等级" width="90" align="center">
                    <template #default="{ row }">
                      <el-tag size="small" :type="levelTagType(row.level)" effect="light">{{ levelLabel(row.level) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="归因" width="120" align="center">
                    <template #default="{ row }">{{ directionLabel(row.direction) }}</template>
                  </el-table-column>
                  <el-table-column label="目标" width="74" align="right">
                    <template #default="{ row }">{{ formatScore(row.scores.goal) }}</template>
                  </el-table-column>
                  <el-table-column label="内容" width="74" align="right">
                    <template #default="{ row }">{{ formatScore(row.scores.content) }}</template>
                  </el-table-column>
                  <el-table-column label="技术" width="74" align="right">
                    <template #default="{ row }">{{ formatScore(row.scores.tech) }}</template>
                  </el-table-column>
                  <el-table-column label="节点" width="74" align="right">
                    <template #default="{ row }">{{ formatScore(row.scores.milestone) }}</template>
                  </el-table-column>
                  <el-table-column label="标签" width="74" align="right">
                    <template #default="{ row }">{{ formatScore(row.scores.tag) }}</template>
                  </el-table-column>
                </el-table>

                <h3 class="block-title">重复点与差异点</h3>
                <el-empty v-if="analysis.topPeers.length === 0" description="本次运行未产生相似项目" :image-size="72" />
                <el-collapse v-else>
                  <el-collapse-item
                    v-for="peer in analysis.topPeers"
                    :key="peer.peer.id"
                    :name="peer.peer.id"
                  >
                    <template #title>
                      <span class="collapse-title">
                        {{ peer.peer.name }}
                        <el-tag size="small" :type="levelTagType(peer.level)" effect="plain">{{ formatScore(peer.totalScore) }}</el-tag>
                      </span>
                    </template>
                    <div class="point-grid">
                      <div class="point-block">
                        <h4>重复点</h4>
                        <el-empty v-if="peer.matchedPoints.length === 0" description="无匹配片段" :image-size="48" />
                        <div v-for="(point, index) in peer.matchedPoints" :key="`m-${index}`" class="point">
                          <strong>{{ dimensionLabel(point.dimension) }}</strong>
                          <span class="terms">{{ point.terms.join('、') }}</span>
                          <p>{{ point.snippet }}</p>
                        </div>
                      </div>
                      <div class="point-block">
                        <h4>差异点</h4>
                        <el-empty v-if="peer.diffPoints.length === 0" description="无差异片段" :image-size="48" />
                        <div v-for="(point, index) in peer.diffPoints" :key="`d-${index}`" class="point">
                          <strong>{{ dimensionLabel(point.dimension) }}</strong>
                          <span class="terms">{{ point.terms.join('、') }}</span>
                          <p>{{ point.snippet }}</p>
                        </div>
                      </div>
                    </div>
                    <el-alert
                      v-if="peer.llmReason"
                      :title="`大模型补充理由：${peer.llmReason}`"
                      type="info"
                      :closable="false"
                      show-icon
                    />
                  </el-collapse-item>
                </el-collapse>
              </template>
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>
    </template>

    <el-dialog v-model="rejectVisible" title="驳回项目" width="460px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="驳回意见" required>
          <el-input v-model="rejectComment" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="解析预览与回填" width="760px">
      <div v-if="preview">
        <el-alert v-if="preview.message" :title="preview.message" type="warning" :closable="false" show-icon />
        <div class="preview-grid">
          <div class="preview-block">
            <h4>项目目标（{{ preview.goals.length }}）</h4>
            <ul><li v-for="(item, i) in preview.goals" :key="i">{{ item }}</li></ul>
          </div>
          <div class="preview-block">
            <h4>研制内容（{{ preview.contents.length }}）</h4>
            <ul><li v-for="(item, i) in preview.contents" :key="i">{{ item }}</li></ul>
          </div>
          <div class="preview-block">
            <h4>关键技术（{{ preview.keyTechs.length }}）</h4>
            <ul><li v-for="(item, i) in preview.keyTechs" :key="i">{{ item }}</li></ul>
          </div>
          <div class="preview-block">
            <h4>节点计划（{{ preview.milestones.length }}）</h4>
            <ul><li v-for="(item, i) in preview.milestones" :key="i">{{ item }}</li></ul>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button type="primary" :loading="applying" :disabled="!canEdit" @click="applyPreview">
          回填到申报内容
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Edit, Refresh, Upload } from '@element-plus/icons-vue'
import {
  deleteDocument,
  downloadDocument,
  getDocumentPreview,
  getProject,
  parseDocument,
  projectAction,
  saveSections,
  uploadDocument
} from '@/api/project'
import { getProjectAnalysis } from '@/api/analysis'
import { useAuthStore } from '@/stores/auth'
import type {
  DuplicateLevel,
  ProjectAnalysisReport,
  ProjectDetail,
  ProjectDocumentItem,
  SectionPreview
} from '@/types'
import {
  DIFFICULTY_LABELS,
  FUND_TYPE_LABELS,
  MEMBER_ROLE_LABELS,
  formatDateTime,
  formatMoney,
  statusLabel,
  statusTagType
} from '@/utils/project'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const project = ref<ProjectDetail | null>(null)
const loading = ref(false)
const acting = ref(false)
const activeTab = ref('overview')
const rejectVisible = ref(false)
const rejectComment = ref('')
const fileInput = ref<HTMLInputElement>()
const previewVisible = ref(false)
const preview = ref<SectionPreview | null>(null)
const previewDocument = ref<ProjectDocumentItem | null>(null)
const applying = ref(false)
const analysis = ref<ProjectAnalysisReport | null>(null)
const analysisLoading = ref(false)
const analysisLoaded = ref(false)

const projectId = computed(() => Number(route.params.id))
const canManage = computed(() => auth.hasRole('ADMIN', 'PMO', 'GROUP_USER'))
const canApprove = computed(() => auth.hasRole('ADMIN', 'PMO'))
const canEdit = computed(() =>
  canManage.value && project.value !== null && ['DRAFT', 'REJECTED'].includes(project.value.status)
)
const canSubmit = computed(() => canEdit.value)
const canWithdraw = computed(() => canManage.value && project.value?.status === 'SUBMITTED')

onMounted(load)

watch(activeTab, (tab) => {
  if (tab === 'duplicate' && !analysisLoaded.value) {
    loadAnalysis()
  }
})

async function loadAnalysis(): Promise<void> {
  if (analysisLoading.value) {
    return
  }
  analysisLoading.value = true
  try {
    analysis.value = await getProjectAnalysis(projectId.value)
    analysisLoaded.value = true
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '重复度分析加载失败')
  } finally {
    analysisLoading.value = false
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    project.value = await getProject(projectId.value)
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '项目加载失败')
  } finally {
    loading.value = false
  }
}

async function doAction(action: string, label: string): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认${label}项目“${project.value?.name}”吗？`, '确认操作', {
      type: 'warning',
      confirmButtonText: label,
      cancelButtonText: '取消'
    })
    acting.value = true
    await projectAction(projectId.value, action, undefined, project.value?.version)
    ElMessage.success(`${label}成功`)
    await load()
  } catch (exception) {
    if (exception === 'cancel' || exception === 'close') {
      return
    }
    ElMessage.error(exception instanceof Error ? exception.message : `${label}失败`)
  } finally {
    acting.value = false
  }
}

async function confirmReject(): Promise<void> {
  if (!rejectComment.value.trim()) {
    ElMessage.warning('请填写驳回意见')
    return
  }
  try {
    acting.value = true
    await projectAction(projectId.value, 'REJECT', rejectComment.value.trim(), project.value?.version)
    rejectVisible.value = false
    rejectComment.value = ''
    ElMessage.success('已驳回')
    await load()
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '驳回失败')
  } finally {
    acting.value = false
  }
}

function pickFile(): void {
  fileInput.value?.click()
}

async function onFileChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  try {
    loading.value = true
    await uploadDocument(projectId.value, file)
    ElMessage.success('附件已上传并解析')
    await load()
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '上传失败')
  } finally {
    loading.value = false
  }
}

async function previewDoc(document: ProjectDocumentItem): Promise<void> {
  try {
    preview.value = await getDocumentPreview(projectId.value, document.id)
    previewDocument.value = document
    previewVisible.value = true
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '预览失败')
  }
}

async function applyPreview(): Promise<void> {
  if (!preview.value || !project.value) {
    return
  }
  try {
    await ElMessageBox.confirm('回填将替换现有目标、研制内容、关键技术与节点计划，保留成员信息。是否继续？', '回填确认', {
      type: 'warning',
      confirmButtonText: '回填',
      cancelButtonText: '取消'
    })
    applying.value = true
    const current = project.value
    await saveSections(projectId.value, {
      goals: preview.value.goals.map((content, index) => ({ content, sort: index })),
      contents: preview.value.contents.map((content, index) => ({ content, sort: index })),
      keyTechs: preview.value.keyTechs.map((name, index) => ({
        name,
        description: '',
        difficulty: 'MEDIUM',
        sort: index
      })),
      milestones: preview.value.milestones.map((name, index) => ({
        name,
        weight: 0,
        sort: index
      })),
      members: current.members.map((member) => ({
        userId: member.userId,
        memberRole: member.memberRole,
        workload: member.workload
      }))
    })
    previewVisible.value = false
    ElMessage.success('已回填申报内容')
    await load()
    activeTab.value = 'sections'
  } catch (exception) {
    if (exception === 'cancel' || exception === 'close') {
      return
    }
    ElMessage.error(exception instanceof Error ? exception.message : '回填失败')
  } finally {
    applying.value = false
  }
}

async function reparseDoc(document: ProjectDocumentItem): Promise<void> {
  try {
    await parseDocument(document.id)
    ElMessage.success('已重新解析')
    await load()
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '重解析失败')
  }
}

async function removeDoc(document: ProjectDocumentItem): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除附件“${document.fileName}”吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteDocument(document.id)
    ElMessage.success('已删除')
    await load()
  } catch (exception) {
    if (exception === 'cancel' || exception === 'close') {
      return
    }
    ElMessage.error(exception instanceof Error ? exception.message : '删除失败')
  }
}

async function downloadDoc(document: ProjectDocumentItem): Promise<void> {
  try {
    await downloadDocument(document.id, document.fileName)
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '下载失败')
  }
}

function fundLabel(code: string): string {
  return FUND_TYPE_LABELS[code] || code
}

function difficultyLabel(code?: string | null): string {
  return code ? DIFFICULTY_LABELS[code] || code : '—'
}

function memberRoleLabel(code: string): string {
  return MEMBER_ROLE_LABELS[code] || code
}

function actionLabel(action: string): string {
  const labels: Record<string, string> = {
    SUBMIT: '提交申报',
    WITHDRAW: '撤回申报',
    APPROVE: '确认立项',
    REJECT: '驳回申报'
  }
  return labels[action] || action
}

function parseLabel(status: string): string {
  const labels: Record<string, string> = {
    PENDING: '待解析',
    PARSED: '已解析',
    FAILED: '解析失败'
  }
  return labels[status] || status
}

function parseTagType(status: string): 'info' | 'success' | 'danger' {
  if (status === 'PARSED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

function levelLabel(level: DuplicateLevel | string | null | undefined): string {
  const labels: Record<string, string> = {
    HIGH: '高重复',
    MEDIUM: '中重复',
    LOW: '低重复',
    NONE: '无明显重复'
  }
  return level ? labels[level] || level : '—'
}

function levelTagType(
  level: DuplicateLevel | string | null | undefined
): 'danger' | 'warning' | 'info' | 'success' {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  if (level === 'LOW') return 'info'
  return 'success'
}

function directionLabel(direction: string | null | undefined): string {
  const labels: Record<string, string> = {
    SELF_GROUP: '同组内部',
    SIMULTANEOUS: '同期并行',
    OUR_EARLIER: '我方在先',
    PEER_EARLIER: '他方在先'
  }
  return direction ? labels[direction] || direction : '—'
}

function dimensionLabel(dimension: string): string {
  const labels: Record<string, string> = {
    goal: '项目目标',
    content: '研制内容',
    tech: '关键技术',
    milestone: '节点计划',
    tag: '技术标签'
  }
  return labels[dimension] || dimension
}

function formatScore(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '—'
  }
  return value.toFixed(2)
}

function formatSize(size: number): string {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
</script>

<style scoped>
.detail-page {
  padding-bottom: 40px;
}

.title-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-row h1 {
  margin: 0;
}

.project-code {
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 11px;
  color: var(--ink-faint);
}

.subline {
  font-size: 12px;
  color: var(--ink-muted);
}

.heading-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.section-card {
  padding: 18px 22px;
}

.text-block {
  margin-top: 20px;
}

.text-block h3,
.section-list h3,
.doc-toolbar h3 {
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-strong);
}

.text-block p {
  color: var(--ink);
  line-height: 1.9;
  white-space: pre-wrap;
}

.section-list {
  margin-bottom: 24px;
}

.section-list ul {
  margin: 0;
  padding-left: 20px;
  color: var(--ink);
  line-height: 1.9;
}

.doc-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.doc-toolbar p {
  font-size: 12px;
  color: var(--ink-muted);
}

.parse-message {
  display: block;
  font-size: 11px;
  margin-top: 2px;
}

.log-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.log-title span {
  color: var(--ink-muted);
  font-size: 12px;
}

.log-comment {
  margin-top: 6px;
  color: var(--ink);
  font-size: 12px;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 14px;
}

.preview-block {
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 12px;
  background: var(--surface-subtle);
  max-height: 240px;
  overflow: auto;
}

.preview-block h4 {
  font-size: 13px;
  margin-bottom: 6px;
  color: var(--ink-strong);
}

.preview-block ul {
  margin: 0;
  padding-left: 18px;
  font-size: 12px;
  line-height: 1.8;
}

.duplicate-pane {
  min-height: 220px;
}

.empty-hint {
  margin-bottom: 12px;
  font-size: 12px;
  color: var(--ink-muted);
}

.analysis-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.analysis-head h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink-strong);
}

.analysis-head p {
  margin-top: 4px;
  font-size: 12px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.kpi-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface-subtle);
}

.kpi-label {
  font-size: 12px;
  color: var(--ink-muted);
}

.kpi-value {
  font-size: 24px;
  line-height: 1.1;
  font-weight: 600;
  color: var(--ink-strong);
}

.kpi-hint {
  font-size: 11px;
  color: var(--ink-faint);
}

.attr-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
  font-size: 12px;
  color: var(--ink-muted);
}

.block-title {
  margin: 22px 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-strong);
}

.peer-name {
  font-size: 13px;
  color: var(--ink-strong);
}

.peer-meta {
  font-size: 11px;
  margin-top: 2px;
}

.collapse-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.point-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.point-block h4 {
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--ink-strong);
}

.point {
  padding: 10px 12px;
  margin-bottom: 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface-subtle);
  font-size: 12px;
}

.point .terms {
  margin-left: 8px;
  color: var(--brand-primary);
}

.point p {
  margin-top: 6px;
  color: var(--ink);
  line-height: 1.8;
}

@media (max-width: 1180px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .heading-actions {
    width: 100%;
  }

  .preview-grid,
  .point-grid {
    grid-template-columns: 1fr;
  }

  .kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
