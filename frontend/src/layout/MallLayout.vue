<template>
  <div class="mall-shell">
    <header class="mall-header">
      <div class="mall-page header-inner">
        <router-link to="/mall" class="mall-brand" aria-label="鲜果集首页">
          <span class="brand-mark"><el-icon><Apple /></el-icon></span>
          <strong>鲜果集</strong>
        </router-link>
        <nav class="desktop-nav" aria-label="商城导航">
          <router-link to="/mall">首页</router-link>
          <router-link to="/mall?browse=all">全部鲜果</router-link>
          <router-link to="/mall/orders">我的订单</router-link>
        </nav>
        <div class="header-search">
          <el-input v-model="keyword" :prefix-icon="Search" placeholder="搜索苹果、柑橘、礼盒" clearable @keyup.enter="search" @clear="search" />
        </div>
        <div class="header-actions">
          <el-button v-if="canManage" class="admin-return-action" :icon="Back" @click="returnToAdmin">返回管理后台</el-button>
          <el-badge :value="cart.count" :hidden="cart.count === 0">
            <el-button :icon="ShoppingCart" @click="cartVisible = true">购物车</el-button>
          </el-badge>
          <el-dropdown v-if="userStore.isLoggedIn" trigger="click">
            <el-button :icon="User">{{ userStore.user?.realName || userStore.user?.username || '我的账户' }}</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/mall/orders')">我的订单</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/mall/addresses')">收货地址</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button v-else class="login-action" :icon="User" @click="goLogin">登录</el-button>
        </div>
        <el-button class="mobile-menu" text :icon="Menu" aria-label="打开商城导航" @click="mobileNavVisible = true" />
      </div>
    </header>

    <main><router-view /></main>

    <footer class="mall-footer">
      <div class="mall-page footer-inner">
        <strong>鲜果集 · 新鲜到家</strong>
        <span>毕业设计演示系统 · 商品、库存、订单与支付统一管理</span>
      </div>
    </footer>

    <CartDrawer v-model="cartVisible" />
    <el-drawer v-model="mobileNavVisible" direction="ltr" size="260px" :with-header="false">
      <div class="mobile-nav">
        <div class="mall-brand"><span class="brand-mark"><el-icon><Apple /></el-icon></span><strong>鲜果集</strong></div>
        <router-link to="/mall" @click="mobileNavVisible = false">首页</router-link>
        <router-link to="/mall?browse=all" @click="mobileNavVisible = false">全部鲜果</router-link>
        <router-link to="/mall/cart" @click="mobileNavVisible = false">购物车（{{ cart.count }}）</router-link>
        <router-link to="/mall/orders" @click="mobileNavVisible = false">我的订单</router-link>
        <router-link to="/mall/addresses" @click="mobileNavVisible = false">收货地址</router-link>
        <router-link v-if="!userStore.isLoggedIn" :to="{ path: '/login', query: { redirect: route.fullPath } }" @click="mobileNavVisible = false">登录 / 注册</router-link>
        <template v-else>
          <span class="mobile-account">当前账号：{{ userStore.user?.realName || userStore.user?.username }}</span>
          <button v-if="canManage" type="button" class="mobile-admin-return" @click="handleMobileAdminReturn">返回管理后台</button>
          <button type="button" class="mobile-logout" @click="handleMobileLogout">退出登录</button>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Apple, Back, Menu, Search, ShoppingCart, User } from '@element-plus/icons-vue'
import { useCartStore } from '@/store/cart'
import { useUserStore } from '@/store/user'
import { logout } from '@/api/auth'
import CartDrawer from '@/components/mall/CartDrawer.vue'

const router = useRouter()
const route = useRoute()
const cart = useCartStore()
const userStore = useUserStore()
const keyword = ref(String(route.query.keyword || ''))
const cartVisible = ref(false)
const mobileNavVisible = ref(false)
const canManage = computed(() => userStore.permissions.length > 0)

