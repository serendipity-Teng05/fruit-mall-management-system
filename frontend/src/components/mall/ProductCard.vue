<template>
  <article class="product-card">
    <router-link :to="`/mall/products/${product.id}`" class="image-link">
      <img :src="imageUrl" :alt="product.name" @error="handleImageError" />
    </router-link>
    <div class="product-content">
      <router-link :to="`/mall/products/${product.id}`" class="product-name">
        {{ product.name }}
      </router-link>
      <p class="product-meta">{{ product.origin || product.category || '源头产区' }} · 已售 {{ product.salesCount || 0 }}{{ product.unit }}</p>
      <div class="product-price-row">
        <span class="product-price"><b>¥{{ formatPrice(product.price) }}</b> / {{ product.unit }}</span>
        <span :class="['stock-state', { empty: product.stock <= 0 }]">
          {{ product.stock > 0 ? `库存 ${product.stock}` : '暂时售罄' }}
        </span>
      </div>
      <div class="product-actions">
        <el-input-number
          v-model="quantity"
          :min="1"
          :max="Math.max(1, product.stock || 1)"
          :disabled="product.stock <= 0"
          size="small"
          controls-position="right"
        />
        <el-button
          class="add-button"
          :disabled="product.stock <= 0"
          :icon="ShoppingCart"
          @click="addToCart"
        >加入购物车</el-button>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useCartStore } from '@/store/cart'

const props = defineProps({ product: { type: Object, required: true } })
const cart = useCartStore()
const quantity = ref(1)
const fallback = ref(false)
const imageUrl = computed(() => fallback.value ? '/images/apple.jpg' : (props.product.image || '/images/apple.jpg'))
const formatPrice = value => Number(value || 0).toFixed(2)
const handleImageError = () => { fallback.value = true }
const addToCart = () => {
  cart.add(props.product, quantity.value)
  ElMessage.success(`${props.product.name} 已加入购物车`)
}
</script>

<style scoped>
.product-card { border: 1px solid var(--mall-border); border-radius: 18px; overflow: hidden; background: #fff; transition: transform .22s, box-shadow .22s, border-color .22s; }
.product-card:hover { transform: translateY(-3px); border-color: #b9dcc7; box-shadow: var(--mall-shadow); }
.image-link { display: block; aspect-ratio: 1.38; overflow: hidden; background: #f4f7f5; }
.image-link img { width: 100%; height: 100%; object-fit: cover; transition: transform .35s; }
.product-card:hover img { transform: scale(1.035); }
.product-content { padding: 16px; }
.product-name { color: var(--mall-text); font-size: 17px; font-weight: 700; text-decoration: none; }
.product-meta { height: 20px; margin: 8px 0 13px; color: var(--mall-muted); font-size: 12px; }
.product-price-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.product-price { color: var(--mall-orange); font-size: 13px; }
.product-price b { font-size: 18px; }
.stock-state { color: var(--mall-green); font-size: 12px; }
.stock-state.empty { color: #9a9f9b; }
.product-actions { display: grid; grid-template-columns: 92px 1fr; gap: 10px; margin-top: 15px; }
.add-button.el-button { min-height: 34px; border-color: var(--mall-green); color: var(--mall-green); border-radius: 9px; font-weight: 600; }
.add-button.el-button:hover { color: #fff; background: var(--mall-green); }
:deep(.el-input-number) { width: 92px; }
@media (max-width: 560px) { .product-content { padding: 13px; } .product-actions { grid-template-columns: 1fr; } :deep(.el-input-number) { width: 100%; } }
</style>
