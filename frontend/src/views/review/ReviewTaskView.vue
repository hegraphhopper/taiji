<template>
  <div class="page review-page">
    <div class="page-heading">
      <div>
        <h1>评审任务</h1>
        <p>处于评审中的项目可提交评分与结论，每轮每位专家一次</p>
      </div>
      <div class="heading-actions">
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <section class="card table-card">
      <el-table :data="projects" size="small" border>
        <el-table-column prop="code" label="项目编码" width="150" />
        <el-table-column prop="name" label="项目名称" min-width="240" />
        <el-table-column prop="groupName" label="项目组" width="150" />
        <el-table-column label="预算（万元）" width="130" align="right">
          <template #default="{ row }">{{ (Number(row.budget) / 10000).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="最高重复度" width="110" align="right">
          <template #default="{ row }">{{ row.duplicateScore ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReview(row)">提交评审</el-button>
            <el-button link type="primary" @click="openProject(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && projects.length === 0" description="当前没有待评审项目" :image-size="88" />
    </section>

    <el-dialog v-model="dialogVisible" title="提交专家评审" width="540px" :close-on-click-modal="false">
      <p class="dialog-project">{{ current?.name }}（{{ current?.code }}）</p>
      <el-form label-position="top">
        <el-form-item label="评分（0-100）" required>
          <el-input-number v-model="form.score" :min="0" :max="100" :step="1" class="full" />
        </el-form-item>
        <el-form-item label="评审意见" required>
          <el-input v-model="form.opinion" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="结论" required>
          <el-select v-model="form.result" class="full">
            <el-option label="PASS 通过" value="PASS" />
            <el-option label="REVISE 修改后重审" value="REVISE" />
            <el-option label="REJECT 不通过" value="REJECT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">提交评审</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProjects } from '@/api/project'
import { submitProjectReview } from '@/api/governance'
import type { ProjectListItem } from '@/types'

const router = useRouter()
const projects = ref<ProjectListItem[]>([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const current = ref<ProjectListItem | null>(null)
const form = ref({ score: 85, opinion: '', result: 'PASS' })

async function load(): Promise<void> {
  loading.value = true
  try {
    const page = await getProjects({ status: ['REVIEWING'], page: 1, size: 100 })
    projects.value = page.records
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '评审任务加载失败')
  } finally {
    loading.value = false
  }
}

function openReview(row: ProjectListItem): void {
  current.value = row
  form.value = { score: 85, opinion: '', result: 'PASS' }
  dialogVisible.value = true
}

function openProject(row: ProjectListItem): void {
  void router.push(`/projects/${row.id}`)
}

async function submit(): Promise<void> {
  if (!current.value) {
    return
  }
  if (!form.value.opinion.trim()) {
    ElMessage.warning('请填写评审意见')
    return
  }
  submitting.value = true
  try {
    await submitProjectReview(current.value.id, {
      score: form.value.score,
      opinion: form.value.opinion.trim(),
      result: form.value.result
    })
    ElMessage.success('评审已提交')
    dialogVisible.value = false
    await load()
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '评审提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.table-card {
  padding: 10px 12px;
}

.dialog-project {
  margin: 0 0 12px;
  color: var(--ink-muted);
  font-size: 13px;
}

.full {
  width: 100%;
}
</style>
