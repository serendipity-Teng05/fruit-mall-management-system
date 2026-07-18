<template>
  <div class="page-container"><section class="main-card">
    <header class="toolbar"><el-input v-model="query.orderNo" :prefix-icon="Search" placeholder="搜索订单编号" clearable @clear="search" @keyup.enter="search" /><el-button type="success" plain :icon="Search" @click="search">查询日志</el-button></header>
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="changeTime" label="变更时间" width="180"><template #default="{row}">{{ formatTime(row.changeTime) }}</template></el-table-column>
      <el-table-column prop="orderNo" label="订单编号" min-width="220" />
      <el-table-column label="状态流转" min-width="220"><template #default="{row}"><el-tag effect="plain">{{ row.statusBefore }}</el-tag><span class="arrow">→</span><el-tag type="success">{{ row.statusAfter }}</el-tag></template></el-table-column>
      <el-table-column prop="source" label="来源" width="110"><template #default="{row}">{{ sourceText(row.source) }}</template></el-table-column>
      <el-table-column prop="operatorId" label="操作人 ID" width="110" />
      <el-table-column prop="remark" label="说明" min-width="260" show-overflow-tooltip />
      <el-table-column prop="requestId" label="请求 ID" min-width="200" show-overflow-tooltip />
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="暂无订单状态日志" />
    <div class="pagination"><el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :page-sizes="[10,20,50,100]" layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="fetchData" @current-change="fetchData" /></div>
  </section></div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'; import { Search } from '@element-plus/icons-vue'; import request from '@/utils/request'
const loading=ref(false), rows=ref([]), total=ref(0); const query=reactive({pageNum:1,pageSize:10,orderNo:''})
const fetchData=async()=>{loading.value=true;try{const res=await request.get('/api/sys_order_log/list',{params:{...query,orderNo:query.orderNo.trim()||undefined}});rows.value=res.data?.records||[];total.value=Number(res.data?.total||0)}finally{loading.value=false}}
const search=()=>{query.pageNum=1;fetchData()}; const formatTime=v=>v?String(v).replace('T',' '):'-'; const sourceText=v=>({CUSTOMER:'顾客',ADMIN:'后台',PAYMENT:'支付',SYSTEM:'系统',LEGACY:'历史数据'}[v]||v||'-'); onMounted(fetchData)
</script>
<style scoped>.page-container{padding:4px}.main-card{background:#fff;border:1px solid #e8eeea;border-radius:16px;padding:22px;box-shadow:0 10px 28px rgba(31,61,42,.045)}.toolbar{display:flex;gap:10px;margin-bottom:20px}.toolbar .el-input{width:320px}.arrow{margin:0 10px;color:#98a39c}.pagination{margin-top:20px;display:flex;justify-content:flex-end}@media(max-width:640px){.toolbar .el-input{width:100%}}</style>
