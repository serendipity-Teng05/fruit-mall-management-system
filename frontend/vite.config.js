import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

const backendTarget = process.env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:8080'
const proxyPrefixes = ['/api', '/auth', '/sys_user', '/sys_product', '/sys_order', '/dashboard/data', '/images', '/actuator']

export default defineConfig({
  plugins: [
    vue(),
    Components({ resolvers: [ElementPlusResolver()], dts: false })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('echarts') || id.includes('zrender')) return 'charts'
          if (id.includes('element-plus') || id.includes('@element-plus')) return 'element-plus'
          if (id.includes('vue') || id.includes('pinia')) return 'vue-vendor'
          if (id.includes('axios')) return 'http-vendor'
          return 'vendor'
        }
      }
    }
  },
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
    open: false,
    proxy: Object.fromEntries(proxyPrefixes.map(prefix => [
      prefix,
      { target: backendTarget, changeOrigin: true }
    ]))
  }
})


