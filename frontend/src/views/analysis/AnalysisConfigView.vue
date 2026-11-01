<template>
  <div class="page analysis-config" v-loading="loading">
    <div class="page-heading">
      <div>
        <h1>分析配置</h1>
        <p>五维权重、等级阈值与大模型融合参数；保存后生成新版本，仅影响之后的运行。</p>
      </div>
      <div class="heading-actions">
        <el-tag v-if="currentVersion" effect="plain" class="version-tag">当前版本 v{{ currentVersion }}</el-tag>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <section class="card card-pad">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="config-form"
        @submit.prevent="submit"
      >
        <div class="form-section">
          <header class="section-head">
            <h2>字段权重</h2>
            <p>权重为整数，总和需大于 0；分析时按有效维度权重归一化。</p>
          </header>
          <div class="weight-grid">
            <el-form-item
              v-for="item in weightFields"
              :key="item.key"
              :label="item.label"
              :prop="`fieldWeights.${item.key}`"
            >
              <el-input-number v-model="form.fieldWeights[item.key]" :min="0" :max="100" :step="5" controls-position="right" />
            </el-form-item>
          </div>
          <p class="form-hint">权重合计：{{ weightTotal }}</p>
        </div>

        <div class="form-section">
          <header class="section-head">
            <h2>等级阈值</h2>
            <p>需满足 0 &lt; 低 &lt; 中 &lt; 高 ≤ 100。</p>
          </header>
          <div class="threshold-grid">
            <el-form-item label="高" prop="thresholds.high">
              <el-input-number v-model="form.thresholds.high" :min="1" :max="100" controls-position="right" />
            </el-form-item>
            <el-form-item label="中" prop="thresholds.medium">
              <el-input-number v-model="form.thresholds.medium" :min="1" :max="100" controls-position="right" />
            </el-form-item>
            <el-form-item label="低" prop="thresholds.low">
              <el-input-number v-model="form.thresholds.low" :min="1" :max="100" controls-position="right" />
            </el-form-item>
          </div>
        </div>

        <div class="form-section">
          <header class="section-head">
            <h2>运行参数</h2>
            <p>Top-K 决定每项目保留的相似项目数，同期窗口用于跨组归因。</p>
          </header>
          <div class="param-grid">
            <el-form-item label="Top-K" prop="topK">
              <el-input-number v-model="form.topK" :min="1" :max="20" controls-position="right" />
            </el-form-item>
            <el-form-item label="同期窗口（天）" prop="simultaneousDays">
              <el-input-number v-model="form.simultaneousDays" :min="0" :max="365" controls-position="right" />
            </el-form-item>
          </div>
        </div>

        <div class="form-section">
          <header class="section-head">
            <h2>大模型增强</h2>
            <p>默认关闭；开启后仅对本地分 Top-N 的候选对调用，失败自动降级为本地分。</p>
          </header>
          <el-form-item label="启用大模型">
            <el-switch v-model="form.llmEnabled" active-text="开启" inactive-text="关闭" />
          </el-form-item>
          <div class="llm-test">
            <el-button size="small" :loading="testingLlm" @click="runLlmTest">测试连接</el-button>
            <span v-if="llmTestMessage" class="llm-test-message">{{ llmTestMessage }}</span>
          </div>
          <div class="param-grid">
            <el-form-item label="本地权重" prop="localWeight">
              <el-input-number v-model="form.localWeight" :min="0" :max="100" controls-position="right" />
            </el-form-item>
            <el-form-item label="大模型权重" prop="llmWeight">
              <el-input-number v-model="form.llmWeight" :min="0" :max="100" controls-position="right" />
            </el-form-item>
            <el-form-item label="候选对数" prop="llmTopN">
              <el-input-number v-model="form.llmTopN" :min="1" :max="20" controls-position="right" />
            </el-form-item>
            <el-form-item label="超时（毫秒）" prop="llmTimeoutMs">
              <el-input-number v-model="form.llmTimeoutMs" :min="500" :max="60000" :step="500" controls-position="right" />
            </el-form-item>
          </div>
          <p class="form-hint">本地与大模型权重之和需大于 0；当前合计 {{ form.localWeight + form.llmWeight }}。</p>
        </div>

        <div class="form-actions">
          <el-button @click="load">重置</el-button>
          <el-button type="primary" :loading="saving" native-type="submit">保存配置</el-button>
        </div>
      </el-form>
    </section>

    <section class="card card-pad history-card">
      <header class="section-head">
        <h2>版本历史</h2>
        <p>新配置仅影响之后的运行，历史结果按当时的配置版本复算可追溯。</p>
      </header>
      <el-table :data="history" size="small" border>
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column label="字段权重" min-width="240">
          <template #default="{ row }">{{ weightSummary(row.fieldWeights) }}</template>
        </el-table-column>
        <el-table-column label="阈值" width="150">
          <template #default="{ row }">
            {{ row.thresholds.high }} / {{ row.thresholds.medium }} / {{ row.thresholds.low }}
          </template>
        </el-table-column>
        <el-table-column prop="topK" label="TopK" width="80" />
        <el-table-column prop="simultaneousDays" label="同期(天)" width="90" />
        <el-table-column label="大模型" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="row.llmEnabled ? 'success' : 'info'" effect="light">
              {{ row.llmEnabled ? `开启 ${row.localWeight}/${row.llmWeight}` : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.active" size="small" type="success" effect="plain">生效中</el-tag>
            <span v-else class="faint">历史</span>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  getAnalysisConfig,
  getAnalysisConfigHistory,
  saveAnalysisConfig,
  testLlmConnection
} from '@/api/analysis'
import type { AnalysisConfig, AnalysisConfigPayload } from '@/types'

const weightFields = [
  { key: 'goal', label: '项目目标' },
  { key: 'content', label: '研制内容' },
  { key: 'tech', label: '关键技术' },
  { key: 'milestone', label: '节点计划' },
  { key: 'tag', label: '技术标签' }
] as const

const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const testingLlm = ref(false)
const llmTestMessage = ref('')
const history = ref<AnalysisConfig[]>([])
const currentVersion = ref<number | null>(null)

const form = reactive<AnalysisConfigPayload>({
  fieldWeights: { goal: 15, content: 35, tech: 35, milestone: 5, tag: 10 },
  thresholds: { high: 80, medium: 60, low: 40 },
  topK: 5,
  simultaneousDays: 30,
  llmEnabled: false,
  localWeight: 70,
  llmWeight: 30,
  llmTopN: 5,
  llmTimeoutMs: 8000
})

const weightTotal = computed(() =>
  Object.values(form.fieldWeights).reduce((total, value) => total + (Number(value) || 0), 0)
)

const rules: FormRules = {
  'fieldWeights.goal': [{ type: 'number', min: 0, message: '权重需 ≥ 0', trigger: 'blur' }],
  'fieldWeights.content': [{ type: 'number', min: 0, message: '权重需 ≥ 0', trigger: 'blur' }],
  'fieldWeights.tech': [{ type: 'number', min: 0, message: '权重需 ≥ 0', trigger: 'blur' }],
  'fieldWeights.milestone': [{ type: 'number', min: 0, message: '权重需 ≥ 0', trigger: 'blur' }],
  'fieldWeights.tag': [{ type: 'number', min: 0, message: '权重需 ≥ 0', trigger: 'blur' }],
  'thresholds.high': [{ type: 'number', min: 1, max: 100, message: '需在 1-100 之间', trigger: 'blur' }],
  'thresholds.medium': [{ type: 'number', min: 1, max: 100, message: '需在 1-100 之间', trigger: 'blur' }],
  'thresholds.low': [{ type: 'number', min: 1, max: 100, message: '需在 1-100 之间', trigger: 'blur' }],
  topK: [{ type: 'number', min: 1, max: 20, message: '需在 1-20 之间', trigger: 'blur' }],
  simultaneousDays: [{ type: 'number', min: 0, max: 365, message: '需在 0-365 之间', trigger: 'blur' }],
  localWeight: [{ type: 'number', min: 0, max: 100, message: '需在 0-100 之间', trigger: 'blur' }],
  llmWeight: [{ type: 'number', min: 0, max: 100, message: '需在 0-100 之间', trigger: 'blur' }],
  llmTopN: [{ type: 'number', min: 1, max: 20, message: '需在 1-20 之间', trigger: 'blur' }],
  llmTimeoutMs: [{ type: 'number', min: 500, max: 60000, message: '需在 500-60000 之间', trigger: 'blur' }]
}

onMounted(load)

async function load(): Promise<void> {
  loading.value = true
  try {
    const [config, versions] = await Promise.all([
      getAnalysisConfig(),
      getAnalysisConfigHistory().catch(() => [])
    ])
    applyConfig(config)
    history.value = versions
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '分析配置加载失败')
  } finally {
    loading.value = false
  }
}

