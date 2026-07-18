<template>
  <div class="payment-page">
    <section class="page-heading">
      <div>
        <p class="eyebrow">PAYMENT CENTER</p>
        <h2>支付流水</h2>
        <p class="subtitle">统一查看订单支付状态、渠道和第三方交易号</p>
      </div>
      <el-button type="primary" :icon="Refresh" :loading="loading" @click="fetchPayments">
        刷新数据
      </el-button>
    </section>

    <section class="summary-grid">
      <div class="summary-card">
        <span>当前页流水</span>
        <strong>{{ paymentList.length }}</strong>
      </div>
      <div class="summary-card paid">
        <span>成功支付</span>
        <strong>{{ paidCount }}</strong>
      </div>
      <div class="summary-card amount">
        <span>当前页实收</span>
        <strong>¥{{ paidAmount }}</strong>
      </div>
    </section>

    <section class="table-card">
      <div class="filters">
        <el-input
          v-model="query.orderNo"
          clearable
          placeholder="输入订单号查询"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="query.status" clearable placeholder="全部支付状态" @change="handleSearch">
          <el-option label="待支付" :value="0" />
          <el-option label="已支付" :value="1" />
          <el-option label="已关闭" :value="2" />
          <el-option label="支付失败" :value="3" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>

      <el-table :data="paymentList" v-loading="loading" empty-text="暂无支付流水">
        <el-table-column prop="paymentNo" label="支付单号" min-width="220">
          <template #default="{ row }"><span class="mono">{{ row.paymentNo }}</span></template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单号" min-width="190" />
        <el-table-column prop="channel" label="渠道" width="100">
          <template #default="{ row }"><el-tag effect="plain">{{ row.channel }}</el-tag></template>
        </el-table-column>
        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }"><strong class="money">¥{{ formatMoney(row.amount) }}</strong></template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type">{{ statusMeta(row.status).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="providerTradeNo" label="第三方交易号" min-width="240">
          <template #default="{ row }"><span class="mono muted">{{ row.providerTradeNo || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="180" />
        <el-table-column prop="paidTime" label="支付时间" min-width="180">
          <template #default="{ row }">{{ row.paidTime || '—' }}</template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        class="pagination"
        background
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        :total="total"
        @current-change="fetchPayments"
        @size-change="handleSearch"
      />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getPaymentList } from '@/api/payment'

const loading = ref(false)
const paymentList = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, orderNo: '', status: null })

const paidCount = computed(() => paymentList.value.filter(item => item.status === 1).length)
const paidAmount = computed(() => paymentList.value
  .filter(item => item.status === 1)
  .reduce((sum, item) => sum + Number(item.amount || 0), 0)
  .toFixed(2))

const statusMeta = status => ({
  0: { text: '待支付', type: 'warning' },
  1: { text: '已支付', type: 'success' },
  2: { text: '已关闭', type: 'info' },
  3: { text: '支付失败', type: 'danger' }
}[status] || { text: '未知', type: 'info' })

const formatMoney = value => Number(value || 0).toFixed(2)

const fetchPayments = async () => {
  loading.value = true
  try {
    const res = await getPaymentList(query)
    paymentList.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.pageNum = 1
  fetchPayments()
}

onMounted(fetchPayments)
</script>

<style scoped>
.payment-page { display: flex; flex-direction: column; gap: 20px; }
.page-heading { display: flex; align-items: center; justify-content: space-between; }
.page-heading h2 { margin: 2px 0 6px; color: #111827; font-size: 26px; }
.eyebrow { margin: 0; color: #10b981; font-size: 11px; font-weight: 800; letter-spacing: 1.8px; }
.subtitle { margin: 0; color: #6b7280; font-size: 14px; }
.summary-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.summary-card { padding: 20px; border: 1px solid #e5e7eb; border-radius: 14px; background: linear-gradient(135deg, #fff, #f8fafc); }
.summary-card span { display: block; margin-bottom: 8px; color: #6b7280; font-size: 13px; }
.summary-card strong { color: #111827; font-size: 28px; }
.summary-card.paid { background: linear-gradient(135deg, #ecfdf5, #fff); }
.summary-card.paid strong, .summary-card.amount strong { color: #059669; }
.summary-card.amount { background: linear-gradient(135deg, #f0fdf4, #fff); }
.table-card { padding: 22px; border: 1px solid #e5e7eb; border-radius: 14px; background: #fff; box-shadow: 0 4px 18px rgba(15, 23, 42, .04); }
.filters { display: grid; grid-template-columns: minmax(240px, 360px) 180px auto; gap: 12px; margin-bottom: 20px; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }
.muted { color: #6b7280; }
.money { color: #059669; }
.pagination { justify-content: flex-end; margin-top: 20px; }
@media (max-width: 900px) {
  .page-heading { align-items: flex-start; gap: 16px; }
  .summary-grid { grid-template-columns: 1fr; }
  .filters { grid-template-columns: 1fr; }
  .table-card { padding: 14px; overflow: hidden; }
}
</style>
