<template>
  <div class="page-container">
    <section class="main-card">
      <header class="toolbar">
        <div class="search-area">
          <el-input v-model="query.userId" placeholder="用户 ID" clearable @keyup.enter="search" />
          <el-input v-model="query.module" placeholder="模块" clearable @keyup.enter="search" />
          <el-input v-model="query.action" placeholder="操作" clearable @keyup.enter="search" />
          <el-button type="success" plain :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </div>
        <el-tag type="info" effect="plain">审计日志不可删除</el-tag>
      </header>
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="createTime" label="时间" width="180"><template #default="{ row }">{{ formatTime(row.createTime) }}</template></el-table-column>
        <el-table-column prop="username" label="操作账号" min-width="120"><template #default="{ row }">{{ row.username || `用户 ${row.userId || '-'}` }}</template></el-table-column>
        <el-table-column prop="module" label="模块" min-width="120" />
        <el-table-column prop="action" label="动作" min-width="140" />
        <el-table-column prop="httpMethod" label="方法" width="90" />
        <el-table-column prop="requestUri" label="请求地址" min-width="220" show-overflow-tooltip />
        <el-table-column label="结果" width="90"><template #default="{ row }"><el-tag :type="row.success === 0 ? 'danger' : 'success'" size="small">{{ row.success === 0 ? '失败' : '成功' }}</el-tag></template></el-table-column>
        <el-table-column prop="durationMs" label="耗时" width="100"><template #default="{ row }">{{ row.durationMs == null ? '-' : `${row.durationMs} ms` }}</template></el-table-column>
        <el-table-column prop="ipAddress" label="IP" width="140" />
        <el-table-column prop="remark" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="requestId" label="请求 ID" min-width="220" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!loading && !rows.length" description="暂无系统审计记录" />
      <div class="pagination"><el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :page-sizes="[10,20,50,100]" layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="fetchData" @current-change="fetchData" /></div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
const loading = ref(false); const rows = ref([]); const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, userId: '', module: '', action: '' })
const fetchData = async () => { loading.value = true; try { const params = { ...query, userId: query.userId || undefined, module: query.module?.trim() || undefined, action: query.action?.trim() || undefined }; const res = await request.get('/api/sys-log/list', { params }); rows.value = res.data?.records || []; total.value = Number(res.data?.total || 0) } finally { loading.value = false } }
const search = () => { query.pageNum = 1; fetchData() }; const reset = () => { Object.assign(query, { pageNum: 1, pageSize: 10, userId: '', module: '', action: '' }); fetchData() }
const formatTime = value => value ? String(value).replace('T', ' ') : '-'
onMounted(fetchData)
</script>

<style scoped>
.page-container { padding: 4px; }.main-card { background: #fff; border: 1px solid #e8eeea; border-radius: 16px; padding: 22px; box-shadow: 0 10px 28px rgba(31,61,42,.045); }.toolbar { display:flex; justify-content:space-between; align-items:center; gap:16px; margin-bottom:20px; }.search-area { display:flex; flex-wrap:wrap; gap:10px; }.search-area .el-input { width:180px; }.pagination { margin-top:20px; display:flex; justify-content:flex-end; }
@media(max-width:760px){.toolbar{align-items:stretch;flex-direction:column}.search-area .el-input{width:100%}}
</style>
