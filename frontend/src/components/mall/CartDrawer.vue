<template>
  <el-drawer v-model="visible" title="购物车" size="min(420px, 94vw)" class="mall-cart-drawer">
    <div v-if="cart.items.length" class="drawer-content">
      <div class="cart-list">
        <div v-for="item in cart.items" :key="item.id" class="cart-item">
          <img :src="item.image || '/images/apple.jpg'" :alt="item.name" />
          <div class="cart-item-main">
            <strong>{{ item.name }}</strong>
            <span class="mall-price">¥{{ Number(item.price).toFixed(2) }} / {{ item.unit }}</span>
            <el-input-number
              :model-value="item.count"
              :min="1"
              :max="item.stock"
              size="small"
              @change="value => cart.setCount(item.id, value)"
            />
          </div>
          <el-button text circle :icon="Delete" aria-label="移除商品" @click="cart.remove(item.id)" />
        </div>
      </div>
      <div class="cart-summary">
        <div><span>小计</span><strong class="mall-price">¥{{ cart.total.toFixed(2) }}</strong></div>
        <p><el-icon><CircleCheckFilled /></el-icon> 库存与订单实时同步</p>
        <el-button class="mall-primary-button checkout-button" type="primary" @click="goCheckout">去结算</el-button>
      </div>
    </div>
    <el-empty v-else description="购物车还是空的">
      <el-button class="mall-primary-button" type="primary" @click="visible = false">去选购鲜果</el-button>
    </el-empty>
  </el-drawer>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { CircleCheckFilled, Delete } from '@element-plus/icons-vue'
import { useCartStore } from '@/store/cart'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])
const router = useRouter()
const cart = useCartStore()
const visible = computed({ get: () => props.modelValue, set: value => emit('update:modelValue', value) })
const goCheckout = () => { visible.value = false; router.push('/mall/checkout') }
</script>

<style scoped>
.drawer-content { height: 100%; display: flex; flex-direction: column; }
.cart-list { flex: 1; overflow: auto; }
.cart-item { display: grid; grid-template-columns: 92px 1fr 36px; gap: 14px; padding: 16px 0; border-bottom: 1px solid var(--mall-border); }
.cart-item img { width: 92px; height: 92px; border-radius: 13px; object-fit: cover; background: #f3f6f4; }
.cart-item-main { display: flex; min-width: 0; flex-direction: column; gap: 8px; align-items: flex-start; }
.cart-item-main strong { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%; }
.cart-summary { padding-top: 20px; border-top: 1px solid var(--mall-border); }
.cart-summary > div { display: flex; align-items: center; justify-content: space-between; font-size: 16px; }
.cart-summary strong { font-size: 24px; }
.cart-summary p { color: var(--mall-muted); font-size: 12px; display: flex; gap: 6px; align-items: center; }
.cart-summary .el-icon { color: var(--mall-green); }
.checkout-button { width: 100%; margin-top: 8px; }
</style>
