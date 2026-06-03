import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { setupGuard } from './router/guard'
import { perm } from './directives/perm'

import './styles/tokens.css'
import './styles/base.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// 注册全部 Element Plus 图标
for (const [name, comp] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, comp)
}

// 操作权限指令
app.directive('perm', perm)

// 路由守卫（依赖 pinia，需在 use 之后装配）
setupGuard(router)

app.mount('#app')
