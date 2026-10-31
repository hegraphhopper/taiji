<template>
  <div class="page form-page" v-loading="loading">
    <div class="page-heading">
      <div>
        <h1>{{ isEdit ? '编辑项目' : '新建项目' }}</h1>
        <p>填写项目基本信息与结构化申报内容，保存草稿后可继续完善</p>
      </div>
      <div class="heading-actions">
        <el-button @click="goBack">返回列表</el-button>
      </div>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <section class="card section-card">
        <header class="section-head">
          <span class="section-index">01</span>
          <div>
            <h2>基本信息</h2>
            <p>项目编码留空时自动生成 TJJH-年份-序号</p>
          </div>
        </header>
        <div class="form-grid">
          <el-form-item label="项目名称" prop="name" class="span-2">
            <el-input v-model="form.name" maxlength="200" show-word-limit placeholder="请输入项目名称" />
          </el-form-item>
          <el-form-item label="项目编码">
            <el-input v-model="form.code" maxlength="50" placeholder="留空自动生成" />
          </el-form-item>
          <el-form-item label="基金类型" prop="fundType">
            <el-select v-model="form.fundType" class="full">
              <el-option v-for="item in dict.fundTypes" :key="item.code" :label="item.name" :value="item.code" />
            </el-select>
          </el-form-item>
          <el-form-item label="项目组" prop="groupId">
            <el-select v-model="form.groupId" class="full" :disabled="!isAdminLike">
              <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="负责人">
            <el-input :model-value="auth.displayName" disabled />
          </el-form-item>
          <el-form-item label="技术方向">
            <el-select v-model="form.techDirectionId" class="full" clearable filterable placeholder="请选择">
              <el-option v-for="item in dict.techDirections" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="预算（元）" prop="budget">
            <el-input-number v-model="form.budget" :min="0.01" :precision="2" :step="10000" class="full" />
          </el-form-item>
          <el-form-item label="开始日期">
            <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" class="full" />
          </el-form-item>
          <el-form-item label="结束日期">
            <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" class="full" />
          </el-form-item>
          <el-form-item label="摘要" class="span-2">
            <el-input v-model="form.summary" type="textarea" :rows="3" maxlength="2000" show-word-limit />
          </el-form-item>
          <el-form-item label="预期成果" class="span-2">
            <el-input v-model="form.expectedOutcome" type="textarea" :rows="3" maxlength="2000" show-word-limit />
          </el-form-item>
        </div>
      </section>

      <section class="card section-card">
        <header class="section-head">
          <span class="section-index">02</span>
          <div>
            <h2>申报内容</h2>
            <p>至少各填写一条，提交时校验</p>
          </div>
        </header>
        <div class="two-column">
          <div class="list-editor">
            <div class="list-head">
              <strong>项目目标</strong>
              <el-button text type="primary" :icon="Plus" @click="goals.push({ content: '' })">添加</el-button>
            </div>
            <div v-for="(item, index) in goals" :key="index" class="list-row">
              <el-input v-model="item.content" type="textarea" :rows="2" maxlength="2000" placeholder="目标描述" />
              <el-button text type="danger" :icon="Delete" @click="goals.splice(index, 1)" />
            </div>
            <el-empty v-if="goals.length === 0" description="暂未填写目标" :image-size="56" />
          </div>
          <div class="list-editor">
            <div class="list-head">
              <strong>研制内容</strong>
              <el-button text type="primary" :icon="Plus" @click="contents.push({ content: '' })">添加</el-button>
            </div>
            <div v-for="(item, index) in contents" :key="index" class="list-row">
              <el-input v-model="item.content" type="textarea" :rows="2" maxlength="4000" placeholder="研制内容" />
              <el-button text type="danger" :icon="Delete" @click="contents.splice(index, 1)" />
            </div>
            <el-empty v-if="contents.length === 0" description="暂未填写研制内容" :image-size="56" />
          </div>
        </div>
      </section>

      <section class="card section-card">
        <header class="section-head">
          <span class="section-index">03</span>
          <div>
            <h2>关键技术</h2>
            <p>技术名称与说明将参与重复度分析的语义匹配</p>
          </div>
          <el-button text type="primary" :icon="Plus" @click="addKeyTech">添加关键技术</el-button>
        </header>
        <div v-for="(item, index) in keyTechs" :key="index" class="keytech-row">
          <el-input v-model="item.name" placeholder="技术名称" maxlength="200" />
          <el-input v-model="item.description" placeholder="技术说明" maxlength="2000" />
          <el-select v-model="item.difficulty" class="w-110" placeholder="难度">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
          <el-button text type="danger" :icon="Delete" @click="keyTechs.splice(index, 1)" />
        </div>
        <el-empty v-if="keyTechs.length === 0" description="暂未填写关键技术" :image-size="56" />
      </section>

      <section class="card section-card">
        <header class="section-head">
          <span class="section-index">04</span>
          <div>
            <h2>节点计划</h2>
            <p>至少填写一个节点，权重合计建议为 100</p>
          </div>
          <el-button text type="primary" :icon="Plus" @click="addMilestone">添加节点</el-button>
        </header>
        <div v-for="(item, index) in milestones" :key="index" class="milestone-row">
          <el-input v-model="item.name" placeholder="阶段名称" maxlength="200" />
          <el-date-picker v-model="item.planStart" type="date" value-format="YYYY-MM-DD" placeholder="开始" />
          <el-date-picker v-model="item.planEnd" type="date" value-format="YYYY-MM-DD" placeholder="结束" />
          <el-input v-model="item.deliverable" placeholder="交付物" maxlength="2000" />
          <el-input-number v-model="item.weight" :min="0" :max="100" :controls="false" class="w-80" />
          <el-button text type="danger" :icon="Delete" @click="milestones.splice(index, 1)" />
        </div>
        <el-empty v-if="milestones.length === 0" description="暂未填写节点计划" :image-size="56" />
      </section>

      <section class="card section-card">
        <header class="section-head">
          <span class="section-index">05</span>
          <div>
            <h2>项目成员</h2>
            <p>暂以用户 ID 录入，治理阶段接入人员选择器</p>
          </div>
          <el-button text type="primary" :icon="Plus" @click="addMember">添加成员</el-button>
        </header>
        <div v-for="(item, index) in members" :key="index" class="member-row">
          <el-input-number v-model="item.userId" :min="1" :controls="false" placeholder="用户 ID" />
          <el-select v-model="item.memberRole" class="w-140">
            <el-option label="负责人" value="LEADER" />
            <el-option label="核心成员" value="CORE" />
            <el-option label="参与人" value="PARTICIPANT" />
          </el-select>
          <el-input-number v-model="item.workload" :min="0.01" :precision="2" :controls="false" placeholder="人月" />
          <el-button text type="danger" :icon="Delete" @click="members.splice(index, 1)" />
        </div>
        <el-empty v-if="members.length === 0" description="暂未填写项目成员" :image-size="56" />
      </section>
    </el-form>

    <div class="form-actions">
      <el-button @click="goBack">取消</el-button>
      <el-button :loading="saving" @click="save(false)">保存草稿</el-button>
      <el-button type="primary" :loading="saving" @click="save(true)">保存并提交</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import {
  createProject,
  getProject,
  projectAction,
  saveSections,
  updateProject
} from '@/api/project'
import { getGroupsApi } from '@/api/system'
import { useAuthStore } from '@/stores/auth'
import { useDictStore } from '@/stores/dict'
import type {
  GroupItem,
  ProjectDetail,
  ProjectKeyTechItem,
  ProjectMemberItem,
  ProjectMilestoneItem,
  ProjectSavePayload,
  SectionSavePayload
} from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const dict = useDictStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const dirty = ref(false)
const groups = ref<GroupItem[]>([])
const detail = ref<ProjectDetail | null>(null)
const isEdit = computed(() => Boolean(route.params.id))
const isAdminLike = computed(() => auth.hasRole('ADMIN', 'PMO'))

