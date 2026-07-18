<template>
  <el-dialog v-model="visible" title="选择支付方式" width="min(480px, 92vw)" :close-on-click-modal="false" :before-close="handleClose">
    <div class="payment-options">
      <button
        v-for="option in channels"
        :key="option.value"
        type="button"
        :class="['payment-option', { active: channel === option.value }]"
        @click="channel = option.value"
      >
        <span :class="['channel-icon', option.value.toLowerCase()]">{{ option.short }}</span>
        <span><strong>{{ option.label }}</strong><small>{{ option.desc }}</small></span>
        <el-icon><CircleCheckFilled v-if="channel === option.value" /><CircleCheck v-else /></el-icon>
      </button>
    </div>
    <div class="demo-notice" :class="{ unavailable: !capabilities.demoMode }">
      <el-icon><InfoFilled /></el-icon>
      {{ capabilities.demoMode ? '当前为毕业设计演示环境，不会产生真实扣款' : '真实支付渠道尚未配置，请稍后再试或联系管理员' }}
    </div>
    <div class="payment-amount"><span>支付金额</span><strong>¥{{ Number(amount || 0).toFixed(2) }}</strong></div>
    <template #footer>
      <el-button @click="later">稍后支付</el-button>
      <el-button class="mall-primary-button" type="primary" :loading="loading" :disabled="!channels.length" @click="pay">
        {{ capabilities.demoMode ? '确认演示支付' : '支付暂不可用' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { CircleCheck, CircleCheckFilled, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { confirmDemoPayment, createPayment, getPaymentCapabilities } from '@/api/payment'

const props = defineProps({ modelValue: Boolean, orderNo: String, amount: [Number, String] })
const emit = defineEmits(['update:modelValue', 'success', 'later'])
const visible = computed({ get: () => props.modelValue, set: value => emit('update:modelValue', value) })
const channel = ref('DEMO')
const loading = ref(false)
const channels = ref([])
const capabilities = ref({ demoMode: false, channels: [] })
const iconText = { WECHAT: '微', ALIPAY: '支', DEMO: '演' }

const loadCapabilities = async () => {
  try {
    const response = await getPaymentCapabilities()
    capabilities.value = response.data || { demoMode: false, channels: [] }
    channels.value = (capabilities.value.channels || []).map(item => ({
      ...item,
      short: iconText[item.value] || '付',
      desc: item.description
    }))
    channel.value = channels.value[0]?.value || ''
  } catch {
    capabilities.value = { demoMode: false, channels: [] }
    channels.value = []
  }
}

watch(visible, value => { if (value) loadCapabilities() })

const pay = async () => {
  if (!props.orderNo || !channel.value) return
  loading.value = true
  try {
    const created = await createPayment(props.orderNo, channel.value)
    const result = created.data
    if (result.demoMode) {
      await confirmDemoPayment(result.payment.paymentNo)
      ElMessage.success(`${result.channelName}演示成功`)
      visible.value = false
      emit('success', result.payment)
    } else {
      ElMessage.info(result.instruction)
    }
  } finally {
    loading.value = false
  }
}
const later = () => { visible.value = false; emit('later') }
const handleClose = done => { done(); emit('later') }
</script>

<style scoped>
.payment-options { display: grid; gap: 10px; }
.payment-option { width: 100%; border: 1px solid var(--mall-border); border-radius: 12px; background: #fff; padding: 14px; display: grid; grid-template-columns: 40px 1fr 24px; align-items: center; gap: 12px; text-align: left; cursor: pointer; color: var(--mall-text); }
.payment-option:hover, .payment-option.active { border-color: var(--mall-green); background: #f8fdf9; }
.payment-option > span:nth-child(2) { display: flex; flex-direction: column; gap: 3px; }
.payment-option small { color: var(--mall-muted); }
.payment-option > .el-icon { color: var(--mall-green); font-size: 20px; }
.channel-icon { width: 36px; height: 36px; border-radius: 10px; display: grid; place-items: center; color: #fff; font-weight: 800; }
.channel-icon.wechat { background: #08b75b; }.channel-icon.alipay { background: #1688ee; }.channel-icon.demo { background: #454b47; }
.demo-notice { margin: 16px 0; padding: 12px 14px; border-radius: 10px; background: var(--mall-mint); color: var(--mall-green-dark); font-size: 13px; display: flex; align-items: center; gap: 8px; }
.demo-notice.unavailable { background: #fff7ed; color: #9a5412; }
.payment-amount { padding-top: 14px; border-top: 1px solid var(--mall-border); display: flex; justify-content: space-between; align-items: center; }
.payment-amount strong { color: var(--mall-orange); font-size: 25px; }
</style>
