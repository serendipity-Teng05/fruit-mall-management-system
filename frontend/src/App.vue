<template>
  <div v-if="!online" class="network-banner" role="status" aria-live="polite">
    网络连接已断开，浏览内容可能不是最新数据；恢复连接后页面会自动继续使用。
  </div>
  <router-view />
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const online = ref(navigator.onLine)
const updateNetworkState = () => { online.value = navigator.onLine }
onMounted(() => {
  window.addEventListener('online', updateNetworkState)
  window.addEventListener('offline', updateNetworkState)
})
onBeforeUnmount(() => {
  window.removeEventListener('online', updateNetworkState)
  window.removeEventListener('offline', updateNetworkState)
})
</script>

<style>
html, body, #app {
  height: 100%;
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  background-color: #f5f7fa;
}
.network-banner {
  position: fixed;
  z-index: 99999;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  max-width: min(620px, calc(100vw - 32px));
  padding: 10px 16px;
  border: 1px solid #fed7aa;
  border-radius: 12px;
  background: #fff7ed;
  color: #9a3412;
  box-shadow: 0 8px 30px rgba(124, 45, 18, 0.14);
  font-size: 14px;
}
</style>
