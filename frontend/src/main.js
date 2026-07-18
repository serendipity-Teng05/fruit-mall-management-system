import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import './styles/mall.css'

const app = createApp(App)

app.config.errorHandler = (error, instance, info) => {
  console.error('Unhandled Vue error', { error, component: instance?.$options?.name, info })
}

app.use(createPinia())
app.use(router)

app.mount('#app')


