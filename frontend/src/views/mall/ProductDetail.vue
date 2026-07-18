<template>
  <div class="mall-page detail-page" v-loading="loading">
    <el-breadcrumb separator="/" class="breadcrumb"><el-breadcrumb-item to="/mall">首页</el-breadcrumb-item><el-breadcrumb-item>{{ product.name || '商品详情' }}</el-breadcrumb-item></el-breadcrumb>
    <section v-if="product.id" class="detail-grid">
      <div class="product-image"><img :src="product.image || '/images/apple.jpg'" :alt="product.name" /></div>
      <div class="detail-content">
        <h1>{{ product.name }}</h1>
        <p class="origin">{{ product.origin || '源头产区' }} · {{ product.category }}</p>
        <p class="description">{{ product.description || `当季严选${product.name}，源头直采，新鲜发货。` }}</p>
        <div class="price-panel"><span>尝鲜价</span><strong>¥{{ Number(product.price).toFixed(2) }}</strong><small>/ {{ product.unit }}</small></div>
        <dl><div><dt>库存</dt><dd>{{ product.stock }} {{ product.unit }}</dd></div><div><dt>累计销量</dt><dd>{{ product.salesCount || 0 }} {{ product.unit }}</dd></div><div><dt>配送</dt><dd>库存实时锁定 · 新鲜发货</dd></div></dl>
        <div class="buy-row">
          <el-input-number v-model="quantity" :min="1" :max="Math.max(1, product.stock)" :disabled="product.stock <= 0" />
          <el-button class="mall-primary-button" type="primary" :disabled="product.stock <= 0" :icon="ShoppingCart" @click="addToCart">{{ product.stock > 0 ? '加入购物车' : '暂时售罄' }}</el-button>
        </div>
      </div>
    </section>
    <el-empty v-else-if="!loading" description="商品服务暂时不可用，请稍后重试" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getMallProduct } from '@/api/mall'
import { useCartStore } from '@/store/cart'
const route = useRoute(); const cart = useCartStore(); const loading = ref(false); const product = ref({}); const quantity = ref(1)
const addToCart = () => { cart.add(product.value, quantity.value); ElMessage.success('已加入购物车') }
onMounted(async () => {
  loading.value = true
  try {
    product.value = (await getMallProduct(route.params.id)).data
  } catch {
    product.value = {}
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.detail-page { padding-top: 28px; }.breadcrumb { margin-bottom: 22px; }.detail-grid { display: grid; grid-template-columns: minmax(0, 1.05fr) minmax(380px, .95fr); gap: 54px; }.product-image { border-radius: 22px; overflow: hidden; background: #f4f7f5; min-height: 520px; }.product-image img { width: 100%; height: 100%; object-fit: cover; }.detail-content { padding: 24px 0; }.detail-content h1 { font-size: 42px; margin: 0 0 12px; }.origin { color: var(--mall-green); font-weight: 650; }.description { color: var(--mall-muted); line-height: 1.8; margin: 24px 0; }.price-panel { background: var(--mall-mint); border-radius: 14px; padding: 20px; display: flex; align-items: baseline; gap: 7px; }.price-panel strong { color: var(--mall-orange); font-size: 38px; }.price-panel small { color: var(--mall-muted); }.detail-content dl { margin: 25px 0; }.detail-content dl > div { display: grid; grid-template-columns: 100px 1fr; padding: 12px 0; border-bottom: 1px solid var(--mall-border); }.detail-content dt { color: var(--mall-muted); }.detail-content dd { margin: 0; }.buy-row { display: grid; grid-template-columns: 150px 1fr; gap: 14px; }.buy-row .el-button { min-height: 48px; }
@media (max-width: 850px) { .detail-grid { grid-template-columns: 1fr; gap: 24px; }.product-image { min-height: 0; aspect-ratio: 1.25; }.detail-content h1 { font-size: 32px; } }
@media (max-width: 480px) { .buy-row { grid-template-columns: 1fr; } :deep(.el-input-number) { width: 100%; } }
</style>
