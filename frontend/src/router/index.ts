import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useDictStore } from '@/stores/dict'
import { safeRedirect } from '@/utils/redirect'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    title?: string
    roles?: string[]
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'projects',
        name: 'projects',
        component: () => import('@/views/project/ProjectListView.vue'),
        meta: { title: '项目管理' }
      },
      {
        path: 'projects/new',
        name: 'project-create',
        component: () => import('@/views/project/ProjectFormView.vue'),
        meta: { title: '新建项目' }
      },
      {
        path: 'projects/:id',
        name: 'project-detail',
        component: () => import('@/views/project/ProjectDetailView.vue'),
        meta: { title: '项目详情' }
      },
      {
        path: 'projects/:id/edit',
        name: 'project-edit',
        component: () => import('@/views/project/ProjectFormView.vue'),
        meta: { title: '编辑项目' }
      },
      {
        path: 'trend',
        name: 'trend',
        component: () => import('@/views/trend/TrendView.vue'),
        meta: { title: '趋势与归因', roles: ['PMO', 'ADMIN', 'GROUP_USER'] }
      },
      {
        path: 'analysis/runs',
        name: 'analysis-runs',
        component: () => import('@/views/analysis/AnalysisRunsView.vue'),
        meta: { title: '分析运行', roles: ['PMO', 'ADMIN'] }
      },
      {
        path: 'analysis/config',
        name: 'analysis-config',
        component: () => import('@/views/analysis/AnalysisConfigView.vue'),
        meta: { title: '分析配置', roles: ['ADMIN'] }
      }
    ]
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/error/ForbiddenView.vue'),
    meta: { public: true, title: '无权限' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: { public: true, title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    if (to.name === 'login' && auth.isLoggedIn) {
      if (!auth.user) {
        try {
          await auth.fetchMe()
        } catch {
          // 无效或过期 token：清理本地会话后停留在登录页
          auth.clearSession()
          return true
        }
        try {
          await useDictStore().load()
        } catch {
          // 字典加载失败不影响登录态判定，进入页面后可重试
        }
      }
      if (auth.user) {
        return { path: safeRedirect(to.query.redirect) }
      }
      return true
    }
    return true
  }
  if (!auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (!auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      auth.clearSession()
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    try {
      await useDictStore().load()
    } catch {
      // 字典加载失败不阻断页面进入，页面内可重试
    }
  }
  if (to.meta.roles?.length && !auth.hasRole(...to.meta.roles)) {
    return { name: 'forbidden' }
  }
  return true
})

router.afterEach((to) => {
  const base = '自投研发项目重复投入治理平台'
  document.title = to.meta.title ? `${to.meta.title} - ${base}` : base
})

export default router
