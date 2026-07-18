<template>
  <div class="page-container">
    <section class="inventory-summary" aria-label="商品管理摘要">
      <div class="summary-copy">
        <h2>商品档案</h2>
        <p>统一维护商品信息、价格、库存与销售状态</p>
      </div>
      <div class="summary-metrics">
        <div><span>商品总数</span><strong>{{ total }}</strong></div>
        <div><span>本页在售</span><strong>{{ onlineCount }}</strong></div>
        <div :class="{ warning: lowStockCount > 0 }"><span>本页库存预警</span><strong>{{ lowStockCount }}</strong></div>
      </div>
    </section>

    <section class="main-card">
      <div class="toolbar">
        <div class="search-area">
          <el-input
            v-model="queryParams.name"
            placeholder="搜索商品名称"
            class="search-input"
            clearable
            @clear="fetchData"
            @keyup.enter="fetchData"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>

          <el-select
            v-model="queryParams.category"
            placeholder="全部分类"
            class="category-select"
            clearable
            @clear="fetchData"
            @change="fetchData"
          >
            <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
          </el-select>

          <el-select
            v-model="queryParams.status"
            placeholder="商品状态"
            class="status-select"
            clearable
            @clear="fetchData"
            @change="fetchData"
          >
            <el-option label="已上架" :value="1" />
            <el-option label="已下架" :value="0" />
          </el-select>

          <el-button class="query-button" @click="fetchData">
            <el-icon><Search /></el-icon><span>查询</span>
          </el-button>
          <el-button class="reset-button" text @click="resetFilters">
            <el-icon><RefreshLeft /></el-icon><span>重置</span>
          </el-button>
        </div>

        <el-button type="success" class="add-btn" @click="handleCreate">
          <el-icon><Plus /></el-icon><span>新增商品</span>
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        row-key="id"
        class="product-table"
        style="width: 100%"
        @sort-change="handleSortChange"
        :default-sort="{ prop: 'id', order: 'ascending' }"
      >
        <el-table-column label="商品信息" min-width="280" prop="id" sortable="custom">
          <template #default="scope">
            <div class="product-info">
              <el-image 
                class="product-img"
                :src="getImageUrl(scope.row.image)" 
                fit="cover"
                lazy
                :preview-src-list="scope.row.image ? [getImageUrl(scope.row.image)] : []"
                preview-teleported
              >
                <template #placeholder>
                  <div class="image-slot loading-slot">
                    <el-icon class="is-loading"><Loading /></el-icon>
                  </div>
                </template>
                <template #error>
                  <div class="image-slot error-slot">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
        
              <div class="product-detail">
                <div class="product-name" :title="scope.row.name">{{ scope.row.name }}</div>
                <div class="product-tags">
                  <el-tag size="small" :type="getTagType(scope.row.category)" effect="light">
                    {{ scope.row.category || '未分类' }}
                  </el-tag>
                  <span class="product-id">ID:{{ scope.row.id }}</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="price" label="价格" width="132" sortable="custom" align="right">
          <template #default="scope">
            <span class="price-text">¥{{ formatPrice(scope.row.price) }}</span>
            <span class="unit-text">/ {{ scope.row.unit }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="stock" label="库存" width="112" sortable="custom" align="right">
          <template #default="scope">
            <div v-if="scope.row.stock < 10 && scope.row.stock > 0" class="stock-value low">
              <el-icon><Warning /></el-icon><span>{{ scope.row.stock }}</span>
            </div>
            <el-tag v-else-if="scope.row.stock === 0" type="info" size="small" effect="dark" round>已售罄</el-tag>
            <span v-else class="stock-value">{{ scope.row.stock }}</span>
          </template>
        </el-table-column>

        <el-table-column label="上架时间" width="172" align="center" prop="createTime" sortable="custom">
          <template #default="scope">
            <span class="time-text">{{ formatTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="118" align="center">
          <template #default="scope">
            <div class="status-control">
              <el-switch
                v-model="scope.row.status"
                :active-value="1"
                :inactive-value="0"
                active-color="#079447"
                inactive-color="#b8c0bb"
                @change="handleStatusChange(scope.row)"
              />
              <span :class="scope.row.status === 1 ? 'is-online' : 'is-offline'">{{ scope.row.status === 1 ? '在售' : '下架' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="218" fixed="right" align="center">
          <template #default="scope">
            <div class="row-actions">
              <el-button link type="primary" :icon="Edit" @click="handleEdit(scope.row)">编辑</el-button>
              <el-button link type="warning" :icon="Box" @click="openUpdateStockDialog(scope.row)">库存</el-button>
              <el-button link type="danger" :icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </section>
	
    <el-dialog
      v-model="stockDialogVisible"
      title="调整商品库存"
      width="min(460px, calc(100vw - 32px))"
      class="custom-dialog"
      align-center
    >
      <div class="stock-dialog-summary">
        <span>当前商品</span>
        <strong>{{ form.name || '-' }}</strong>
        <small>当前库存：{{ form.stock }}</small>
      </div>
      <el-form label-width="92px">
        <el-form-item label="变更数量">
          <el-input-number v-model="stockChange" :min="-form.stock" />
          <span class="stock-dialog-tip">负数表示扣减库存</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="stockChange === 0" @click="handleUpdateStock">保存变更</el-button>
      </template>
    </el-dialog>
	
    <el-dialog 
      v-model="dialogVisible" 
      :title="form.id ? '编辑商品' : '新增商品'" 
      width="min(560px, calc(100vw - 32px))"
      class="custom-dialog"
      align-center
    >
      <el-form ref="formRef" :model="form" label-width="80px" :rules="rules" class="custom-form">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="form.category" allow-create filterable placeholder="选择或输入">
                <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-select v-model="form.unit" placeholder="请选择单位" style="width: 100%">
                <el-option value="斤" label="斤" />
                <el-option value="盒" label="盒" />
                <el-option value="个" label="个" />
                <el-option value="箱" label="箱" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="价格" prop="price">
              <el-input-number 
                v-model="form.price" 
                :precision="2" 
                :step="0.1" 
                :min="0"
                style="width: 100%" 
                controls-position="right"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
             <el-form-item label="库存" prop="stock">
              <el-input-number 
                v-model="form.stock" 
                :min="0" 
                :step="1"
                step-strictly
                style="width: 100%" 
                controls-position="right"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="图片链接" prop="image">
          <el-input v-model="form.image" placeholder="请输入图片URL或本地路径 /images/xxx.jpg" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="产地"><el-input v-model="form.origin" maxlength="100" placeholder="如：山东烟台" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="库存预警值"><el-input-number v-model="form.stockThreshold" :min="0" :max="999999" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="商品介绍"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">上架销售</el-radio>
            <el-radio :label="0">暂不上架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确认保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { Box, Delete, Edit, Loading, Picture, Plus, RefreshLeft, Search, Warning } from '@element-plus/icons-vue'

// 1. 定义分类常量 (供下拉框使用)
const categoryOptions = ['水果', '热带水果', '进口水果', '时令水果', '热销水果', '鲜果切']
const total = ref(0)
const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)
// ===== 新增库存修改功能 =====
const stockDialogVisible = ref(false)
const stockChange = ref(0)
const onlineCount = computed(() => tableData.value.filter(item => item.status === 1).length)
const lowStockCount = computed(() => tableData.value.filter(item => Number(item.stock) < 10).length)

// 打开库存修改弹窗
const openUpdateStockDialog = (row) => {
  Object.assign(form, row)
  stockChange.value = 0
  stockDialogVisible.value = true
}

// 提交库存修改
const handleUpdateStock = async () => {
  try {
    await request.put('/sys_product/stock', { id: form.id, quantityChange: stockChange.value })
    ElMessage.success('库存更新成功')
    stockDialogVisible.value = false
    fetchData()
  } catch (err) {
    console.error(err)
    ElMessage.error('库存更新失败')
  }
}

// 2. 查询参数对象
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  status: null,
  category: '',    // 分类
  sortField: 'id',   // 排序字段
  sortOrder: ''    // 排序顺序
})

// 3. 表单对象
const form = reactive({
  id: null,
  name: '',
  category: '',
  price: 0,
  stock: 0,
  unit: '斤',
  image: '',
  description: '',
  origin: '',
  stockThreshold: 10,
  status: 1
})

// 表单校验规则
const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  category: [{ required: true, message: '请输入分类', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

onMounted(() => {
  fetchData()
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/sys_product/list', {
      params: {
        pageNum: queryParams.pageNum,
        pageSize: queryParams.pageSize,
        name: queryParams.name,
        status: queryParams.status,
        category: queryParams.category,
        sortField: queryParams.sortField, 
        sortOrder: queryParams.sortOrder
      }
    })
     if (res.code === 200 || res.code === '200') {
          // 兼容逻辑：如果是分页对象
          if (res.data.records) {
            tableData.value = res.data.records
            total.value = res.data.total // 👈 关键：获取数据库总条数
          } else {
            // 如果后端没返回分页（不太可能），就用 list.length
            tableData.value = res.data
            total.value = res.data.length
          }
        }
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
}

const resetFilters = () => {
  Object.assign(queryParams, { pageNum: 1, name: '', status: null, category: '' })
  fetchData()
}

// 排序处理
const handleSortChange = ({ prop, order }) => {
  queryParams.sortField = prop
  queryParams.sortOrder = order
  queryParams.pageNum = 1
  fetchData()
}

// 图片地址处理
const getImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return url
}

// 时间格式化
const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  return timeStr.replace('T', ' ')
}

const formatPrice = value => Number(value || 0).toFixed(2)

// 状态切换
const handleStatusChange = async (row) => {
  try {
    await request.put('/sys_product/status', { id: row.id, status: row.status })
    ElMessage.success(row.status === 1 ? '已上架' : '已下架')
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error('操作失败')
  }
}

// 3. 新增：处理每页显示数量变化 (比如从10条变成20条)
const handleSizeChange = (val) => {
  queryParams.pageSize = val
  queryParams.pageNum = 1
  fetchData()
}

// 4. 新增：处理翻页 (比如点击第2页)
const handleCurrentChange = (val) => {
  queryParams.pageNum = val
  fetchData()
}

// 标签颜色
const getTagType = (category) => {
  const map = {
    '水果': '',
    '热带水果': 'warning',
    '进口水果': 'success',
    '时令水果': 'danger',
    '鲜果切': 'info'
  }
  return map[category] || ''
}

// 按钮操作逻辑
const handleCreate = () => {
  form.id = null
  // 重置表单，注意要重置所有字段
  Object.assign(form, { id: null, name: '', category: '', price: 0, stock: 0, unit: '斤', image: '', description: '', origin: '', stockThreshold: 10, status: 1 })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  // 浅拷贝行数据到表单
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认下架商品“${row.name}”吗？历史订单仍会保留。`, '下架商品', {
    type: 'warning',
    confirmButtonText: '确认下架',
    cancelButtonText: '取消'
  })
    .then(async () => {
      await request.post('/sys_product/delete', null, { params: { id: row.id } })
      ElMessage.success('商品已下架')
      fetchData()
    })
    .catch(() => {})
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      await request.post('/sys_product/save', form)
      ElMessage.success('保存成功')
      dialogVisible.value = false
      fetchData()
    }
  })
}
</script>

<style scoped>
.page-container {
  --admin-green: #079447;
  --admin-green-dark: #06723a;
  --admin-text: #18231d;
  --admin-muted: #77827b;
  --admin-border: #e4ebe6;
  display: grid;
  gap: 16px;
}

.inventory-summary {
  min-height: 92px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 18px 22px;
  border: 1px solid var(--admin-border);
  border-radius: 16px;
  background: linear-gradient(110deg, #ffffff 0%, #fbfefc 70%, #f1faf4 100%);
  box-shadow: 0 10px 28px rgba(25, 61, 40, 0.045);
}

.summary-copy h2 { margin: 0; color: var(--admin-text); font-size: 19px; line-height: 1.25; }
.summary-copy p { margin: 7px 0 0; color: var(--admin-muted); font-size: 13px; }

.summary-metrics {
  display: flex;
  align-items: stretch;
}

.summary-metrics > div {
  min-width: 108px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 5px;
  padding: 0 20px;
  border-left: 1px solid var(--admin-border);
}

.summary-metrics span { color: #8b958f; font-size: 11px; }
.summary-metrics strong { color: var(--admin-text); font-size: 21px; line-height: 1; }
.summary-metrics .warning strong { color: #e15a35; }

.main-card {
  min-width: 0;
  overflow: hidden;
  padding: 18px 20px 0;
  border: 1px solid var(--admin-border);
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(25, 61, 40, 0.045);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.search-area {
  min-width: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 9px;
}

.search-input { width: min(340px, 34vw); }
.category-select { width: 138px; }
.status-select { width: 122px; }

.search-area :deep(.el-input__wrapper),
.search-area :deep(.el-select__wrapper) {
  min-height: 40px;
  border-radius: 9px;
  box-shadow: 0 0 0 1px #dfe7e1 inset;
}

.search-area :deep(.el-input__wrapper.is-focus),
.search-area :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px var(--admin-green) inset, 0 0 0 3px rgba(7, 148, 71, 0.1);
}

.query-button.el-button,
.add-btn.el-button {
  min-height: 40px;
  border-radius: 9px;
  font-weight: 650;
}

.query-button.el-button {
  border-color: #b9ddc8;
  background: #eef9f2;
  color: var(--admin-green-dark);
}

.query-button.el-button:hover { border-color: var(--admin-green); background: var(--admin-green); color: #fff; }
.query-button .el-icon, .reset-button .el-icon, .add-btn .el-icon { margin-right: 6px; }
.reset-button.el-button { color: #7b867f; }
.reset-button.el-button:hover { background: #f3f7f4; color: var(--admin-green-dark); }
.add-btn.el-button { flex: 0 0 auto; padding: 0 17px; background: var(--admin-green); border-color: var(--admin-green); }
.add-btn.el-button:hover { background: var(--admin-green-dark); border-color: var(--admin-green-dark); }

.product-table { --el-table-border-color: #edf1ee; --el-table-row-hover-bg-color: #f8fcf9; }
.product-table :deep(.el-table__header th.el-table__cell) { height: 48px; background: #f7faf8; color: #4a564f; font-size: 13px; font-weight: 700; }
.product-table :deep(.el-table__body td.el-table__cell) { padding: 11px 0; }
.product-table :deep(.el-table__inner-wrapper::before) { display: none; }
.product-table :deep(.el-table-fixed-column--right) { background: #fff; }
.product-table :deep(.el-table__row:hover .el-table-fixed-column--right) { background: #f8fcf9; }

.product-info { display: flex; align-items: center; gap: 13px; min-width: 0; }
.product-img {
  width: 58px;
  height: 58px;
  flex: 0 0 58px;
  overflow: hidden;
  border: 1px solid #e3eae5;
  border-radius: 12px;
  background: #f4f7f5;
}
.image-slot { width: 100%; height: 100%; display: flex; justify-content: center; align-items: center; color: #9aa49e; font-size: 20px; }
.product-detail { min-width: 0; display: flex; flex-direction: column; }
.product-name { overflow: hidden; margin-bottom: 7px; color: #26322b; font-size: 14px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.product-tags { display: flex; align-items: center; gap: 7px; }
.product-tags :deep(.el-tag) { border-radius: 7px; }
.product-id { color: #9aa39d; font-size: 11px; }
.price-text { color: #e7501e; font-family: Arial, sans-serif; font-size: 15px; font-weight: 750; }
.unit-text { margin-left: 4px; color: #929c96; font-size: 11px; }
.stock-value { display: inline-flex; align-items: center; justify-content: flex-end; gap: 4px; color: #536058; font-weight: 650; }
.stock-value.low { color: #e2554f; }
.time-text { color: #748078; font-size: 12px; }

.status-control { display: inline-flex; align-items: center; gap: 8px; }
.status-control span { min-width: 24px; font-size: 11px; }
.status-control .is-online { color: var(--admin-green); }
.status-control .is-offline { color: #929c96; }

.row-actions { display: flex; align-items: center; justify-content: center; gap: 4px; }
.row-actions .el-button + .el-button { margin-left: 0; }
.row-actions .el-button { padding: 5px 7px; border-radius: 7px; font-size: 12px; }
.row-actions .el-button:hover { background: #f2f6f3; }

.pagination-container {
  min-height: 68px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-top: 4px;
  border-top: 1px solid #edf1ee;
}

.stock-dialog-summary {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 20px;
  padding: 13px 15px;
  border-radius: 10px;
  background: #f2f9f5;
  color: #748078;
  font-size: 12px;
}
.stock-dialog-summary strong { overflow: hidden; color: #27342c; font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.stock-dialog-tip { margin-left: 10px; color: #919b95; font-size: 12px; }

@media (max-width: 1100px) {
  .inventory-summary { align-items: flex-start; }
  .summary-metrics > div { min-width: 94px; padding: 0 14px; }
  .toolbar { align-items: flex-start; }
  .search-input { width: min(290px, 40vw); }
}

@media (max-width: 768px) {
  .inventory-summary { display: block; padding: 17px; }
  .summary-metrics { margin-top: 18px; }
  .summary-metrics > div { flex: 1; min-width: 0; align-items: flex-start; padding: 0 12px; border-left: 0; border-right: 1px solid var(--admin-border); }
  .summary-metrics > div:first-child { padding-left: 0; }
  .summary-metrics > div:last-child { border-right: 0; }
  .summary-metrics strong { font-size: 18px; }
  .main-card { padding: 14px 14px 0; }
  .toolbar { display: block; }
  .search-area { display: grid; grid-template-columns: 1fr 1fr; }
  .search-input { width: 100%; grid-column: 1 / -1; }
  .category-select, .status-select { width: 100%; }
  .query-button.el-button, .reset-button.el-button { margin-left: 0; }
  .add-btn.el-button { width: 100%; margin-top: 12px; }
  .pagination-container { justify-content: center; overflow-x: auto; }
}

@media (max-width: 480px) {
  .summary-copy p { line-height: 1.55; }
  .search-area { grid-template-columns: 1fr; }
  .search-input { grid-column: auto; }
  .query-button.el-button, .reset-button.el-button { width: 100%; }
  .stock-dialog-summary { grid-template-columns: 1fr; }
  .stock-dialog-tip { display: block; margin: 7px 0 0; }
}
</style>