const form = reactive({
  name: '',
  code: '',
  fundType: 'PLATFORM',
  groupId: null as number | null,
  ownerId: auth.user?.id ?? 1,
  techDirectionId: null as number | null,
  budget: 100000,
  startDate: '' as string | null,
  endDate: '' as string | null,
  summary: '',
  expectedOutcome: ''
})

const goals = ref<Array<{ content: string }>>([{ content: '' }])
const contents = ref<Array<{ content: string }>>([{ content: '' }])
const keyTechs = ref<ProjectKeyTechItem[]>([])
const milestones = ref<ProjectMilestoneItem[]>([])
const members = ref<ProjectMemberItem[]>([])

const rules: FormRules = {
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  fundType: [{ required: true, message: '请选择基金类型', trigger: 'change' }],
  groupId: [{ required: true, message: '请选择项目组', trigger: 'change' }],
  budget: [{ required: true, message: '请输入预算', trigger: 'blur' }]
}

watch(
  [form, goals, contents, keyTechs, milestones, members],
  () => {
    if (!loading.value) {
      dirty.value = true
    }
  },
  { deep: true, flush: 'sync' }
)

onBeforeRouteLeave(async () => {
  if (!dirty.value) {
    return true
  }
  try {
    await ElMessageBox.confirm('当前修改尚未保存，确定离开吗？', '离开确认', {
      type: 'warning',
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑'
    })
    return true
  } catch {
    return false
  }
})

