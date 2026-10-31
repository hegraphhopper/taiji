<template>
  <div class="page roles-page">
    <div class="page-heading">
      <div>
        <h1>角色与权限点</h1>
        <p>四类固定角色；权限目录与前端菜单、后端接口矩阵共用同一口径</p>
      </div>
      <div class="heading-actions">
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <section class="role-grid">
      <article v-for="role in roles" :key="role.code" class="card role-card">
        <header>
          <h3>{{ role.name }}</h3>
          <span class="role-code">{{ role.code }}</span>
        </header>
        <p class="role-desc">{{ role.description || '—' }}</p>
        <div class="perm-list">
          <el-tag v-for="permission in role.permissions" :key="permission.code" size="small" effect="plain">
            {{ permission.label }}
          </el-tag>
        </div>
        <p class="role-count">共 {{ role.permissions.length }} 个权限点</p>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getRoles } from '@/api/governance'
import type { RolePermission } from '@/types'

const roles = ref<RolePermission[]>([])
const loading = ref(false)

async function load(): Promise<void> {
  loading.value = true
  try {
    roles.value = await getRoles()
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '角色加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.role-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
}

.role-card {
  padding: 18px 20px;
}

.role-card header {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.role-card h3 {
  margin: 0;
  font-size: 16px;
  color: var(--ink-strong);
}

.role-code {
  font-size: 12px;
  color: var(--ink-faint);
}

.role-desc {
  margin: 8px 0 12px;
  font-size: 12px;
  color: var(--ink-muted);
  min-height: 32px;
}

.perm-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.role-count {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--ink-faint);
}
</style>
