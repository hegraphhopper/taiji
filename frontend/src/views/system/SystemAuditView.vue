<template>
  <div class="page audit-page">
    <div class="page-heading">
      <div>
        <h1>审计日志</h1>
        <p>只读记录登录、用户与字典变更、项目流转、分析、决策与导出</p>
      </div>
      <div class="heading-actions">
        <el-button :loading="exporting" @click="exportCsv">导出 CSV</el-button>
      </div>
    </div>

    <section class="card filter-bar">
      <el-input v-model="filters.username" clearable placeholder="操作人" class="w-150" />
      <el-input v-model="filters.action" clearable placeholder="动作，如 PROJECT_APPROVE" class="w-220" />
      <el-input v-model="filters.resourceType" clearable placeholder="资源类型" class="w-160" />
      <el-select v-model="filters.result" clearable placeholder="结果" class="w-120">
        <el-option label="成功" value="SUCCESS" />
        <el-option label="失败" value="FAIL" />
      </el-select>
      <el-date-picker
        v-model="range"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        class="w-260"
      />
      <el-button type="primary" plain :loading="loading" @click="search">查询</el-button>
      <el-button text @click="reset">重置</el-button>
    </section>

    <section class="card table-card">
      <el-table :data="logs" size="small" border @row-click="openDetail">
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="action" label="动作" width="200" />
        <el-table-column prop="resourceType" label="资源" width="150" />
        <el-table-column prop="resourceId" label="资源ID" width="100" />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.result === 'SUCCESS' ? 'success' : 'danger'">
              {{ row.result }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column label="耗时" width="90" align="right">
          <template #default="{ row }">{{ row.costMs ?? '—' }} ms</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="load"
        />
      </div>
    </section>

    <el-drawer v-model="drawerVisible" title="审计详情" size="560px">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="时间">{{ detail.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ detail.username || '—' }}</el-descriptions-item>
          <el-descriptions-item label="动作">{{ detail.action }}</el-descriptions-item>
          <el-descriptions-item label="资源">{{ detail.resourceType }} / {{ detail.resourceId }}</el-descriptions-item>
          <el-descriptions-item label="结果">{{ detail.result }}</el-descriptions-item>
          <el-descriptions-item label="IP">{{ detail.ip || '—' }}</el-descriptions-item>
        </el-descriptions>
        <h4 class="detail-title">请求摘要</h4>
        <pre class="detail-json">{{ detail.detail || '—' }}</pre>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { downloadAuditLogs, getAuditLogs } from '@/api/governance'
import type { AuditLogItem } from '@/types'

const filters = reactive({
  username: '',
  action: '',
  resourceType: '',
  result: ''
})
const range = ref<[string, string] | null>(null)
const logs = ref<AuditLogItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const exporting = ref(false)
const drawerVisible = ref(false)
const detail = ref<AuditLogItem | null>(null)

function params() {
  return {
    username: filters.username || null,
    action: filters.action || null,
    resourceType: filters.resourceType || null,
    result: filters.result || null,
    from: range.value?.[0] || null,
    to: range.value?.[1] || null
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const data = await getAuditLogs({ ...params(), page: page.value, size: size.value })
    logs.value = data.records
    total.value = data.total
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '审计日志加载失败')
  } finally {
    loading.value = false
  }
}

function search(): void {
  page.value = 1
  void load()
}

function reset(): void {
  filters.username = ''
  filters.action = ''
  filters.resourceType = ''
  filters.result = ''
  range.value = null
  search()
}

async function exportCsv(): Promise<void> {
  exporting.value = true
  try {
    await downloadAuditLogs({ ...params(), page: 1, size: 10 })
    ElMessage.success('导出已开始')
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '导出失败')
  } finally {
    exporting.value = false
  }
}

function openDetail(row: AuditLogItem): void {
  detail.value = row
  drawerVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  padding: 14px 16px;
  margin-bottom: 16px;
}

.w-150 { width: 150px; }
.w-220 { width: 220px; }
.w-160 { width: 160px; }
.w-120 { width: 120px; }
.w-260 { width: 260px; }

.table-card {
  padding: 10px 12px 16px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

.detail-title {
  margin: 18px 0 8px;
  font-size: 13px;
  color: var(--ink-strong);
}

.detail-json {
  margin: 0;
  padding: 12px;
  border-radius: 8px;
  background: var(--surface-page);
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--ink-muted);
}
</style>
