<template>
  <div class="page org-page">
    <div class="page-heading">
      <div>
        <h1>组织与字典</h1>
        <p>维护项目组与技术方向；被用户或项目引用时不可删除</p>
      </div>
      <div class="heading-actions">
        <el-button type="primary" @click="openCreate">
          {{ tab === 'groups' ? '新增项目组' : '新增技术方向' }}
        </el-button>
      </div>
    </div>

    <section class="card table-card">
      <el-tabs v-model="tab" @tab-change="load">
        <el-tab-pane label="项目组" name="groups">
          <el-table :data="groups" size="small" border>
            <el-table-column prop="name" label="名称" min-width="180" />
            <el-table-column prop="code" label="编码" width="190" />
            <el-table-column prop="leader" label="负责人" width="120" />
            <el-table-column prop="sort" label="排序" width="80" align="right" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.enabled === 1 ? 'success' : 'info'">
                  {{ row.enabled === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="remove(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="技术方向" name="directions">
          <el-table :data="directions" size="small" border>
            <el-table-column prop="name" label="名称" min-width="180" />
            <el-table-column prop="code" label="编码" width="190" />
            <el-table-column label="关键词" min-width="220">
              <template #default="{ row }">{{ (row.keywords || []).join('、') }}</template>
            </el-table-column>
            <el-table-column prop="sort" label="排序" width="80" align="right" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.enabled === 1 ? 'success' : 'info'">
                  {{ row.enabled === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="remove(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="form.code" maxlength="50" />
        </el-form-item>
        <el-form-item v-if="tab === 'groups'" label="负责人">
          <el-input v-model="form.leader" maxlength="50" />
        </el-form-item>
        <el-form-item v-if="tab === 'directions'" label="关键词（逗号分隔）">
          <el-input v-model="form.keywords" placeholder="如：机器学习,深度学习" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :step="1" class="full" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.enabled">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createGroup,
  createTechDirection,
  deleteGroup,
  deleteTechDirection,
  getAllGroups,
  getAllTechDirections,
  updateGroup,
  updateTechDirection
} from '@/api/governance'
import type { GroupItem, TechDirection } from '@/types'

type Row = GroupItem | TechDirection

const tab = ref<'groups' | 'directions'>('groups')
const groups = ref<GroupItem[]>([])
const directions = ref<TechDirection[]>([])
const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  name: '',
  code: '',
  leader: '',
  keywords: '',
  sort: 0,
  enabled: 1
})

const dialogTitle = computed(() => {
  const type = tab.value === 'groups' ? '项目组' : '技术方向'
  return editingId.value ? `编辑${type}` : `新增${type}`
})

async function load(): Promise<void> {
  try {
    if (tab.value === 'groups') {
      groups.value = await getAllGroups()
    } else {
      directions.value = await getAllTechDirections()
    }
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '加载失败')
  }
}

function openCreate(): void {
  editingId.value = null
  Object.assign(form, { name: '', code: '', leader: '', keywords: '', sort: 0, enabled: 1 })
  dialogVisible.value = true
}

function openEdit(row: Row): void {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    code: row.code,
    leader: 'leader' in row ? (row.leader || '') : '',
    keywords: 'keywords' in row ? (row.keywords || []).join(',') : '',
    sort: row.sort || 0,
    enabled: row.enabled ?? 1
  })
  dialogVisible.value = true
}

async function submit(): Promise<void> {
  if (!form.name.trim() || !form.code.trim()) {
    ElMessage.warning('名称与编码不能为空')
    return
  }
  submitting.value = true
  try {
    if (tab.value === 'groups') {
      const payload = {
        name: form.name.trim(),
        code: form.code.trim(),
        leader: form.leader || null,
        sort: form.sort,
        enabled: form.enabled
      }
      if (editingId.value) {
        await updateGroup(editingId.value, payload)
      } else {
        await createGroup(payload)
      }
    } else {
      const payload = {
        name: form.name.trim(),
        code: form.code.trim(),
        keywords: form.keywords.split(/[,，]/).map((item) => item.trim()).filter(Boolean),
        sort: form.sort,
        enabled: form.enabled
      }
      if (editingId.value) {
        await updateTechDirection(editingId.value, payload)
      } else {
        await createTechDirection(payload)
      }
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '保存失败')
  } finally {
    submitting.value = false
  }
}

async function remove(row: Row): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    if (tab.value === 'groups') {
      await deleteGroup(row.id)
    } else {
      await deleteTechDirection(row.id)
    }
    ElMessage.success('已删除')
    await load()
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.table-card {
  padding: 6px 12px 16px;
}

.full {
  width: 100%;
}
</style>
