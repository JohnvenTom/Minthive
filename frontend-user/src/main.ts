/**
 * 应用入口
 * @module main
 * @description 创建 Vue 应用、注册全局插件与样式，配置路由进度条
 */

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import Vant from 'vant'
import 'vant/lib/index.css'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

import App from './App.vue'
import router from './router'
import './styles/global.scss'
import './styles/animations.scss'

const app = createApp(App)

/**
 * Pinia 状态管理初始化
 * @description 创建 Pinia 实例并注册持久化插件，使状态可自动同步至 localStorage
 */
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.use(Vant)

/**
 * 路由前置守卫 - 启动进度条
 * @returns {void}
 */
router.beforeEach(() => {
  NProgress.start()
})

/**
 * 路由后置守卫 - 完成进度条
 * @returns {void}
 */
router.afterEach(() => {
  NProgress.done()
})

app.mount('#app')
