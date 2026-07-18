<template>
  <el-table :data="logs" v-loading="loading" style="width: 100%">
    <el-table-column prop="orderId" label="订单ID"/>
    <el-table-column prop="statusBefore" label="变更前状态"/>
    <el-table-column prop="statusAfter" label="变更后状态"/>
    <el-table-column prop="remark" label="备注"/>
    <el-table-column prop="createTime" label="时间"/>
  </el-table>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOrderLogs } from '@/api/orderLog'

const props = defineProps({
  orderId: Number
})

const logs = ref([])
const loading = ref(false)

const fetchLogs = async () => {
  loading.value = true
  const res = await getOrderLogs({ orderId: props.orderId })
  logs.value = res.data.records || []
  loading.value = false
}

onMounted(fetchLogs)
</script>

<style scoped>
.el-table { width: 100%; }
</style>