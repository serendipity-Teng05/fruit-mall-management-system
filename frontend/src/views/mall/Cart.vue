<template>
  <div class="mall-page cart-page">
    <div class="page-title"><div><h1>购物车</h1><p>已选择 {{ cart.count }} 件鲜果</p></div><router-link to="/mall">继续选购</router-link></div>
    <div v-if="cart.items.length" class="cart-layout">
      <section class="cart-lines">
        <div v-for="item in cart.items" :key="item.id" class="cart-line">
          <img :src="item.image || '/images/apple.jpg'" :alt="item.name" />
          <div><strong>{{ item.name }}</strong><span>库存 {{ item.stock }} {{ item.unit }}</span></div>
          <span class="unit-price">¥{{ Number(item.price).toFixed(2) }} / {{ item.unit }}</span>
          <el-input-number :model-value="item.count" :min="1" :max="item.stock" @change="value => cart.setCount(item.id, value)" />
          <strong class="mall-price">¥{{ (item.price * item.count).toFixed(2) }}</strong>
          <el-button text circle :icon="Delete" @click="cart.remove(item.id)" />
        </div>
      </section>
      <aside class="cart-total"><h2>订单金额</h2><div><span>商品金额</span><b>¥{{ cart.total.toFixed(2) }}</b></div><div><span>配送费</span><b>¥0.00</b></div><div class="payable"><span>应付金额</span><strong>¥{{ cart.total.toFixed(2) }}</strong></div><el-button class="mall-primary-button" type="primary" @click="$router.push('/mall/checkout')">去结算</el-button><p><el-icon><CircleCheckFilled /></el-icon>库存将在提交订单时实时校验</p></aside>
    </div>
    <el-empty v-else description="购物车还是空的"><el-button class="mall-primary-button" type="primary" @click="$router.push('/mall')">去选购鲜果</el-button></el-empty>
  </div>
</template>
<script setup>
import { CircleCheckFilled, Delete } from '@element-plus/icons-vue'; import { useCartStore } from '@/store/cart'; const cart = useCartStore()
</script>
<style scoped>
.cart-page { padding-top: 38px; }.page-title { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 24px; }.page-title h1 { font-size: 36px; margin: 0; }.page-title p { color: var(--mall-muted); margin: 8px 0 0; }.page-title a { color: var(--mall-green); text-decoration: none; }.cart-layout { display: grid; grid-template-columns: 1fr 350px; gap: 30px; }.cart-lines { border-top: 1px solid var(--mall-border); }.cart-line { display: grid; grid-template-columns: 100px minmax(140px,1fr) 130px 150px 110px 40px; gap: 16px; align-items: center; padding: 20px 0; border-bottom: 1px solid var(--mall-border); }.cart-line img { width: 100px; height: 86px; object-fit: cover; border-radius: 13px; }.cart-line > div { display: flex; flex-direction: column; gap: 7px; }.cart-line > div span, .unit-price { color: var(--mall-muted); font-size: 13px; }.cart-total { align-self: start; position: sticky; top: 98px; border: 1px solid var(--mall-border); border-radius: 18px; padding: 26px; }.cart-total h2 { margin: 0 0 24px; }.cart-total > div { display: flex; justify-content: space-between; padding: 12px 0; }.cart-total .payable { border-top: 1px dashed #d6ddd8; margin-top: 8px; padding-top: 20px; }.payable strong { color: var(--mall-orange); font-size: 26px; }.cart-total .el-button { width: 100%; margin-top: 18px; }.cart-total p { color: var(--mall-muted); font-size: 12px; display: flex; align-items: center; gap: 5px; }.cart-total p .el-icon { color: var(--mall-green); }
@media (max-width: 1000px) { .cart-layout { grid-template-columns: 1fr; }.cart-total { position: static; }.cart-line { grid-template-columns: 86px 1fr 120px 38px; }.cart-line img { width: 86px; height: 76px; }.unit-price { display: none; }.cart-line :deep(.el-input-number) { grid-column: 2; }.cart-line > .mall-price { grid-column: 3; grid-row: 2; } }
@media (max-width: 560px) { .cart-line { grid-template-columns: 72px 1fr 34px; gap: 10px; }.cart-line img { width: 72px; height: 68px; }.cart-line :deep(.el-input-number) { grid-column: 2; width: 120px; }.cart-line > .mall-price { grid-column: 2; grid-row: 3; }.cart-line > .el-button { grid-column: 3; grid-row: 1; } }
</style>