function applyConfig(config: AnalysisConfig): void {
  currentVersion.value = config.version
  form.fieldWeights = {
    goal: config.fieldWeights.goal ?? 15,
    content: config.fieldWeights.content ?? 35,
    tech: config.fieldWeights.tech ?? 35,
    milestone: config.fieldWeights.milestone ?? 5,
    tag: config.fieldWeights.tag ?? 10
  }
  form.thresholds = {
    high: config.thresholds.high ?? 80,
    medium: config.thresholds.medium ?? 60,
    low: config.thresholds.low ?? 40
  }
  form.topK = config.topK
  form.simultaneousDays = config.simultaneousDays
  form.llmEnabled = config.llmEnabled
  form.localWeight = config.localWeight
  form.llmWeight = config.llmWeight
  form.llmTopN = config.llmTopN
  form.llmTimeoutMs = config.llmTimeoutMs
}

function validateCrossField(): boolean {
  if (weightTotal.value <= 0) {
    ElMessage.warning('字段权重之和必须大于 0')
    return false
  }
  const { low, medium, high } = form.thresholds
  if (!(low > 0 && low < medium && medium < high && high <= 100)) {
    ElMessage.warning('阈值必须满足 0 < 低 < 中 < 高 ≤ 100')
    return false
  }
  if (form.localWeight + form.llmWeight <= 0) {
    ElMessage.warning('本地与大模型权重之和必须大于 0')
    return false
  }
  return true
}

