<template>
  <div class="page llm-logs">
    <div class="page-heading">
      <div>
        <h1>模型调用日志</h1>
        <p>查看大模型调用的状态、耗时与 Token 消耗；不记录 Prompt、API Key 或项目正文。</p>
      </div>
      <div class="heading-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="stats-grid">
      <div class="card stat-card">
        <span>总调用</span>
        <strong>{{ data.total }}</strong>
      </div>
      <div class="card stat-card">
        <span>成功</span>
        <strong>{{ data.successCount }}</strong>
      </div>
      <div class="card stat-card">
        <span>失败</span>
        <strong class="danger">{{ data.failureCount }}</strong>
      </div>
      <div class="card stat-card">
        <span>平均耗时</span>
        <strong>{{ data.avgLatencyMs }} ms</strong>
      </div>
      <div class="card stat-card">
        <span>Prompt Tokens</span>
        <strong>{{ data.promptTokens }}</strong>
      </div>
      <div class="card stat-card">
        <span>Completion Tokens</span>
        <strong>{{ data.completionTokens }}</strong>
      </div>
    </div>

    <section class="card card-pad">
      <div class="filters">
        <el-input v-model="runId" placeholder="运行 ID" clearable class="filter-run" />
        <el-select v-model="status" placeholder="状态" clearable class="filter-status">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="超时" value="TIMEOUT" />
          <el-option label="HTTP 错误" value="HTTP_ERROR" />
          <el-option label="响应非法" value="INVALID" />
          <el-option label="未启用" value="DISABLED" />
        </el-select>
        <el-input v-model="model" placeholder="模型名" clearable class="filter-model" />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="data.records" size="small" border v-loading="loading">
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="runId" label="运行 ID" width="90" align="right" />
        <el-table-column label="项目对" min-width="120">
          <template #default="{ row }">
            <span v-if="row.projectId || row.peerProjectId">
              #{{ row.projectId ?? '-' }} ↔ #{{ row.peerProjectId ?? '-' }}
            </span>
            <span v-else class="muted">连接测试</span>
          </template>
        </el-table-column>
        <el-table-column prop="model" label="模型" min-width="140" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)" effect="plain">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="latencyMs" label="耗时(ms)" width="100" align="right" />
        <el-table-column label="Tokens" width="120" align="right">
          <template #default="{ row }">
            {{ row.promptTokens ?? 0 }} / {{ row.completionTokens ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="error" label="错误" min-width="180" show-overflow-tooltip />
      </el-table>

      <el-pagination
        class="pager"
        background
        layout="total, sizes, prev, pager, next"
        :total="data.total"
        :page-size="size"
        :current-page="page"
        :page-sizes="[10, 20, 50]"
        @current-change="changePage"
        @size-change="changeSize"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getLlmLogs } from '@/api/analysis'
import type { LlmLogPage } from '@/types'

const loading = ref(false)
const page = ref(1)
const size = ref(20)
const runId = ref<number | null>(null)
const status = ref('')
const model = ref('')
const data = reactive<LlmLogPage>({
  records: [],
  total: 0,
  page: 1,
  size: 20,
  successCount: 0,
  failureCount: 0,
  avgLatencyMs: 0,
  promptTokens: 0,
  completionTokens: 0
})

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await getLlmLogs({
      page: page.value,
      size: size.value,
      runId: runId.value,
      status: status.value,
      model: model.value
    })
    Object.assign(data, result)
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function search(): void {
  page.value = 1
  load()
}

function reset(): void {
  runId.value = null
  status.value = ''
  model.value = ''
  page.value = 1
  load()
}

function changePage(value: number): void {
  page.value = value
  load()
}

function changeSize(value: number): void {
  size.value = value
  page.value = 1
  load()
}

function formatTime(value: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function statusType(value: string): 'success' | 'warning' | 'info' | 'danger' {
  if (value === 'SUCCESS') return 'success'
  if (value === 'DISABLED') return 'info'
  if (value === 'TIMEOUT') return 'warning'
  return 'danger'
}

function statusLabel(value: string): string {
  const map: Record<string, string> = {
    SUCCESS: '成功',
    TIMEOUT: '超时',
    HTTP_ERROR: 'HTTP 错误',
    INVALID: '响应非法',
    DISABLED: '未启用'
  }
  return map[value] || value
}

onMounted(load)
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-card span {
  color: var(--ink-muted);
  font-size: 12px;
}

.stat-card strong {
  font-size: 20px;
}

.stat-card .danger {
  color: var(--danger, #d9534f);
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.filter-run {
  width: 120px;
}

.filter-status {
  width: 140px;
}

.filter-model {
  width: 180px;
}

.muted {
  color: var(--ink-muted);
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
