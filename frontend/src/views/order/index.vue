<template>
  <div class="page-container">
    <div class="main-card">
      <div class="header-actions">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="custom-tabs">
          <el-tab-pane label="全部订单" name="-1" />
          <el-tab-pane label="待支付" name="0" />
          <el-tab-pane label="待发货" name="1" />
          <el-tab-pane label="待收货" name="2" />
          <el-tab-pane label="已完成" name="3" />
          <el-tab-pane label="已取消" name="4" />
        </el-tabs>

        <div class="search-box">
          <el-input 
            v-model="queryParams.orderNo" 
            placeholder="搜索订单号" 
            prefix-icon="Search"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
            style="width: 220px"
          />
          <el-button type="primary" @click="handleSearch" class="search-btn">查询</el-button>
        </div>
      </div>

      <el-table 
        :data="orderList" 
        style="width: 100%" 
        v-loading="loading"
        :header-cell-style="{ background: '#F9FAFB', color: '#374151', fontWeight: '600' }"
      >
        <el-table-column prop="orderNo" label="订单号" width="180">
          <template #default="scope">
            <span class="order-id">{{ scope.row.orderNo }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="客户信息" min-width="160">
          <template #default="scope">
            <div class="customer-info">
              <span class="name">{{ scope.row.customerName || '匿名用户' }}</span>
              <span class="phone">{{ scope.row.customerPhone }}</span>
              <span class="address">{{ scope.row.address }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="总金额" width="140">
          <template #default="scope">
            <span class="total-amount">¥{{ scope.row.totalAmount }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <div class="status-badge" :class="getStatusStyle(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="下单时间" width="180" class-name="text-gray" />
        
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="scope">
            <el-space wrap>
              <el-button size="small" link type="primary" @click="handleViewDetail(scope.row)">
                查看详情
              </el-button>
              <el-button
                v-if="scope.row.status === 0"
                size="small"
                type="danger"
                plain
                @click="handleUpdateStatus(scope.row, 4)"
              >取消</el-button>
              <el-button
                v-if="scope.row.status === 1"
                size="small"
                class="action-btn-primary"
                @click="handleUpdateStatus(scope.row, 2)"
              >发货</el-button>
              <el-button
                v-if="scope.row.status === 2"
                size="small"
                type="primary"
                plain
                @click="handleUpdateStatus(scope.row, 3)"
              >确认完成</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-container">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="queryParams.pageSize"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog
      v-model="detailVisible"
      title="订单详情"
      width="800px"
      destroy-on-close
      top="5vh"
    >
    <el-steps :active="activeStep" finish-status="success" align-center style="margin-bottom: 30px">
            <el-step title="提交订单" :description="formatTime(currentOrder.createTime)" />
            
            <el-step title="支付成功" :description="currentOrder.status >= 1 ? formatTime(currentOrder.paymentTime) : ''" />
            
            <el-step title="商家发货" :description="currentOrder.status >= 2 ? formatTime(currentOrder.deliveryTime) : ''" />
            
            <el-step title="确认收货" :description="currentOrder.status === 3 ? formatTime(currentOrder.receiveTime) : ''" />
          </el-steps>
		  

      <el-descriptions title="基础信息" :column="2" border style="margin-bottom: 20px">
        <el-descriptions-item label="订单编号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <div class="status-badge" :class="getStatusStyle(currentOrder.status)">
              {{ getStatusText(currentOrder.status) }}
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ currentOrder.createTime }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ currentOrder.status >= 1 ? 'DEMO 演示支付' : '尚未支付' }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions title="收货信息" :column="2" border style="margin-bottom: 20px">
        <el-descriptions-item label="收货人">{{ currentOrder.customerName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentOrder.customerPhone }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ currentOrder.address }}</el-descriptions-item>
      </el-descriptions>

<div class="section-title">商品清单</div>
<el-table :data="currentOrder.items || []" border style="width: 100%" empty-text="暂无商品明细">
  <!-- 商品图片 -->
<el-table-column label="商品图片" width="80" align="center">
  <template #default="scope">
    <el-image
      class="order-product-img"
      :src="getProductImage(scope.row)"
      fit="contain"
    />
  </template>
