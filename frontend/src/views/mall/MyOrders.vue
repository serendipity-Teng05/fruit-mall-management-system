<template>
  <div class="mall-page orders-page">
    <div class="orders-heading"><h1>我的订单</h1><el-input v-model="keyword" :prefix-icon="Search" placeholder="搜索订单号或商品" clearable @keyup.enter="search" @clear="search" /></div>
    <div class="status-tabs">
      <button v-for="tab in tabs" :key="String(tab.value)" type="button" :class="{ active: status === tab.value }" @click="changeStatus(tab.value)">{{ tab.label }}</button>
    </div>
    <section v-loading="loading" class="order-list">
      <article v-for="order in orders" :key="order.orderNo" class="order-row">
        <div class="order-main"><span class="order-icon"><el-icon><Tickets /></el-icon></span><span><strong>{{ order.orderNo }}</strong><small>{{ formatTime(order.createTime) }}</small><small v-if="order.status === 0" class="countdown">{{ paymentCountdown(order) }}</small></span></div>
        <div class="order-recipient"><span>收货人</span><strong>{{ order.customerName }}</strong><small>{{ maskPhone(order.customerPhone) }}</small></div>
        <div class="order-amount"><span>订单金额</span><strong>¥{{ Number(order.totalAmount).toFixed(2) }}</strong></div>
        <div class="order-status"><span>状态</span><strong :class="`status-${order.status}`">{{ statusText(order.status) }}</strong></div>
        <div class="order-actions">
          <el-button v-if="order.status === 0" type="danger" @click="openPayment(order)">立即支付</el-button>
          <el-button v-if="order.status === 0" @click="cancel(order)">取消订单</el-button>
          <el-button v-if="order.status === 2" class="mall-primary-button" type="primary" @click="receive(order)">确认收货</el-button>
          <el-button plain @click="viewDetail(order)">查看详情</el-button>
        </div>
      </article>
    </section>
    <el-empty v-if="!loading && orders.length === 0" description="这里还没有相关订单"><el-button class="mall-primary-button" type="primary" @click="$router.push('/mall')">去选购鲜果</el-button></el-empty>
    <div v-if="total > pageSize" class="pagination"><el-pagination background layout="prev, pager, next" :total="total" :page-size="pageSize" :current-page="pageNum" @current-change="changePage" /></div>

    <el-drawer v-model="detailVisible" title="订单详情" size="min(500px, 96vw)">
      <div v-loading="detailLoading" class="order-detail">
        <div class="detail-head"><span><strong>{{ currentOrder.orderNo }}</strong><small>下单时间：{{ formatTime(currentOrder.createTime) }}</small></span><b :class="`status-${currentOrder.status}`">{{ statusText(currentOrder.status) }}</b></div>
        <section><h3><el-icon><Location /></el-icon>收货信息</h3><p>{{ currentOrder.customerName }}　{{ currentOrder.customerPhone }}</p><p>{{ currentOrder.address }}</p></section>
        <section><h3><el-icon><ShoppingBag /></el-icon>商品明细</h3><div v-for="item in currentOrder.items || []" :key="item.id" class="detail-item"><img :src="item.productImage || '/images/apple.jpg'" :alt="item.productName" /><span><strong>{{ item.productName }}</strong><small>¥{{ Number(item.price).toFixed(2) }} / {{ item.productUnit || '份' }}</small></span><b>× {{ item.count }}</b></div><div class="detail-total"><span>订单总额</span><strong>¥{{ Number(currentOrder.totalAmount || 0).toFixed(2) }}</strong></div></section>
        <section><h3><el-icon><Wallet /></el-icon>支付状态</h3><p :class="`status-${currentOrder.status}`">{{ paymentText }}</p><small v-if="latestPayment">渠道：{{ channelName(latestPayment.channel) }}　支付单：{{ latestPayment.paymentNo }}</small></section>
        <div class="drawer-actions"><el-button v-if="currentOrder.status === 0" type="danger" @click="openPayment(currentOrder)">立即支付</el-button><el-button v-if="currentOrder.status === 0" @click="cancel(currentOrder)">取消订单</el-button><el-button v-if="currentOrder.status === 2" class="mall-primary-button" type="primary" @click="receive(currentOrder)">确认收货</el-button></div>
      </div>
    </el-drawer>
    <PaymentDialog v-model="paymentVisible" :order-no="paymentOrder.orderNo" :amount="paymentOrder.totalAmount" @success="paymentDone" />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Location, Search, ShoppingBag, Tickets, Wallet } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelMyOrder, confirmReceipt, getMyOrder, getMyOrders } from '@/api/mall'
