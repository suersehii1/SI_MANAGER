import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import ko from 'element-plus/es/locale/lang/ko'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import 'ag-grid-community/styles/ag-grid.css'
import 'ag-grid-community/styles/ag-theme-quartz.css'
import './styles.css'
import './utils/echarts'

import App from './App.vue'
import router from './router'
import { setupInterceptors } from './api/http'
import { useAuthStore } from './stores/auth'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: ko })
for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component)
}

setupInterceptors(router, useAuthStore)

app.mount('#app')