</el-table-column>

  <!-- 商品名称 -->
  <el-table-column prop="productName" label="商品名称" />

  <!-- 单价 -->
  <el-table-column prop="price" label="单价" width="120">
    <template #default="scope">¥{{ scope.row.price }}</template>
  </el-table-column>

  <!-- 数量 -->
  <el-table-column prop="quantity" label="数量" width="100" align="center">
    <template #default="scope">
      x {{ scope.row.quantity || scope.row.count || 0 }}
    </template>
  </el-table-column>

  <!-- 小计 -->
  <el-table-column label="小计" width="120" align="right">
    <template #default="scope">
      <span style="color: #f56c6c">
        ¥{{ (scope.row.price * (scope.row.quantity || scope.row.count || 0)).toFixed(2) }}
      </span>
    </template>
  </el-table-column>
</el-table>

      <div class="detail-footer">
        <div class="total-box">
          <span>商品总额: </span><span class="price">¥{{ currentOrder.totalAmount }}</span>
          <span style="margin-left: 20px">运费: </span><span class="price">¥0.00</span>
          <span style="margin-left: 20px; font-size: 16px; font-weight: bold">实付款: </span>
          <span class="final-price">¥{{ currentOrder.totalAmount }}</span>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
// ⚠️ 注意：这里必须引入 getOrderDetail
import { getOrderList, updateOrderStatus, getOrderDetail } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue' 

// === 数据定义 ===
const activeTab = ref('-1')
const loading = ref(false)
const orderList = ref([])
const total = ref(0)

// 详情弹窗相关数据
const detailVisible = ref(false)
const currentOrder = ref({})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  orderNo: '',
  status: null 
})

// === 计算属性：计算进度条走到哪一步 ===
const activeStep = computed(() => {
  const status = currentOrder.value.status
  // status: 0=待支付, 1=待发货, 2=待收货, 3=已完成, 4=已取消
  if (status === 0) return 1
  if (status === 1) return 2
  if (status === 2) return 3
  if (status === 3) return 4
  return 0
})

// === 核心逻辑 ===

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getOrderList(queryParams)
    // 后端 MyBatis-Plus 分页对象，数据在 records
    orderList.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleTabChange = (name) => {
  if (name === '-1') {
    queryParams.status = null
  } else {
    queryParams.status = parseInt(name)
  }
  queryParams.pageNum = 1 
  fetchData()
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchData()
}

const handlePageChange = (val) => {
  queryParams.pageNum = val
  fetchData()
}