import { getPaymentStatus } from '@/api/payment'
import PaymentDialog from '@/components/mall/PaymentDialog.vue'

const route = useRoute(); const tabs = [{ label: '全部', value: null }, { label: '待支付', value: 0 }, { label: '待发货', value: 1 }, { label: '待收货', value: 2 }, { label: '已完成', value: 3 }, { label: '已取消', value: 4 }]
const status = ref(null); const keyword = ref(''); const orders = ref([]); const total = ref(0); const pageNum = ref(1); const pageSize = 10; const loading = ref(false); const detailVisible = ref(false); const detailLoading = ref(false); const currentOrder = ref({}); const latestPayment = ref(null); const paymentVisible = ref(false); const paymentOrder = ref({})
const now = ref(Date.now()); const ticker = window.setInterval(() => { now.value = Date.now() }, 1000)
const statusText = value => ({ 0: '待支付', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消' }[value] || '未知')
const channelName = value => ({ WECHAT: '微信支付演示', ALIPAY: '支付宝演示', DEMO: '演示支付' }[value] || value)
const formatTime = value => value ? String(value).replace('T', ' ') : '--'
const maskPhone = value => value ? value.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '--'
const paymentText = computed(() => currentOrder.value.status === 0 ? '等待支付' : currentOrder.value.status === 4 ? '订单已取消' : latestPayment.value?.status === 1 ? `${channelName(latestPayment.value.channel)}成功` : '已进入履约流程')
const fetchOrders = async () => { loading.value = true; try { const res = await getMyOrders({ pageNum: pageNum.value, pageSize, keyword: keyword.value || undefined, status: status.value }); orders.value = res.data.records || []; total.value = Number(res.data.total || 0) } finally { loading.value = false } }
const search = () => { pageNum.value = 1; fetchOrders() }; const changeStatus = value => { status.value = value; pageNum.value = 1; fetchOrders() }; const changePage = value => { pageNum.value = value; fetchOrders() }
const viewDetail = async order => { currentOrder.value = { ...order, items: [] }; latestPayment.value = null; detailVisible.value = true; detailLoading.value = true; try { const [detail, payment] = await Promise.allSettled([getMyOrder(order.orderNo), getPaymentStatus(order.orderNo)]); if (detail.status === 'fulfilled') currentOrder.value = detail.value.data; if (payment.status === 'fulfilled') latestPayment.value = payment.value.data } finally { detailLoading.value = false } }
const openPayment = order => { paymentOrder.value = order; paymentVisible.value = true }
const cancel = async order => { try { await ElMessageBox.confirm('取消后库存会自动恢复，确认取消订单吗？', '取消订单', { type: 'warning' }); await cancelMyOrder(order.orderNo); ElMessage.success('订单已取消，库存已恢复'); detailVisible.value = false; fetchOrders() } catch (error) { if (error !== 'cancel' && error !== 'close') throw error } }
const receive = async order => { try { await ElMessageBox.confirm('请确认已经收到商品，确认后订单将完成。', '确认收货', { type: 'success' }); await confirmReceipt(order.orderNo); ElMessage.success('确认收货成功'); detailVisible.value = false; fetchOrders() } catch (error) { if (error !== 'cancel' && error !== 'close') throw error } }
const paymentDone = () => { detailVisible.value = false; fetchOrders() }
const paymentCountdown = order => { if (!order.expireTime) return '请尽快完成支付'; const seconds = Math.max(0, Math.floor((new Date(order.expireTime).getTime() - now.value) / 1000)); if (!seconds) return '支付时间已结束'; return `剩余 ${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}` }
onMounted(() => { if (route.query.paid) ElMessage.success(`订单 ${route.query.paid} 支付成功`); fetchOrders() })
onUnmounted(() => window.clearInterval(ticker))
</script>

<style scoped>
.orders-page { padding-top: 38px; }.orders-heading { display: flex; align-items: center; justify-content: space-between; gap: 24px; }.orders-heading h1 { margin: 0; font-size: 36px; }.orders-heading .el-input { width: 300px; }.status-tabs { display: flex; gap: 36px; margin-top: 24px; border-bottom: 1px solid var(--mall-border); overflow-x: auto; }.status-tabs button { border: 0; background: none; color: #555e58; padding: 13px 2px; cursor: pointer; white-space: nowrap; border-bottom: 3px solid transparent; }.status-tabs button.active { color: var(--mall-green); font-weight: 700; border-color: var(--mall-green); }.order-list { min-height: 180px; }.order-row { display: grid; grid-template-columns: 1.45fr .9fr .7fr .65fr 270px; gap: 18px; align-items: center; padding: 20px 16px; border-bottom: 1px solid var(--mall-border); }.order-row:hover { background: #fbfefc; }.order-main { display: grid; grid-template-columns: 38px 1fr; align-items: center; gap: 12px; }.order-icon { width: 38px; height: 38px; border-radius: 10px; background: var(--mall-mint); color: var(--mall-green); display: grid; place-items: center; }.order-main > span:last-child, .order-recipient, .order-amount, .order-status { display: flex; min-width: 0; flex-direction: column; gap: 5px; }.order-row small, .order-row span:first-child { color: var(--mall-muted); font-size: 12px; }.order-main strong { overflow: hidden; text-overflow: ellipsis; }.order-amount strong { color: var(--mall-orange); font-size: 18px; }.order-actions { display: flex; flex-wrap: wrap; gap: 8px; justify-content: flex-end; }.order-actions .el-button + .el-button { margin-left: 0; }.status-0 { color: var(--mall-orange) !important; }.status-1 { color: #287bd1 !important; }.status-2 { color: #7a55bf !important; }.status-3 { color: var(--mall-green) !important; }.status-4 { color: #8d938f !important; }.pagination { display: flex; justify-content: center; margin-top: 28px; }.detail-head { display: flex; justify-content: space-between; padding-bottom: 20px; border-bottom: 1px solid var(--mall-border); }.detail-head > span { display: flex; flex-direction: column; gap: 7px; }.detail-head small, .order-detail section > small { color: var(--mall-muted); }.order-detail section { padding: 20px 0; border-bottom: 1px solid var(--mall-border); }.order-detail h3 { display: flex; align-items: center; gap: 8px; margin: 0 0 14px; }.order-detail h3 .el-icon { color: var(--mall-green); }.order-detail p { color: #4f5752; }.detail-item { display: grid; grid-template-columns: 64px 1fr auto; align-items: center; gap: 12px; padding: 10px 0; }.detail-item img { width: 64px; height: 58px; object-fit: cover; border-radius: 10px; }.detail-item span { display: flex; flex-direction: column; gap: 6px; }.detail-item small { color: var(--mall-muted); }.detail-total { display: flex; justify-content: space-between; margin-top: 14px; padding-top: 14px; border-top: 1px dashed var(--mall-border); }.detail-total strong { color: var(--mall-orange); font-size: 22px; }.drawer-actions { display: flex; justify-content: flex-end; gap: 10px; padding-top: 20px; }.drawer-actions .el-button + .el-button { margin-left: 0; }
@media (max-width: 1080px) { .order-row { grid-template-columns: 1.4fr .8fr .7fr 260px; }.order-recipient { display: none; } }
@media (max-width: 760px) { .orders-heading { align-items: stretch; flex-direction: column; }.orders-heading .el-input { width: 100%; }.status-tabs { gap: 26px; }.order-row { grid-template-columns: 1fr auto; padding: 18px 0; }.order-recipient, .order-status { display: none; }.order-amount { text-align: right; }.order-actions { grid-column: 1 / -1; justify-content: flex-start; }.order-actions .el-button { flex: 1; } }
.countdown { color: var(--mall-orange) !important; }
</style>
