<template>
  <el-container class="layout-container">
    <el-aside width="252px" class="layout-aside">
      <Sidebar />
    </el-aside>
    <el-container class="content-shell">
      <el-header class="layout-header">
        <el-button class="mobile-menu-button" text :icon="Menu" aria-label="打开管理菜单" @click="mobileMenuVisible = true" />
        <div class="page-heading">
          <div class="page-title">{{ route.meta.title || '水果商城管理系统' }}</div>
          <div v-if="route.meta.description" class="page-description">{{ route.meta.description }}</div>
        </div>
        <div class="header-right">
          <el-tooltip content="进入商城浏览并选择商品" placement="bottom">
            <el-button class="storefront-shortcut" @click="goToMall">
              <el-icon><Shop /></el-icon>
              <span class="shortcut-label">前往商城选品</span>
            </el-button>
          </el-tooltip>
          <span class="header-divider" aria-hidden="true"></span>
          <span class="today">{{ today }}</span>
          <el-tooltip content="刷新当前页面"><el-button class="header-icon-button" circle text :icon="Refresh" aria-label="刷新当前页面" @click="reloadPage" /></el-tooltip>
          <el-tooltip content="全屏显示"><el-button class="header-icon-button" circle text :icon="FullScreen" aria-label="全屏显示" @click="toggleFullscreen" /></el-tooltip>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" :key="`${route.fullPath}:${refreshKey}`" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
    <el-drawer v-model="mobileMenuVisible" direction="ltr" size="252px" :with-header="false" class="mobile-drawer">
      <Sidebar @click="mobileMenuVisible = false" />
    </el-drawer>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { FullScreen, Menu, Refresh, Shop } from '@element-plus/icons-vue'
import Sidebar from './Sidebar.vue'

const route = useRoute()
const router = useRouter()
const mobileMenuVisible = ref(false)
const refreshKey = ref(0)
const today = computed(() => new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric', month: 'long', day: 'numeric', weekday: 'short'
}).format(new Date()))

const reloadPage = () => { refreshKey.value += 1 }
const goToMall = () => {
  sessionStorage.setItem('adminReturnPath', route.fullPath)
  router.push('/mall')
}
const toggleFullscreen = () => {
  if (document.fullscreenElement) document.exitFullscreen()
  else document.documentElement.requestFullscreen?.()
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  background: #f4f7f5;
}
.layout-aside {
  background-color: #ffffff;
  border-right: 1px solid #e7ece9;
  transition: width 0.28s;
  overflow-x: hidden;
  z-index: 10;
}
.content-shell { min-width: 0; }
.layout-header {
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid #e7ece9;
  padding: 0 26px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #4e5a53;
  font-size: 14px;
  box-shadow: 0 4px 18px rgba(26, 61, 41, 0.035);
  backdrop-filter: blur(12px);
  z-index: 9;
}
.layout-main {
  min-width: 0;
  background:
    radial-gradient(circle at 92% 5%, rgba(7, 148, 71, 0.045), transparent 26%),
    #f4f7f5;
  padding: 24px;
  overflow: auto;
}
.page-heading { flex: 1; min-width: 0; }
.page-title { color: #17221c; font-size: 18px; font-weight: 760; line-height: 1.2; }
.page-description { margin-top: 5px; color: #8a958f; font-size: 12px; line-height: 1.2; }
.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #6b756f;
}
.storefront-shortcut.el-button {
  min-height: 38px;
  margin-right: 8px;
  padding: 0 15px;
  border-color: #b9ddc8;
  border-radius: 10px;
  background: #eef9f2;
  color: #06723a;
  font-weight: 650;
}
.storefront-shortcut.el-button:hover {
  border-color: #079447;
  background: #079447;
  color: #fff;
}
.storefront-shortcut .el-icon { margin-right: 7px; font-size: 17px; }
.header-divider { width: 1px; height: 24px; margin: 0 10px 0 4px; background: #e5ebe7; }
.today { margin-right: 6px; color: #8a958f; font-size: 13px; white-space: nowrap; }
.header-icon-button.el-button { color: #68736d; }
.header-icon-button.el-button:hover { background: #eef7f1; color: #079447; }
.mobile-menu-button { display: none; margin-right: 8px; }
:global(.mobile-drawer .el-drawer__body) { padding: 0; }
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
@media (max-width: 768px) {
  .layout-aside { display: none; }
  .mobile-menu-button { display: inline-flex; }
  .layout-header { height: 64px; padding: 0 12px; }
  .layout-main { padding: 14px; }
  .today, .header-divider, .page-description { display: none; }
  .storefront-shortcut.el-button { width: 38px; padding: 0; margin-right: 2px; }
  .storefront-shortcut .el-icon { margin: 0; }
  .shortcut-label { display: none; }
  .page-title { font-size: 16px; }
}
</style>
