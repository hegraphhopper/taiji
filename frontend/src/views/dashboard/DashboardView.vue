<template>
  <div class="page dashboard">
    <div class="page-heading">
      <div>
        <h1>工作台</h1>
        <p>{{ today }} · 欢迎回来，{{ auth.displayName }}</p>
      </div>
      <div class="heading-actions">
        <el-tag effect="plain" class="env-tag">A1 基础版</el-tag>
        <el-button type="primary" plain :icon="Folder" disabled>新建项目（A2）</el-button>
      </div>
    </div>

    <section class="kpi-grid">
      <article v-for="item in kpis" :key="item.label" class="card kpi-card">
        <div class="kpi-top">
          <span class="kpi-label">{{ item.label }}</span>
          <span class="kpi-phase">{{ item.phase }}</span>
        </div>
        <div class="kpi-value">{{ item.value }}</div>
        <div class="kpi-hint">{{ item.hint }}</div>
      </article>
    </section>

    <section class="content-grid">
      <article class="card card-pad roadmap-card">
        <header class="card-head">
          <div>
            <h2>A 阶段进度</h2>
            <p>核心主线：申报书 → 重复度 → 趋势归因</p>
          </div>
          <el-tag size="small" type="success" effect="light">1 / 4 完成</el-tag>
        </header>
        <ol class="roadmap">
          <li v-for="stage in roadmap" :key="stage.code" :class="stage.state">
            <span class="stage-dot"></span>
            <div class="stage-body">
              <div class="stage-title">
                <strong>{{ stage.code }} {{ stage.title }}</strong>
                <el-tag size="small" :type="stage.tagType" effect="plain">{{ stage.badge }}</el-tag>
              </div>
              <p>{{ stage.description }}</p>
            </div>
          </li>
        </ol>
      </article>

      <article class="card card-pad base-card">
        <header class="card-head">
          <div>
            <h2>基础数据</h2>
            <p>来自 V2 种子与字典接口</p>
          </div>
          <el-button text :icon="Refresh" :loading="dict.loading" @click="reloadDict">刷新</el-button>
        </header>
        <div class="base-grid">
          <div v-for="item in baseItems" :key="item.label" class="base-item">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
        <el-empty v-if="dict.error" :description="dict.error" :image-size="72" />
      </article>
    </section>

    <section class="content-grid lower">
      <article class="card card-pad account-card">
        <header class="card-head">
          <div>
            <h2>当前账号</h2>
            <p>角色与数据范围将在 C 阶段细化</p>
          </div>
        </header>
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item label="用户名">{{ auth.user?.username || '—' }}</el-descriptions-item>
          <el-descriptions-item label="真实姓名">{{ auth.user?.realName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ auth.roleLabel }}</el-descriptions-item>
          <el-descriptions-item label="项目组">{{ auth.user?.groupName || '未归属' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ auth.user?.status || '—' }}</el-descriptions-item>
          <el-descriptions-item label="最近登录">{{ auth.user?.lastLoginAt || '—' }}</el-descriptions-item>
        </el-descriptions>
      </article>

      <article class="card card-pad next-card">
        <header class="card-head">
          <div>
            <h2>下一步</h2>
            <p>按 A 阶段顺序推进</p>
          </div>
        </header>
        <ul class="next-list">
          <li>
            <span class="next-code">A2</span>
            <div>
              <strong>项目与申报书</strong>
              <p>结构化目标、研制内容、关键技术、节点计划与附件解析。</p>
            </div>
          </li>
          <li>
            <span class="next-code">A3</span>
            <div>
              <strong>重复度分析</strong>
              <p>五维相似度、权重配置、本地与大模型混合评分。</p>
            </div>
          </li>
          <li>
            <span class="next-code">A4</span>
            <div>
              <strong>趋势与归因</strong>
              <p>月度趋势、四类归因、热力图与项目对下钻。</p>
            </div>
          </li>
        </ul>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import dayjs from 'dayjs'
import { Folder, Refresh } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useDictStore } from '@/stores/dict'

const auth = useAuthStore()
const dict = useDictStore()

const today = dayjs().format('YYYY 年 MM 月 DD 日')

const kpis = computed(() => [
  {
    label: '项目总数',
    value: '—',
    phase: 'A2',
    hint: '项目管理接入后展示'
  },
  {
    label: '平均最高重复度',
    value: '—',
    phase: 'A3',
    hint: '分析引擎运行后计算'
  },
  {
    label: '高风险项目',
    value: '—',
    phase: 'A3',
    hint: '重复度达到高阈值'
  },
  {
    label: '技术方向',
    value: String(dict.techDirections.length),
    phase: '就绪',
    hint: '用于项目打标与相似度'
  }
])