async function submit(): Promise<void> {
  if (!formRef.value || saving.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid || !validateCrossField()) {
    return
  }
  saving.value = true
  try {
    const saved = await saveAnalysisConfig({
      fieldWeights: { ...form.fieldWeights },
      thresholds: { ...form.thresholds },
      topK: form.topK,
      simultaneousDays: form.simultaneousDays,
      llmEnabled: form.llmEnabled,
      localWeight: form.localWeight,
      llmWeight: form.llmWeight,
      llmTopN: form.llmTopN,
      llmTimeoutMs: form.llmTimeoutMs
    })
    ElMessage.success(`配置已保存，新版本 v${saved.version}`)
    applyConfig(saved)
    await load()
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function runLlmTest(): Promise<void> {
  testingLlm.value = true
  llmTestMessage.value = ''
  try {
    const result = await testLlmConnection()
    llmTestMessage.value = result.success
      ? `连接成功（${result.model}，${result.latencyMs}ms）`
      : `连接失败：${result.message}`
    if (result.success) {
      ElMessage.success(llmTestMessage.value)
    } else {
      ElMessage.error(llmTestMessage.value)
    }
  } catch (exception) {
    llmTestMessage.value = exception instanceof Error ? exception.message : '连接测试失败'
    ElMessage.error(llmTestMessage.value)
  } finally {
    testingLlm.value = false
  }
}

function weightSummary(weights: Record<string, number>): string {
  return weightFields
    .map((field) => `${field.label} ${weights[field.key] ?? 0}`)
    .join(' · ')
}
</script>

<style scoped>
.heading-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.version-tag {
  border-color: var(--border-strong);
  color: var(--ink-muted);
}

.form-section {
  padding-bottom: 18px;
  margin-bottom: 18px;
  border-bottom: 1px dashed var(--border);
}

.form-section:last-of-type {
  border-bottom: 0;
}

.section-head {
  margin-bottom: 14px;
}

.section-head h2 {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink-strong);
}

.section-head p {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ink-muted);
}

.weight-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.threshold-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.param-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.form-hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ink-faint);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}

.history-card {
  margin-top: 16px;
}

@media (max-width: 1180px) {
  .weight-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .param-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .weight-grid,
  .threshold-grid,
  .param-grid {
    grid-template-columns: 1fr;
  }
}
</style>
