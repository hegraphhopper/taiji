<template>
  <div class="error-page">
    <div class="error-code">403</div>
    <h1>没有访问权限</h1>
    <p>当前账号的角色不允许访问该页面。如需开通，请联系系统管理员。</p>
    <div class="error-actions">
      <el-button type="primary" @click="router.replace('/dashboard')">返回工作台</el-button>
      <el-button @click="router.back()">返回上一页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.error-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  text-align: center;
  background: var(--surface-page);
}

.error-code {
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 88px;
  font-weight: 700;
  line-height: 1;
  color: var(--brand-primary);
  opacity: 0.16;
  letter-spacing: 4px;
}

h1 {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 600;
  color: var(--ink-strong);
}

p {
  margin-top: 10px;
  max-width: 420px;
  font-size: 13px;
  color: var(--ink-muted);
  line-height: 1.8;
}

.error-actions {
  margin-top: 24px;
  display: flex;
  gap: 10px;
}
</style>
