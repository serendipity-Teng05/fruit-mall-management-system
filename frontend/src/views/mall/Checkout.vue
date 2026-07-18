<template>
  <div class="mall-page checkout-page">
    <h1>确认订单</h1>
    <div v-if="cart.items.length || createdOrderNo" class="checkout-layout">
      <div class="checkout-main">
        <section class="checkout-section">
          <div class="section-title"><span><el-icon><Location /></el-icon>收货地址</span><el-button plain type="success" :icon="Plus" @click="openAddress()">新增地址</el-button></div>
          <div v-if="addresses.length" class="address-list">
            <button v-for="address in addresses" :key="address.id" type="button" :class="['address-row', { active: selectedAddressId === address.id }]" @click="selectedAddressId = address.id">
              <span class="radio-dot"></span>
              <span class="address-copy"><strong>{{ address.recipientName }}　{{ maskPhone(address.phone) }}</strong><small>{{ fullAddress(address) }}</small></span>
              <span class="address-actions"><a @click.stop.prevent="openAddress(address)">编辑</a><a @click.stop.prevent="removeAddress(address)">删除</a></span>
            </button>
          </div>
          <el-empty v-else description="请先新增一个收货地址" :image-size="80" />
        </section>

        <section class="checkout-section">
          <div class="section-title"><span><el-icon><ShoppingBag /></el-icon>商品清单</span></div>
          <div class="checkout-items">
            <div v-for="item in cart.items" :key="item.id" class="checkout-item">
              <img :src="item.image || '/images/apple.jpg'" :alt="item.name" />
              <span><strong>{{ item.name }}</strong><small>{{ item.unit }}装 · 新鲜发货</small></span>
              <span>¥{{ Number(item.price).toFixed(2) }} × {{ item.count }}</span>
              <strong class="mall-price">¥{{ (Number(item.price) * item.count).toFixed(2) }}</strong>
            </div>
          </div>
        </section>
      </div>

      <aside class="order-summary">
        <h2><el-icon><Wallet /></el-icon>订单金额</h2>
        <div><span>商品金额</span><b>¥{{ cart.total.toFixed(2) }}</b></div>
        <div><span>配送费</span><b>¥0.00</b></div>
        <div class="payable"><span>应付金额</span><strong>¥{{ cart.total.toFixed(2) }}</strong></div>
        <el-alert v-if="cart.hasInvalidItems" title="购物车中有已下架或库存不足的商品，请返回购物车处理" type="warning" :closable="false" show-icon />
        <el-button class="mall-primary-button" type="primary" :loading="submitting" :disabled="!selectedAddressId || !cart.items.length || cart.hasInvalidItems" @click="submitOrder">提交订单</el-button>
        <p><el-icon><CircleCheckFilled /></el-icon>价格、库存与订单均以后端校验为准</p>
      </aside>
    </div>
    <el-empty v-else description="没有待结算的商品"><el-button class="mall-primary-button" type="primary" @click="$router.push('/mall')">返回商城</el-button></el-empty>

    <el-dialog v-model="addressDialog" :title="addressForm.id ? '编辑收货地址' : '新增收货地址'" width="min(560px, 94vw)">
      <el-form ref="addressFormRef" :model="addressForm" :rules="addressRules" label-position="top">
        <div class="address-form-grid"><el-form-item label="收货人" prop="recipientName"><el-input v-model="addressForm.recipientName" maxlength="50" /></el-form-item><el-form-item label="手机号" prop="phone"><el-input v-model="addressForm.phone" maxlength="11" /></el-form-item></div>
        <div class="address-form-grid three"><el-form-item label="省" prop="province"><el-input v-model="addressForm.province" placeholder="福建省" /></el-form-item><el-form-item label="市" prop="city"><el-input v-model="addressForm.city" placeholder="泉州市" /></el-form-item><el-form-item label="区/县"><el-input v-model="addressForm.district" placeholder="丰泽区" /></el-form-item></div>
        <el-form-item label="详细地址" prop="detailAddress"><el-input v-model="addressForm.detailAddress" type="textarea" :rows="3" maxlength="255" show-word-limit /></el-form-item>
        <el-checkbox v-model="addressForm.defaultChecked">设为默认地址</el-checkbox>
      </el-form>
      <template #footer><el-button @click="addressDialog = false">取消</el-button><el-button class="mall-primary-button" type="primary" :loading="savingAddress" @click="saveAddress">保存地址</el-button></template>
    </el-dialog>

    <PaymentDialog v-model="paymentVisible" :order-no="createdOrderNo" :amount="createdAmount" @success="finishPayment" @later="finishLater" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CircleCheckFilled, Location, Plus, ShoppingBag, Wallet } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createAddress, createMallOrder, deleteAddress, getAddresses, getMallProduct, updateAddress } from '@/api/mall'