onMounted(async () => {
  loading.value = true
  try {
    await dict.load()
    groups.value = await getGroupsApi()
    if (!isAdminLike.value && auth.user?.groupId) {
      form.groupId = auth.user.groupId
    } else if (groups.value.length) {
      form.groupId = groups.value[0].id
    }
    if (isEdit.value) {
      const project = await getProject(Number(route.params.id))
      applyDetail(project)
    } else {
      addMilestone()
      addMember()
      addKeyTech()
    }
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '项目加载失败')
  } finally {
    loading.value = false
  }
})

function applyDetail(project: ProjectDetail): void {
  detail.value = project
  form.name = project.name
  form.code = project.code
  form.fundType = project.fundType
  form.groupId = project.groupId
  form.ownerId = project.ownerId
  form.techDirectionId = project.techDirectionId
  form.budget = Number(project.budget)
  form.startDate = project.startDate
  form.endDate = project.endDate
  form.summary = project.summary || ''
  form.expectedOutcome = project.expectedOutcome || ''
  goals.value = project.goals.length
    ? project.goals.map((item) => ({ content: item.content }))
    : [{ content: '' }]
  contents.value = project.contents.length
    ? project.contents.map((item) => ({ content: item.content }))
    : [{ content: '' }]
  keyTechs.value = project.keyTechs.map((item) => ({
    name: item.name,
    description: item.description || '',
    difficulty: item.difficulty || 'MEDIUM',
    sort: item.sort
  }))
  milestones.value = project.milestones.map((item) => ({ ...item }))
  members.value = project.members.map((item) => ({ ...item }))
}

function addKeyTech(): void {
  keyTechs.value.push({ name: '', description: '', difficulty: 'MEDIUM', sort: keyTechs.value.length })
}

function addMilestone(): void {
  milestones.value.push({
    name: '',
    planStart: '',
    planEnd: '',
    deliverable: '',
    weight: 0,
    sort: milestones.value.length
  })
}

function addMember(): void {
  members.value.push({
    userId: auth.user?.id ?? 1,
    memberRole: 'CORE',
    workload: 1
  })
}

