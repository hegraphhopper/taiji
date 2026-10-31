<template>
  <div class="login-page">
    <section class="brand-panel">
      <div class="brand-top">
        <span class="brand-mark">TJ</span>
        <span class="brand-name">电科太极 · 自投项目管理</span>
      </div>

      <div class="brand-copy">
        <h1>把自投项目的<br />重复账算清楚</h1>
        <p>结构化申报、重复度分析、趋势归因，让每一笔研发投入都有据可查。</p>
      </div>

      <ul class="brand-points">
        <li><span>01</span>申报书结构化与附件解析</li>
        <li><span>02</span>多维重复度分析与可解释片段</li>
        <li><span>03</span>重复度趋势与四类归因</li>
      </ul>

      <div class="brand-foot">A 阶段 · 核心主线</div>
    </section>

    <section class="form-panel">
      <div class="form-card">
        <div class="form-heading">
          <h2>登录</h2>
          <p>使用所内账号进入自投项目治理平台</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="submit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              autocomplete="username"
              :prefix-icon="User"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              autocomplete="current-password"
              :prefix-icon="Lock"
            />
          </el-form-item>

          <el-alert
            v-if="errorMessage"
            :title="errorMessage"
            type="error"
            show-icon
            :closable="false"
            class="login-alert"
          />

          <el-button
            type="primary"
            size="large"
            class="submit-button"
            :loading="submitting"
            native-type="submit"
          >
            {{ submitting ? '正在登录' : '进入平台' }}
          </el-button>
        </el-form>

        <div class="demo-box">
          <div class="demo-title">演示账号</div>
          <div class="demo-row"><span>管理员</span><code>admin / Admin@123</code></div>
          <div class="demo-row"><span>基金办</span><code>pmo01 / Pmo@12345</code></div>
          <div class="demo-row"><span>项目组</span><code>user01 / User@12345</code></div>
          <p class="demo-tip">首次进入后请修改默认密码。</p>
        </div>
      </div>

      <footer class="form-foot">自投研发项目重复投入治理平台 · v1.0</footer>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useDictStore } from '@/stores/dict'
import { safeRedirect } from '@/utils/redirect'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const dict = useDictStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const errorMessage = ref('')
const form = reactive({
  username: 'admin',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit(): Promise<void> {
  if (submitting.value || !formRef.value) {
    return
  }
  submitting.value = true
  errorMessage.value = ''
  try {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) {
      return
    }
    await auth.login(form.username.trim(), form.password)
    try {
      await dict.load(true)
    } catch {
      // 字典失败不影响登录，进入工作台后可重试
    }
    ElMessage.success(`欢迎回来，${auth.displayName}`)
    await router.replace(safeRedirect(route.query.redirect))
  } catch (exception) {
    errorMessage.value = exception instanceof Error ? exception.message : '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(420px, 0.9fr);
  background: var(--surface-page);
}

.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 44px 56px;
  color: #e6edf8;
  background:
    radial-gradient(circle at 20% 20%, rgba(31, 94, 255, 0.42), transparent 45%),
    radial-gradient(circle at 80% 70%, rgba(0, 194, 184, 0.28), transparent 45%),
    linear-gradient(160deg, #0b1730 0%, #0f1b33 55%, #0a1427 100%);
  overflow: hidden;
}

.brand-panel::after {
  content: "";
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.7), transparent 85%);
  pointer-events: none;
}

.brand-top {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--brand-primary), var(--brand-accent));
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.brand-name {
  font-size: 13px;
  letter-spacing: 0.4px;
  color: #b9c7dd;
}

.brand-copy {
  position: relative;
  z-index: 1;
  margin-top: auto;
  margin-bottom: auto;
  max-width: 560px;
}

.brand-copy h1 {
  font-size: 44px;
  line-height: 1.22;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: #fff;
}

.brand-copy p {
  margin-top: 20px;
  font-size: 15px;
  line-height: 1.8;
  color: #9fb0cc;
  max-width: 440px;
}

.brand-points {
  position: relative;
  z-index: 1;
  list-style: none;
  padding: 0;
  margin: 0 0 48px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  font-size: 13px;
  color: #c7d2e5;
}

.brand-points li {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-points span {
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 11px;
  color: var(--brand-accent);
  border: 1px solid rgba(0, 194, 184, 0.35);
  border-radius: 999px;
  padding: 2px 8px;
}

.brand-foot {
  position: relative;
  z-index: 1;
  font-size: 11px;
  color: #6f82a3;
  letter-spacing: 0.5px;
}

.form-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #fff;
}

.form-card {
  width: 100%;
  max-width: 380px;
}

.form-heading h2 {
  font-size: 26px;
  font-weight: 600;
  color: var(--ink-strong);
  letter-spacing: 0.3px;
}

.form-heading p {
  margin-top: 8px;
  font-size: 13px;
  color: var(--ink-muted);
}

.el-form {
  margin-top: 28px;
}

.el-form :deep(.el-form-item__label) {
  font-size: 13px;
  color: var(--ink);
  padding-bottom: 6px;
}

.login-alert {
  margin-bottom: 16px;
}

.submit-button {
  width: 100%;
  height: 44px;
  font-size: 14px;
  letter-spacing: 0.5px;
}

.demo-box {
  margin-top: 28px;
  padding: 16px 18px;
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-md);
  background: var(--surface-subtle);
}

.demo-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--ink-muted);
  letter-spacing: 0.5px;
  margin-bottom: 10px;
}

.demo-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
  color: var(--ink-muted);
  padding: 3px 0;
}

.demo-row code {
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 11px;
  color: var(--ink);
}

.demo-tip {
  margin-top: 8px;
  font-size: 11px;
  color: var(--ink-faint);
}

.form-foot {
  margin-top: 36px;
  font-size: 11px;
  color: var(--ink-faint);
}

@media (max-width: 980px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    display: none;
  }

  .form-panel {
    min-height: 100vh;
  }
}
</style>