import { useCartStore } from '@/store/cart'
import PaymentDialog from '@/components/mall/PaymentDialog.vue'

const router = useRouter(); const cart = useCartStore(); const addresses = ref([]); const selectedAddressId = ref(); const addressDialog = ref(false); const addressFormRef = ref(); const savingAddress = ref(false); const submitting = ref(false); const paymentVisible = ref(false); const createdOrderNo = ref(''); const createdAmount = ref(0)
const blankAddress = () => ({ id: null, recipientName: '', phone: '', province: '', city: '', district: '', detailAddress: '', defaultChecked: false })
const addressForm = reactive(blankAddress())
const addressRules = { recipientName: [{ required: true, message: '请输入收货人', trigger: 'blur' }], phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入正确手机号', trigger: 'blur' }], province: [{ required: true, message: '请输入省份', trigger: 'blur' }], city: [{ required: true, message: '请输入城市', trigger: 'blur' }], detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }] }
const fullAddress = a => `${a.province || ''}${a.city || ''}${a.district || ''}${a.detailAddress || ''}`
const maskPhone = value => value ? value.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : ''
const loadAddresses = async () => { const res = await getAddresses(); addresses.value = res.data || []; const selected = addresses.value.find(a => a.id === selectedAddressId.value) || addresses.value.find(a => a.isDefault === 1) || addresses.value[0]; selectedAddressId.value = selected?.id }
const openAddress = address => { Object.assign(addressForm, address ? { ...address, defaultChecked: address.isDefault === 1 } : blankAddress()); addressDialog.value = true }
const saveAddress = async () => { const valid = await addressFormRef.value?.validate().catch(() => false); if (!valid) return; savingAddress.value = true; try { const payload = { ...addressForm, isDefault: addressForm.defaultChecked ? 1 : 0 }; const res = addressForm.id ? await updateAddress(addressForm.id, payload) : await createAddress(payload); selectedAddressId.value = res.data.id; addressDialog.value = false; await loadAddresses(); ElMessage.success('收货地址已保存') } finally { savingAddress.value = false } }
const removeAddress = address => ElMessageBox.confirm('确认删除这个收货地址吗？', '删除地址', { type: 'warning' }).then(async () => { await deleteAddress(address.id); await loadAddresses(); ElMessage.success('地址已删除') })
const requestId = () => globalThis.crypto?.randomUUID?.() || `order_${Date.now()}_${Math.random().toString(16).slice(2)}`
const submitOrder = async () => { submitting.value = true; try { await cart.refresh(getMallProduct); if (cart.hasInvalidItems) { ElMessage.warning('商品状态或库存已变化，请处理后再提交'); return } const res = await createMallOrder({ requestId: requestId(), addressId: selectedAddressId.value, items: cart.items.map(item => ({ productId: item.id, count: item.count })) }); createdOrderNo.value = res.data.orderNo; createdAmount.value = Number(res.data.totalAmount || 0); cart.clear(); paymentVisible.value = true; ElMessage.success('订单创建成功，请选择支付方式') } finally { submitting.value = false } }
const finishPayment = () => router.replace({ path: '/mall/orders', query: { paid: createdOrderNo.value } })
const finishLater = () => router.replace('/mall/orders')
onMounted(async () => { await Promise.allSettled([loadAddresses(), cart.refresh(getMallProduct)]) })
</script>

<style scoped>
.checkout-page { padding-top: 38px; }.checkout-page > h1 { font-size: 36px; margin: 0 0 24px; }.checkout-layout { display: grid; grid-template-columns: 1fr 390px; gap: 30px; }.checkout-main { display: grid; gap: 26px; }.checkout-section, .order-summary { border: 1px solid var(--mall-border); border-radius: 18px; overflow: hidden; background: #fff; }.section-title { min-height: 70px; padding: 16px 24px; background: linear-gradient(90deg, #f2faf5, #fbfefc); display: flex; align-items: center; justify-content: space-between; }.section-title > span { display: flex; gap: 10px; align-items: center; font-size: 18px; font-weight: 750; }.section-title .el-icon { color: var(--mall-green); font-size: 24px; }.address-list { padding: 22px; display: grid; gap: 12px; }.address-row { width: 100%; border: 1px solid var(--mall-border); background: #fff; border-radius: 12px; padding: 18px; display: grid; grid-template-columns: 22px 1fr auto; gap: 12px; align-items: center; text-align: left; cursor: pointer; }.address-row.active { border-color: var(--mall-green); background: #fbfefc; box-shadow: 0 0 0 1px var(--mall-green) inset; }.radio-dot { width: 16px; height: 16px; border: 2px solid #b8c1bb; border-radius: 50%; }.address-row.active .radio-dot { border: 5px solid var(--mall-green); }.address-copy { display: flex; flex-direction: column; gap: 7px; }.address-copy small { color: var(--mall-muted); }.address-actions { display: flex; gap: 14px; }.address-actions a { color: var(--mall-green); }.checkout-items { padding: 0 24px; }.checkout-item { display: grid; grid-template-columns: 90px 1fr 150px 100px; gap: 16px; align-items: center; padding: 20px 0; border-bottom: 1px solid var(--mall-border); }.checkout-item:last-child { border: 0; }.checkout-item img { width: 90px; height: 80px; object-fit: cover; border-radius: 12px; }.checkout-item > span { display: flex; flex-direction: column; gap: 7px; }.checkout-item small, .checkout-item > span:nth-child(3) { color: var(--mall-muted); }.order-summary { align-self: start; position: sticky; top: 98px; padding: 26px; }.order-summary h2 { margin: 0 0 20px; display: flex; align-items: center; gap: 9px; }.order-summary h2 .el-icon { color: var(--mall-green); }.order-summary > div { display: flex; justify-content: space-between; padding: 13px 0; }.order-summary .payable { border-top: 1px dashed #d5ddd7; margin-top: 10px; padding-top: 24px; }.payable strong { color: var(--mall-orange); font-size: 28px; }.order-summary .el-button { width: 100%; margin-top: 18px; }.order-summary p { color: var(--mall-muted); font-size: 12px; display: flex; gap: 5px; align-items: center; }.order-summary p .el-icon { color: var(--mall-green); }.address-form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }.address-form-grid.three { grid-template-columns: repeat(3, 1fr); }
@media (max-width: 960px) { .checkout-layout { grid-template-columns: 1fr; }.order-summary { position: static; } }
@media (max-width: 620px) { .checkout-item { grid-template-columns: 70px 1fr auto; }.checkout-item img { width: 70px; height: 65px; }.checkout-item > span:nth-child(3) { grid-column: 2; }.checkout-item > .mall-price { grid-column: 3; grid-row: 1; }.address-row { grid-template-columns: 20px 1fr; }.address-actions { grid-column: 2; }.address-form-grid, .address-form-grid.three { grid-template-columns: 1fr; gap: 0; } }
</style>
