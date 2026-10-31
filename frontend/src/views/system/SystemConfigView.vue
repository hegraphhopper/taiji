<template>
  <div class="page config-page">
    <div class="page-heading">
      <div>
        <h1>系统参数</h1>
        <p>评审通过人数等运行参数，修改后立即生效并写入审计</p>
      </div>
      <div class="heading-actions">
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <section class="card table-card">
      <el-table :data="configs" size="small" border>
        <el-table-column prop="key" label="参数键" width="240" />
        <el-table-column prop="description" label="说明" min-width="240" />
        <el-table-column label="值" width="180">
          <template #default="{ row }">
            <el-input-number
              v-if="row.key === 'review.requiredPassCount'"
              v-model="values[row.key]"
              :min="1"
              :max="10"
              :step="1"
              controls-position="right"
              size="small"
            />
            <el-input v-else v-model="values[row.key]" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="170" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" :loading="saving === row.key" @click="save(row)">
              保存
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemConfigs, updateSystemConfig } from '@/api/governance'
import type { SystemConfigItem } from '@/types'

const configs = ref<SystemConfigItem[]>([])
const values = reactive<Record<string, number | string>>({})
const loading = ref(false)
const saving = ref<string | null>(null)

async function load(): Promise<void> {
  loading.value = true
  try {
    configs.value = await getSystemConfigs()
    for (const item of configs.value) {
      values[item.key] = item.key === 'review.requiredPassCount'
        ? Number(item.value) : item.value
    }
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '参数加载失败')
  } finally {
    loading.value = false
  }
}

async function save(row: SystemConfigItem): Promise<void> {
  saving.value = row.key
  try {
    const value = String(values[row.key] ?? '').trim()
    const saved = await updateSystemConfig(row.key, value, row.description)
    ElMessage.success('参数已保存：' + saved.key + ' = ' + saved.value)
    await load()
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '保存失败')
  } finally {
    saving.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.table-card {
  padding: 10px 12px;
}
</style>
