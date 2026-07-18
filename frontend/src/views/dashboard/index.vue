<template>
  <div class="dashboard-container">
    <div class="dashboard-header">
      <h2>数据概览</h2>
    </div>

    <el-row :gutter="24">
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="stat-content">
            <div class="label">总销售额</div>
            <div class="value">¥{{ dashboardData.totalSales }}</div>
          </div>
          <div class="stat-icon-bg bg-green">
            <el-icon><Money /></el-icon>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="stat-content">
            <div class="label">待处理订单</div>
            <div class="value">{{ dashboardData.pendingOrders }}</div>
          </div>
          <div class="stat-icon-bg bg-blue">
            <el-icon><List /></el-icon>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="stat-content">
            <div class="label">库存预警</div>
            <div class="value" :class="{ 'text-danger': dashboardData.lowStockCount > 0 }">
              {{ dashboardData.lowStockCount }}
            </div>
          </div>
          <div class="stat-icon-bg bg-red">
            <el-icon><Warning /></el-icon>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="stat-content">
            <div class="label">商品总数</div>
            <div class="value">{{ dashboardData.productCount }}</div>
          </div>
          <div class="stat-icon-bg bg-purple">
            <el-icon><TrendCharts /></el-icon>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="24" style="margin-top: 24px;">
      <el-col :xs="24" :lg="16">
        <div class="chart-card">
          <div class="chart-header">近7天销售趋势</div>
          <div class="chart-body">
             <div ref="barChartRef" style="height: 320px; width: 100%;"></div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="8">
        <div class="chart-card">
          <div class="chart-header">订单状态分布</div>
          <div class="chart-body">
            <div ref="pieChartRef" style="height: 320px; width: 100%;"></div>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="alert-section" v-if="dashboardData.lowStockList && dashboardData.lowStockList.length > 0">
      <div class="alert-header">
        <el-icon><WarningFilled /></el-icon>
        <span>库存严重不足预警 (低于10件)</span>
      </div>
      <el-row :gutter="24">
        <el-col :xs="24" :md="12" :lg="8" v-for="item in dashboardData.lowStockList" :key="item.id">
          <div class="alert-item">
            <div class="alert-info">
              <div class="name">{{ item.name }}</div>
              <div class="code">编号: {{ item.id }}</div>
            </div>
            <div class="alert-stat">
              <div class="stock">{{ item.stock }} <span class="unit">{{ item.unit }}</span></div>
              <div class="tag">需补货</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, reactive } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import request from '@/utils/request' 

echarts.use([BarChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const barChartRef = ref(null)
const pieChartRef = ref(null)

// 定义响应式数据对象
const dashboardData = reactive({
  totalSales: 0.00,
  pendingOrders: 0,
  lowStockCount: 0,
  productCount: 0,
  lowStockList: [], // 存储库存预警商品列表
  chartDays: [],
  chartSales: [],
  statusData: []
})

// 加载数据的函数
const loadData = async () => {
  try {
    // 调用后端接口
    const res = await request.get('/dashboard/data')
    
    // 注意：根据你的 Result 封装，可能是 code===200 或 code==='200'
    if (res.code === 200 || res.code === '200') {
      const data = res.data
      
      // 1. 赋值基础数据
      dashboardData.totalSales = data.totalSales
      dashboardData.pendingOrders = data.pendingOrders
      dashboardData.lowStockCount = data.lowStockCount
      dashboardData.productCount = data.productCount
      // 核心：赋值底部列表
      dashboardData.lowStockList = data.lowStockList || []

      // 2. 初始化图表
      initBarChart(data.chartDays, data.chartSales)
      initPieChart(data.statusData)
    } else {
      console.error('获取数据失败:', res.msg)
    }
  } catch (error) {
    console.error("网络请求异常", error)
  }
}

const resizeCharts = () => {
  echarts.getInstanceByDom(barChartRef.value)?.resize()
  echarts.getInstanceByDom(pieChartRef.value)?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  echarts.getInstanceByDom(barChartRef.value)?.dispose()
  echarts.getInstanceByDom(pieChartRef.value)?.dispose()
})

const initBarChart = (days, sales) => {
  if (!barChartRef.value) return
  
  let myChart = echarts.getInstanceByDom(barChartRef.value)
  if (!myChart) myChart = echarts.init(barChartRef.value)

  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '2%', right: '2%', bottom: '5%', top: '15%', containLabel: true },
    xAxis: [
      {
        type: 'category',
        data: days || [], 
        axisTick: { show: false },
        axisLine: { show: false },
        axisLabel: { color: '#6B7280', margin: 12 }
      }
    ],
    yAxis: [
      {
        type: 'value',
        splitLine: { lineStyle: { type: 'dashed', color: '#F3F4F6' } },
        axisLabel: { formatter: '¥{value}', color: '#9CA3AF' }
      }
    ],
    series: [
      {
        name: '销售额',
        type: 'bar',
        barWidth: '60%',
        data: sales || [],
        itemStyle: { color: '#10B981', borderRadius: [4, 4, 0, 0] },
        showBackground: true,
        backgroundStyle: { color: 'rgba(180, 180, 180, 0.05)' }
      }
    ]
  }
  myChart.setOption(option)
}

