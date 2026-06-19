/**
 * 路由配置
 * @module router
 * @description 定义应用路由表、路由守卫（登录态校验、页面切换动画）
 */

import { createRouter, createWebHistory, RouteRecordRaw, RouteLocationNormalized } from 'vue-router'
import { getToken } from '@/utils/token'

/**
 * 路由表
 * @description 包含所有页面路由，meta 字段说明：
 *   - requiresAuth: 是否需要登录（默认 true）
 *   - transition: 页面切换动画名称
 *   - title: 页面标题
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginPage.vue'),
    meta: { requiresAuth: false, transition: 'fade-slide', title: '登录' }
  },
  {
    path: '/interest-select',
    name: 'InterestSelect',
    component: () => import('@/views/interest/InterestSelectPage.vue'),
    meta: { requiresAuth: true, transition: 'fade-slide', title: '选择兴趣' }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/home/HomePage.vue'),
    meta: { requiresAuth: false, transition: 'fade-slide', title: '首页' }
  },
  {
    path: '/post/:id',
    name: 'PostDetail',
    component: () => import('@/views/post/PostDetailPage.vue'),
    meta: { requiresAuth: true, transition: 'fade-slide', title: '帖子详情' }
  },
  {
    path: '/post/create',
    name: 'PostCreate',
    component: () => import('@/views/post/CreatePostPage.vue'),
    meta: { requiresAuth: true, transition: 'scale-in', title: '发布帖子' }
  },
  {
    path: '/circle',
    name: 'Circle',
    component: () => import('@/views/circle/CirclePlazaPage.vue'),
    meta: { requiresAuth: true, transition: 'fade-slide', title: '圈子广场' }
  },
  {
    path: '/circle/:id',
    name: 'CircleDetail',
    component: () => import('@/views/circle/CircleDetailPage.vue'),
    meta: { requiresAuth: true, transition: 'fade-slide', title: '圈子详情' }
  },
  {
    path: '/messages',
    name: 'Messages',
    component: () => import('@/views/message/MessageCenterPage.vue'),
    meta: { requiresAuth: true, transition: 'fade-slide', title: '消息通知' }
  },
  {
    path: '/chat',
    redirect: '/messages'
  },
  {
    path: '/chat/:userId',
    name: 'Chat',
    component: () => import('@/views/message/ChatPage.vue'),
    meta: { requiresAuth: true, transition: 'slide-in-right', title: '私信' }
  },
  {
    path: '/profile/:id',
    name: 'UserProfile',
    component: () => import('@/views/profile/ProfilePage.vue'),
    meta: { requiresAuth: true, transition: 'fade-slide', title: '个人主页' }
  },
  {
    path: '/profile',
    name: 'MyProfile',
    component: () => import('@/views/profile/ProfilePage.vue'),
    meta: { requiresAuth: true, transition: 'fade-slide', title: '我的' },
    beforeEnter: (to) => {
      // 从 store 获取当前用户ID，跳转到带 ID 的个人主页
      const userId = JSON.parse(localStorage.getItem('user') || '{}').id
      if (userId) return `/profile/${userId}`
      // 未登录则放行，由 requiresAuth 守卫处理
    }
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/search/SearchPage.vue'),
    meta: { requiresAuth: true, transition: 'fade-slide', title: '搜索' }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/settings/SettingsPage.vue'),
    meta: { requiresAuth: true, transition: 'fade-slide', title: '设置' }
  },
  {
    path: '/edit-profile',
    name: 'EditProfile',
    component: () => import('@/views/profile/EditProfilePage.vue'),
    meta: { requiresAuth: true, transition: 'fade-slide', title: '编辑资料' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFoundPage.vue'),
    meta: { requiresAuth: false, transition: 'fade-slide', title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    return savedPosition || { top: 0 }
  }
})

/**
 * 路由前置守卫
 * @description 校验登录态：
 *   - 未登录访问需鉴权页面 → 跳转 /login，携带 redirect 参数
 *   - 已登录访问 /login → 跳转首页
 * @param {RouteLocationNormalized} to - 目标路由
 * @returns {boolean | string} 放行或重定向路径
 */
router.beforeEach((to) => {
  const token = getToken()
  const requiresAuth = to.meta.requiresAuth !== false

  // 已登录访问登录页 → 跳转首页
  if (token && to.path === '/login') {
    return '/'
  }

  // 未登录访问需鉴权页面 → 跳转登录页
  if (!token && requiresAuth) {
    const redirect = encodeURIComponent(to.fullPath)
    return `/login?redirect=${redirect}`
  }

  return true
})

export default router
