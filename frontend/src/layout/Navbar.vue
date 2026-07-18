<template>
  <div class="logo-container">
    <div class="logo-bg">
      <el-icon :size="20" color="#fff"><Apple /></el-icon>
    </div>
    <span class="title">水果商城管理</span>
  </div>
  <el-menu
    active-text-color="#10B981"
    background-color="#ffffff"
    class="el-menu-vertical"
    :default-active="activeMenu"
    text-color="#4B5563"
    router
  >
    <el-menu-item index="/dashboard">
      <el-icon><Odometer /></el-icon>
      <span>数据概览</span>
    </el-menu-item>
    
    <el-menu-item index="/product" v-if="userStore.isAdmin">
      <el-icon><Goods /></el-icon>
      <span>商品管理</span>
    </el-menu-item>
    
    <el-menu-item index="/order">
      <el-icon><List /></el-icon>
      <span>订单管理</span>
    </el-menu-item>
    
    <el-menu-item index="/user" v-if="userStore.isAdmin">
      <el-icon><User /></el-icon>
      <span>人员管理</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
</script>

<style scoped>
.logo-container {
  height: 64px;
  display: flex;
  align-items: center;
  padding-left: 24px;
  background-color: #ffffff;
  /* border-bottom: 1px solid #F3F4F6; */
}
.logo-bg {
  width: 32px;
  height: 32px;
  background-color: #10B981;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  box-shadow: 0 2px 4px rgba(16, 185, 129, 0.2);
}
.title {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  letter-spacing: 0.5px;
}
.el-menu-vertical {
  border-right: none;
  padding: 16px 12px;
}
:deep(.el-menu-item) {
  border-radius: 8px; /* 圆角 */
  margin-bottom: 4px;
  height: 48px;
  margin-left: 8px; /* 左右缩进 */
  margin-right: 8px;
  border: none;
}
:deep(.el-menu-item.is-active) {
  background-color: #ECFDF5 !important; /* 浅绿色背景 */
  font-weight: 600;
}
:deep(.el-menu-item:hover) {
  background-color: #F3F4F6 !important;
}
:deep(.el-menu-item.is-active:hover) {
  background-color: #ECFDF5 !important;
}
</style>