const handleUpdateStatus = (row, newStatus) => {
  const actionMap = { 2: '发货', 3: '确认完成', 4: '取消订单' }
  const actionText = actionMap[newStatus] || '更新状态'
  ElMessageBox.confirm(`确认对订单 ${row.orderNo} 进行${actionText}吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await updateOrderStatus(row.orderNo, newStatus)
    ElMessage.success(`${actionText}成功`)
    fetchData() 
  })
}

// 🌟 核心优化：查看详情
const handleViewDetail = async (row) => {
  // 1. 【立即响应】先用表格里已有的数据填充弹窗
  // 这样 订单号、收货人、总金额、状态 会立马显示，不会一片白
  currentOrder.value = { ...row, items: [] } 
  
  // 2. 打开弹窗
  detailVisible.value = true
  
  // 3. 【后台加载】去后端查完整的 DTO (包含商品明细 items)
  try {
    const res = await getOrderDetail(row.orderNo)
    if (res.code === '200' || res.code === 200) {
      // 拿到完整数据后覆盖，商品列表就会显示出来了
      currentOrder.value = res.data
    }
  } catch (error) {
    console.error("加载商品明细失败", error)
  }
}

// === 辅助显示函数 ===
const getStatusText = (status) => {
  const map = { 0: '待支付', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消' }
  return map[status] || '未知'
}

const getStatusStyle = (status) => {
  const map = {
    0: 'status-warning', 1: 'status-primary', 2: 'status-info', 3: 'status-success', 4: 'status-danger'
  }
  return map[status] || 'status-info'
}

// 商品名称 -> 图片文件名 映射表
const productImageMap = {
  '红富士苹果': 'apple.jpg',
  '香甜香蕉': 'banana.jpg',
  '巨峰葡萄': 'grape.jpg',
  '海南西瓜': 'watermelon.jpg',
  '红心火龙果': 'pitaya.jpg',
  '丹东奶油草莓': 'strawberry.jpg',
  '泰国金枕榴莲': 'durian.jpg',
  '智利车厘子JJJ': 'cherry.jpg',
  '新疆阿克苏苹果': 'apple.jpg',
  '海南贵妃芒': 'mango.jpg',
  '云南阳光玫瑰': 'rose.jpg',
  '精选混合果切': 'mixed.jpg',
  '黄金百香果': 'passion fruit.jpg',
  '广西沃柑': 'tangerine.jpg',
  '秘鲁进口蓝莓': 'blueberry.jpg',
  '绿宝石甜瓜': 'melon.jpg',
  '泰国5A山竹': 'Mangosteen.jpg',
  '陕西洛川苹果': 'apple.jpg',
  '库尔勒香梨': 'pear.jpg',
  '突尼斯软籽石榴': 'Pomegranate.jpg',
  '越南白肉火龙果': 'dragon.jpg',
  '墨西哥牛油果': 'avocado.jpg',
  '菲律宾凤梨': 'pineapple.jpg',
  '福建琯溪蜜柚': 'pomelo.jpg',
  '山东治化冬枣': 'jujube.jpg',
  '阳山水蜜桃': 'peach.jpg',
  '晴王葡萄': 'grape.jpg',
  '人参果': 'Ginseng fruit.jpg',
  '羊角蜜瓜': 'Honeydew melon.jpg',
  '红肉菠萝蜜': 'jackfruit.jpg',
  '甘肃花牛苹果': 'apple.jpg',
  '攀枝花凯特芒': 'mango.jpg'
}

function getProductImage(item) {
  if (item.image) {
    // 如果数据库里已经有图片路径
    return item.image.startsWith('http')
      ? item.image
      : item.image
  }
  // 根据商品名称映射图片文件
  const filename = productImageMap[item.productName] || 'placeholder.png'
  return '/images/' + encodeURIComponent(filename)
}

// 时间格式化辅助函数
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ') // 把 2026-01-30T12:00 变成 2026-01-30 12:00
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.main-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  border: 1px solid #F3F4F6;
  min-height: 600px;
  display: flex;
  flex-direction: column;
}

/* 头部布局 */
.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-box {
  display: flex;
  gap: 10px;
}

/* Tabs */
.custom-tabs {
  flex: 1;
}
:deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: #F3F4F6;
}
:deep(.el-tabs__item) {
  font-size: 15px;
  color: #6B7280;
}
:deep(.el-tabs__item.is-active) {
  color: #10B981;
  font-weight: 600;
}
:deep(.el-tabs__active-bar) {
  background-color: #10B981;
  height: 3px;
  border-radius: 3px;
}

/* 表格内容 */
.order-id {
  font-family: monospace;
  color: #374151;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.customer-info {
  display: flex;
  flex-direction: column;
}
.customer-info .name {
  font-weight: 500;
  color: #111827;
}
.customer-info .phone {
  font-size: 12px;
  color: #9CA3AF;
  margin-top: 2px;
}
.customer-info .address {
  font-size: 12px;
  color: #9CA3AF;
  transform: scale(0.9);
  transform-origin: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.total-amount {
  font-size: 15px;
  font-weight: 700;
  color: #10B981;
}

/* 状态徽章 */
.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 12px;
  border-radius: 99px;
  font-size: 12px;
  font-weight: 500;
}
.status-warning { background-color: #FFFBEB; color: #F59E0B; }
.status-primary { background-color: #EFF6FF; color: #3B82F6; }
.status-success { background-color: #ECFDF5; color: #10B981; }
.status-info    { background-color: #F3F4F6; color: #6B7280; }
.status-danger  { background-color: #FEF2F2; color: #EF4444; }

/* 按钮 */
.action-btn-primary {
  background-color: #10B981;
  border-color: #10B981;
  color: white;
  border-radius: 6px;
}
.action-btn-primary:hover {
  background-color: #059669;
  border-color: #059669;
}

.action-btn-secondary {
  background-color: #fff;
  border-color: #E5E7EB;
  color: #9CA3AF;
  border-radius: 6px;
}

:deep(.text-gray .cell) {
  color: #9CA3AF;
  font-size: 13px;
}

.pagination-container {
  margin-top: auto;
  padding-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 🌟 详情弹窗样式 */
.section-title {
  font-size: 16px;
  font-weight: bold;
  margin: 20px 0 10px 0;
  padding-left: 10px;
  border-left: 4px solid #10B981; 
}

.detail-footer {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #eee;
  padding-top: 20px;
}

.total-box {
  text-align: right;
}

.price {
  color: #333;
  font-weight: bold;
}

.final-price {
  color: #f56c6c;
  font-size: 20px;
  font-weight: bold;
  margin-left: 5px;
}

.order-product-img {
  width: 40px;
  height: 40px;
  object-fit: contain; /* 保持图片完整显示，不裁剪 */
  border-radius: 4px;
}
</style>