const baseItems = computed(() => [
  { label: '项目组', value: dict.dicts ? '8' : '—' },
  { label: '技术方向', value: String(dict.techDirections.length) },
  { label: '角色', value: String(dict.roles.length) },
  { label: '基金类型', value: String(dict.fundTypes.length) }
])

type RoadmapState = 'done' | 'active' | 'pending'

const roadmap: Array<{
  code: string
  title: string
  description: string
  state: RoadmapState
  badge: string
  tagType: 'success' | 'primary' | 'info'
}> = [
  {
    code: 'A1',
    title: '平台底座',
    description: '工程骨架、Flyway 种子、JWT 登录、审计写入与前端壳。',
    state: 'done',
    badge: '已完成',
    tagType: 'success'
  },
  {
    code: 'A2',
    title: '项目与申报书',
    description: '结构化申报内容、附件解析、草稿到提交的最小闭环。',
    state: 'pending',
    badge: '待开始',
    tagType: 'info'
  },
  {
    code: 'A3',
    title: '重复度分析',
    description: '五维相似度、权重阈值配置、混合评分与项目快照。',
    state: 'pending',
    badge: '待开始',
    tagType: 'info'
  },
  {
    code: 'A4',
    title: '趋势与归因',
    description: '月度趋势、四类归因、热力图、下钻与导出。',
    state: 'pending',
    badge: '待开始',
    tagType: 'info'
  }
]

function reloadDict(): void {
  dict.load(true).catch(() => undefined)
}
</script>

<style scoped>
.heading-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.env-tag {
  border-color: var(--border-strong);
  color: var(--ink-muted);
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.kpi-card {
  padding: 18px 20px 16px;
  position: relative;
  overflow: hidden;
}

.kpi-card::after {
  content: "";
  position: absolute;
  right: -24px;
  top: -24px;
  width: 86px;
  height: 86px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(31, 94, 255, 0.12), transparent 70%);
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kpi-label {
  font-size: 12px;
  color: var(--ink-muted);
}

.kpi-phase {
  font-size: 10px;
  color: var(--ink-faint);
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 1px 7px;
}

.kpi-value {
  margin-top: 10px;
  font-size: 28px;
  line-height: 1.1;
  font-weight: 600;
  color: var(--ink-strong);
  letter-spacing: 0.5px;
}

.kpi-hint {
  margin-top: 8px;
  font-size: 11px;
  color: var(--ink-faint);
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
  gap: 16px;
  margin-top: 16px;
}

.content-grid.lower {
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.card-head h2 {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink-strong);
}

.card-head p {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ink-muted);
}

.roadmap {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.roadmap li {
  display: flex;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px dashed var(--border);
}

.roadmap li:last-child {
  border-bottom: 0;
}

.stage-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  margin-top: 7px;
  flex: 0 0 auto;
  background: var(--border-strong);
}

.roadmap li.done .stage-dot {
  background: var(--brand-accent);
  box-shadow: 0 0 0 4px rgba(0, 194, 184, 0.12);
}

.roadmap li.active .stage-dot {
  background: var(--brand-primary);
  box-shadow: 0 0 0 4px rgba(31, 94, 255, 0.12);
}

.stage-body {
  flex: 1;
  min-width: 0;
}

.stage-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stage-title strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-strong);
}

.stage-body p {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ink-muted);
}

.base-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.base-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: var(--radius-sm);
  background: var(--surface-subtle);
  border: 1px solid var(--border);
}

.base-item span {
  font-size: 12px;
  color: var(--ink-muted);
}

.base-item strong {
  font-size: 22px;
  font-weight: 600;
  color: var(--ink-strong);
}

.next-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.next-list li {
  display: flex;
  gap: 12px;
}

.next-code {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 24px;
  border-radius: 6px;
  background: var(--brand-primary-soft);
  color: var(--brand-primary);
  font-size: 11px;
  font-weight: 600;
  flex: 0 0 auto;
}

.next-list strong {
  font-size: 13px;
  color: var(--ink-strong);
}

.next-list p {
  margin-top: 3px;
  font-size: 12px;
  color: var(--ink-muted);
}

@media (max-width: 1180px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid,
  .content-grid.lower {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }

  .page {
    padding: 18px 16px 32px;
  }
}
</style>