function buildSectionPayload(): SectionSavePayload {
  return {
    goals: goals.value
      .filter((item) => item.content.trim())
      .map((item, index) => ({ content: item.content.trim(), sort: index })),
    contents: contents.value
      .filter((item) => item.content.trim())
      .map((item, index) => ({ content: item.content.trim(), sort: index })),
    keyTechs: keyTechs.value
      .filter((item) => item.name.trim())
      .map((item, index) => ({
        name: item.name.trim(),
        description: item.description || '',
        difficulty: item.difficulty || 'MEDIUM',
        sort: index
      })),
    milestones: milestones.value
      .filter((item) => item.name.trim())
      .map((item, index) => ({
        name: item.name.trim(),
        planStart: item.planStart || undefined,
        planEnd: item.planEnd || undefined,
        deliverable: item.deliverable || undefined,
        weight: item.weight ?? 0,
        sort: index
      })),
    members: members.value
      .filter((item) => item.userId && item.workload > 0)
      .map((item) => ({
        userId: item.userId,
        memberRole: item.memberRole,
        workload: item.workload
      }))
  }
}

async function save(submitAfter: boolean): Promise<void> {
  if (saving.value || !formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (!form.groupId) {
    ElMessage.warning('请选择项目组')
    return
  }
  saving.value = true
  try {
    const payload: ProjectSavePayload = {
      name: form.name.trim(),
      code: form.code.trim() || undefined,
      fundType: form.fundType,
      groupId: form.groupId,
      ownerId: form.ownerId,
      techDirectionId: form.techDirectionId,
      budget: form.budget,
      startDate: form.startDate || null,
      endDate: form.endDate || null,
      summary: form.summary.trim() || null,
      expectedOutcome: form.expectedOutcome.trim() || null,
      version: detail.value?.version
    }
    const saved = isEdit.value
      ? await updateProject(Number(route.params.id), payload)
      : await createProject(payload)
    await saveSections(saved.id, buildSectionPayload())
    if (submitAfter) {
      await projectAction(saved.id, 'SUBMIT', undefined, saved.version)
    }
    ElMessage.success(submitAfter ? '已保存并提交' : '草稿已保存')
    dirty.value = false
    await router.replace(`/projects/${saved.id}`)
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function goBack(): void {
  router.push('/projects')
}
</script>

<style scoped>
.form-page {
  padding-bottom: 96px;
}

.heading-actions {
  display: flex;
  gap: 10px;
}

.section-card {
  padding: 22px 24px;
  margin-bottom: 16px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 20px;
}

.section-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: var(--brand-primary-soft);
  color: var(--brand-primary);
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 11px;
  font-weight: 600;
  flex: 0 0 auto;
}

.section-head h2 {
  font-size: 16px;
  font-weight: 600;
  color: var(--ink-strong);
}

.section-head p {
  margin-top: 3px;
  font-size: 12px;
  color: var(--ink-muted);
}

.section-head > .el-button:last-child {
  margin-left: auto;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 18px;
}

.span-2 {
  grid-column: span 2;
}

.full,
.form-grid :deep(.el-select),
.form-grid :deep(.el-date-editor) {
  width: 100%;
}

.two-column {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.list-editor {
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 14px;
  background: var(--surface-subtle);
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.list-head strong {
  font-size: 13px;
  color: var(--ink-strong);
}

.list-row {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-bottom: 8px;
}

.keytech-row,
.milestone-row,
.member-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.keytech-row :deep(.el-input:nth-child(2)),
.milestone-row :deep(.el-input:nth-child(4)) {
  flex: 2;
}

.milestone-row :deep(.el-input:first-child) {
  flex: 1.2;
}

.w-110 {
  width: 110px;
  flex: 0 0 auto;
}

.w-140 {
  width: 140px;
  flex: 0 0 auto;
}

.w-80 {
  width: 80px;
  flex: 0 0 auto;
}

.member-row :deep(.el-input-number) {
  width: 120px;
}

.form-actions {
  position: fixed;
  left: 232px;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 28px;
  background: rgba(255, 255, 255, 0.94);
  border-top: 1px solid var(--border);
  backdrop-filter: blur(8px);
  z-index: 10;
}

@media (max-width: 1100px) {
  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .span-2 {
    grid-column: span 2;
  }

  .two-column {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .form-actions {
    left: 72px;
  }
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: span 1;
  }

  .keytech-row,
  .milestone-row,
  .member-row {
    flex-wrap: wrap;
  }
}
</style>
