<template>
  <div class="page todo-page">
    <div class="page-heading">
      <div>
        <h1>待办中心</h1>
        <p>按角色汇总需要处理的申报、审查与评审任务</p>
      </div>
      <div class="heading-actions">
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <section class="todo-stats">
      <article v-for="(value, key) in todo?.counts || {}" :key="key" class="card todo-stat">
        <span>{{ actionLabel(String(key)) }}</span>
        <strong>{{ value }}</strong>
      </article>
      <article v-if="!todo || todo.total === 0" class="card todo-stat empty">
        <span>当前没有待办</span>
        <strong>0</strong>
      </article>
    </section>

    <section class="card table-card">
      <el-table :data="todo?.items || []" size="small" border @row-click="openProject">
        <el-table-column prop="code" label="项目编码" width="140" />
        <el-table-column prop="name" label="项目名称" min-width="220" />
        <el-table-column prop="groupName" label="项目组" width="150" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">{{ statusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="actionLabel" label="待办动作" width="150" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openProject(row)">去处理</el-button>
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
import { getTodo } from '@/api/governance'
import type { TodoItem, TodoSummary } from '@/types'
import { statusLabel } from '@/utils/project'

const router = useRouter()
const todo = ref<TodoSummary | null>(null)
const loading = ref(false)

const actionMap: Record<string, string> = {
  ACCEPT: '待形式审查',
  APPROVE: '待立项/驳回',
  REVIEW: '待专家评审',
  RESUBMIT: '待修改重提',
  EDIT: '待完善草稿'
}

function actionLabel(action: string): string {
  return actionMap[action] || action
}

async function load(): Promise<void> {
  loading.value = true
  try {
    todo.value = await getTodo()
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '待办加载失败')
  } finally {
    loading.value = false
  }
}

function openProject(row: TodoItem): void {
  void router.push(`/projects/${row.projectId}`)
}

onMounted(load)
</script>

<style scoped>
.todo-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.todo-stat {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
}

.todo-stat span {
  color: var(--ink-muted);
  font-size: 12px;
}

.todo-stat strong {
  color: var(--ink-strong);
  font-size: 24px;
}

.todo-stat.empty strong {
  color: var(--ink-faint);
}

.table-card {
  padding: 10px 12px;
}
</style>
