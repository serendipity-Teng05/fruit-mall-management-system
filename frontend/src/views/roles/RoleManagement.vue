<template>
  <div class="page-container">
    <div class="main-card">
      <div class="toolbar">
        <div class="search-area">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索角色名称..."
            class="search-input"
            clearable
            @clear="handleFilter"
            @keyup.enter="handleFilter"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <el-button type="success" plain @click="handleFilter">
            <el-icon style="margin-right: 5px"><Search /></el-icon>
            查询
          </el-button>
        </div>

        <el-button type="success" class="add-btn" @click="handleCreate">
          <el-icon style="margin-right: 5px"><Plus /></el-icon>
          新增角色
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="pagedData"
        style="width: 100%"
        :header-cell-style="{ background: '#F9FAFB', color: '#374151', fontWeight: '600' }"
      >
        <el-table-column prop="roleCode" label="角色编码" width="150" />
        <el-table-column prop="roleName" label="角色名称" min-width="180" />
        <el-table-column prop="roleDesc" label="角色描述" min-width="260" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" align="center">
          <template #default="scope">
            <span class="time-text">{{ formatTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180" align="center">
          <template #default="scope">
            <span class="time-text">{{ formatTime(scope.row.updateTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" class="action-btn" @click="handleEdit(scope.row)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button v-if="!['ADMIN','CUSTOMER','STAFF'].includes(scope.row.roleCode)" link type="danger" class="action-btn" @click="handleDelete(scope.row)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="filteredData.length"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="form.roleId ? '编辑角色' : '新增角色'"
      width="520px"
      class="custom-dialog"
      align-center
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="custom-form">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="Boolean(form.roleId)" placeholder="例如 PURCHASER" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>

        <el-form-item label="角色描述" prop="roleDesc">
          <el-input
            v-model="form.roleDesc"
            type="textarea"
            :rows="4"
            placeholder="请输入角色描述"
          />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'

const loading = ref(false)
const rawData = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

const form = reactive({
  roleId: null,
  roleCode: '',
  roleName: '',
  roleDesc: ''
})

const rules = {
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]{2,49}$/, message: '使用3-50位大写字母、数字或下划线', trigger: 'blur' }
  ],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

onMounted(() => {
  fetchData()
})

const filteredData = computed(() => {
  const keyword = queryParams.keyword.trim().toLowerCase()
  if (!keyword) return rawData.value
  return rawData.value.filter(item => {
    const name = item.roleName || ''
    const desc = item.roleDesc || ''
    return name.toLowerCase().includes(keyword) || desc.toLowerCase().includes(keyword)
  })
})

const pagedData = computed(() => {
  const start = (queryParams.pageNum - 1) * queryParams.pageSize
  const end = start + queryParams.pageSize
  return filteredData.value.slice(start, end)
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/sys_role')
    rawData.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (error) {
    console.error(error)
    ElMessage.error('获取角色列表失败')
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  queryParams.pageNum = 1
}

const handleCreate = () => {
  Object.assign(form, {
    roleId: null,
    roleCode: '',
    roleName: '',
    roleDesc: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, {
    roleId: row.roleId,
    roleCode: row.roleCode,
    roleName: row.roleName,
    roleDesc: row.roleDesc
  })
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除角色“${row.roleName}”吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await request.delete(`/api/sys_role/${row.roleId}`)
      ElMessage.success('删除成功')
      fetchData()
    })
    .catch(() => {})
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      const payload = {
        roleId: form.roleId,
        roleCode: form.roleCode.trim().toUpperCase(),
        roleName: form.roleName,
        roleDesc: form.roleDesc
      }

      if (form.roleId) {
        await request.put('/api/sys_role', payload)
      } else {
        await request.post('/api/sys_role', payload)
      }

      ElMessage.success('保存成功')
      dialogVisible.value = false
      fetchData()
    } catch (error) {
      console.error(error)
      ElMessage.error('保存失败')
    }
  })
}

const handleSizeChange = (val) => {
  queryParams.pageSize = val
  queryParams.pageNum = 1
}

const handleCurrentChange = (val) => {
  queryParams.pageNum = val
}

const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  return String(timeStr).replace('T', ' ')
}
</script>

<style scoped>
.page-container {
  padding: 20px;
}
.main-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}
.search-area {
  display: flex;
  gap: 10px;
}
.search-input {
  width: 260px;
}
.time-text {
  font-size: 12px;
  color: #666;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