watch(() => route.query.keyword, value => { keyword.value = String(value || '') })
watch(() => userStore.user?.id, value => cart.switchOwner(value), { immediate: true })
const search = () => router.push({ path: '/mall', query: keyword.value.trim() ? { keyword: keyword.value.trim() } : {} })
const goLogin = () => router.push({ path: '/login', query: { redirect: route.fullPath } })
const returnToAdmin = () => {
  const savedPath = sessionStorage.getItem('adminReturnPath') || ''
  const destination = savedPath.startsWith('/') && !savedPath.startsWith('//') ? savedPath : '/dashboard'
  router.push(destination)
}
const handleMobileAdminReturn = () => {
  mobileNavVisible.value = false
  returnToAdmin()
}
const handleLogout = async () => {
  try { await logout() } catch { /* local logout still proceeds */ }
  userStore.logout()
  router.push('/mall')
}
const handleMobileLogout = async () => {
  mobileNavVisible.value = false
  await handleLogout()
}
</script>

<style scoped>
.mall-shell { min-height: 100vh; background: #fff; display: flex; flex-direction: column; }
.mall-header { position: sticky; top: 0; z-index: 30; height: 74px; background: rgba(255,255,255,.96); border-bottom: 1px solid var(--mall-border); backdrop-filter: blur(12px); }
.header-inner { height: 100%; display: grid; grid-template-columns: auto auto minmax(220px, 420px) auto; gap: 34px; align-items: center; }
.mall-brand { display: inline-flex; align-items: center; gap: 9px; color: var(--mall-green); text-decoration: none; font-size: 24px; white-space: nowrap; }
.brand-mark { width: 36px; height: 36px; border: 3px solid currentColor; border-radius: 50%; display: grid; place-items: center; }
.brand-mark .el-icon { font-size: 22px; }
.desktop-nav { display: flex; align-items: stretch; height: 100%; gap: 30px; }
.desktop-nav a { position: relative; display: grid; place-items: center; color: #272c29; text-decoration: none; white-space: nowrap; }
.desktop-nav a.router-link-exact-active { color: var(--mall-green); font-weight: 700; }
.desktop-nav a.router-link-exact-active::after { content: ''; position: absolute; height: 3px; left: 0; right: 0; bottom: 0; background: var(--mall-green); border-radius: 3px 3px 0 0; }
.header-search :deep(.el-input__wrapper) { min-height: 42px; border-radius: 10px; box-shadow: 0 0 0 1px var(--mall-border) inset; }
.header-actions { display: flex; gap: 10px; justify-content: flex-end; }
.header-actions .el-button { min-height: 42px; border-radius: 10px; }
.admin-return-action.el-button { border-color: #b9ddc8; background: var(--mall-mint); color: var(--mall-green-dark); font-weight: 650; }
.admin-return-action.el-button:hover { border-color: var(--mall-green); background: var(--mall-green); color: #fff; }
.login-action.el-button { color: var(--mall-green); border-color: var(--mall-green); }
.mobile-menu { display: none; }
main { flex: 1; }
.mall-footer { border-top: 1px solid var(--mall-border); margin-top: 64px; color: var(--mall-muted); }
.footer-inner { min-height: 96px; display: flex; align-items: center; justify-content: space-between; gap: 20px; font-size: 13px; }
.footer-inner strong { color: var(--mall-green); font-size: 15px; }
.mobile-nav { padding: 24px; display: flex; flex-direction: column; gap: 8px; }
.mobile-nav .mall-brand { margin-bottom: 24px; }
.mobile-nav > a { color: var(--mall-text); text-decoration: none; padding: 14px 10px; border-bottom: 1px solid var(--mall-border); }
.mobile-account { margin-top: 10px; padding: 12px 10px 4px; color: var(--mall-muted); font-size: 13px; }
.mobile-admin-return, .mobile-logout { border: 0; border-bottom: 1px solid var(--mall-border); background: transparent; padding: 14px 10px; text-align: left; cursor: pointer; font: inherit; }
.mobile-admin-return { color: var(--mall-green-dark); font-weight: 700; }
.mobile-logout { color: #d64a4a; }
@media (max-width: 1100px) { .header-inner { grid-template-columns: auto 1fr auto; gap: 18px; } .desktop-nav { display: none; } }
@media (max-width: 760px) { .mall-header { height: 64px; } .header-inner { grid-template-columns: auto 1fr auto; } .header-search, .header-actions { display: none; } .mobile-menu { display: inline-flex; justify-self: end; } .mall-brand { font-size: 21px; } .brand-mark { width: 32px; height: 32px; } .footer-inner { padding: 22px 0; flex-direction: column; align-items: flex-start; justify-content: center; } }
</style>