const initPieChart = (statusData) => {
  if (!pieChartRef.value) return
  let myChart = echarts.getInstanceByDom(pieChartRef.value)
  if (!myChart) myChart = echarts.init(pieChartRef.value)

  // 颜色映射
  const colorMap = {
    '待支付': '#FBBF24',
    '待发货': '#60A5FA',
    '待收货': '#818CF8',
    '已完成': '#34D399'
  }
  
  const formattedData = (statusData || []).map(item => ({
    ...item,
    itemStyle: { color: colorMap[item.name] || '#ccc' }
  }))

  const option = {
    tooltip: { trigger: 'item' },
    legend: { 
      bottom: '5%', 
      left: 'center', 
      itemWidth: 8, 
      itemHeight: 8,
      icon: 'circle',
      textStyle: { color: '#6B7280' }
    },
    series: [
      {
        name: '订单状态',
        type: 'pie',
        radius: ['45%', '75%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 0, borderColor: '#fff', borderWidth: 3 },
        label: { show: false, position: 'center' },
        emphasis: { label: { show: false } },
        data: formattedData
      }
    ]
  }
  myChart.setOption(option)
}
</script>

<style scoped>
.dashboard-container { padding: 0; }
.dashboard-header h2 { font-size: 20px; font-weight: 700; color: #111827; margin-bottom: 24px; margin-top: 0; }

/* 卡片通用 */
.stat-card, .chart-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid rgba(243, 244, 246, 0.6);
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.02);
  transition: box-shadow 0.3s ease;
}
.stat-card:hover, .chart-card:hover { box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05); }

/* 顶部统计 */
.stat-card { padding: 24px; display: flex; align-items: center; justify-content: space-between; height: 120px; box-sizing: border-box; }
.stat-content .label { font-size: 14px; color: #6B7280; margin-bottom: 8px; font-weight: 500; }
.stat-content .value { font-size: 28px; font-weight: 700; color: #111827; line-height: 1; }
.text-danger { color: #DC2626 !important; }
.stat-icon-bg { width: 56px; height: 56px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24px; }
.bg-green { background-color: #ECFDF5; color: #10B981; }
.bg-blue { background-color: #EFF6FF; color: #3B82F6; }
.bg-red { background-color: #FEF2F2; color: #EF4444; }
.bg-purple { background-color: #F3E8FF; color: #A855F7; }

/* 图表区域 */
.chart-card { padding: 24px; height: 420px; display: flex; flex-direction: column; }
.chart-header { font-size: 16px; font-weight: 600; color: #1F2937; margin-bottom: 20px; }
.chart-body { flex: 1; display: flex; align-items: center; justify-content: center; }

/* --- 底部预警区域 (核心样式) --- */
.alert-section {
  margin-top: 24px;
  background-color: #FFF5F5; /* 淡红色背景 */
  border: 1px solid #FECACA; /* 淡红色边框 */
  border-radius: 16px;
  padding: 24px;
}
.alert-header {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #991B1B; /* 深红色标题 */
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 20px;
}
.alert-item {
  background: #ffffff;
  border: 1px solid #FECACA;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 2px rgba(0,0,0,0.02);
  margin-bottom: 12px; /* 防止循环时连在一起 */
}
.alert-info .name { font-weight: 600; font-size: 15px; color: #1F2937; }
.alert-info .code { font-size: 13px; color: #6B7280; margin-top: 4px; }
.alert-stat { text-align: right; }
.alert-stat .stock { font-size: 20px; font-weight: 700; color: #DC2626; }
.alert-stat .unit { font-size: 14px; font-weight: 500; color: #EF4444; }
.alert-stat .tag { font-size: 12px; color: #F87171; margin-top: 2px; font-weight: 500; }
</style>
