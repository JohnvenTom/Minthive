import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import { getToken } from '@/api/request'

/**
 * 路由配置
 * 功能：定义管理后台路由表与全局前置守卫
 * 参数：无
 * 返回值：Router 实例
 * 注意事项：除登录页外所有路由需登录态，未登录跳转 /login
 */

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/AdminLoginPage.vue'),
    meta: { title: '管理员登录', public: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DataScreenPage.vue'),
        meta: { title: '数据大屏', icon: 'DataAnalysis' }
      },
      {
        path: 'user',
        name: 'UserManage',
        component: () => import('@/views/user/UserManagePage.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'content',
        name: 'ContentAudit',
        component: () => import('@/views/content/ContentAuditPage.vue'),
        meta: { title: '内容审核', icon: 'Document' }
      },
      {
        path: 'circle',
        name: 'CircleManage',
        component: () => import('@/views/circle/CircleManagePage.vue'),
        meta: { title: '圈子管理', icon: 'Connection' }
      },
      {
        path: 'report',
        name: 'ReportWorkOrder',
        component: () => import('@/views/report/ReportWorkOrderPage.vue'),
        meta: { title: '举报工单', icon: 'Warning' }
      },
      {
        path: 'config',
        name: 'SystemConfig',
        component: () => import('@/views/config/SystemConfigPage.vue'),
        meta: { title: '系统配置', icon: 'Setting' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 全局前置守卫
 * 功能：校验登录态，设置页面标题
 */
router.beforeEach((to, _from, next) => {
  document.title = to.meta.title ? `${to.meta.title} · MintHive Admin` : 'MintHive Admin'
  const isPublic = to.meta.public === true
  const token = getToken()
  if (isPublic) {
    // 已登录访问登录页则跳转首页
    if (to.path === '/login' && token) {
      next('/dashboard')
      return
    }
    next()
    return
  }
  if (!token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  // 拉取管理员信息（仅首次）
  const adminStore = useAdminStore()
  if (!adminStore.adminInfo) {
    adminStore.fetchAdminInfo().catch(() => {
      // 拉取失败不阻塞，由请求拦截器处理 401
    })
  }
  next()
})

export default router
