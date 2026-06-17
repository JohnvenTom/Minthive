import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

// 全局样式
import './styles/global.scss'
import './styles/element-dark.scss'

/**
 * 应用入口
 * 功能：创建 Vue 应用，注册 Pinia、Router、Element Plus 及全部图标
 * 注意事项：图标全局注册，组件按需自动导入由 vite 插件处理
 */
const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { size: 'default' })

// 注册全部 Element Plus 图标为全局组件
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
