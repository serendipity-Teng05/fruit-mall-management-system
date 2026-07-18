<template>
  <div class="page-container">
    <div class="main-card">
      <div class="toolbar">
        <div class="search-area">
          <el-input
            v-model="queryParams.username"
            placeholder="搜索用户名..."
            class="search-input"
            clearable
            @clear="fetchUserData"
            @keyup.enter="fetchUserData"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <el-button type="success" plain @click="fetchUserData">
            <el-icon style="margin-right: 5px"><Search /></el-icon>
            查询
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
        :header-cell-style="{ background: '#F9FAFB', color: '#374151', fontWeight: '600' }"
      >
        <el-table-column prop="userNo" label="人员编号" width="140" />
        <el-table-column prop="username" label="用户名" min-width="160" />
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="160" />
        <el-table-column label="当前角色" min-width="260">
          <template #default="scope">
            <div class="role-tags">
              <el-tag
                v-for="role in scope.row.roles || []"
                :key="role.roleId"
                size="small"
                effect="light"
              >
                {{ role.roleName }}
              </el-tag>
              <span v-if="!(scope.row.roles || []).length" class="empty-text">未分配</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" class="action-btn" @click="handleAssign(scope.row)">
              <el-icon><Edit /></el-icon>
              分配角色
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
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      title="分配角色"
      width="520px"
      class="custom-dialog"
      align-center
    >
      <div class="dialog-user-info">
        当前用户：<span class="highlight">{{ currentUser.username || '-' }}</span>
      </div>

      <el-checkbox-group v-model="checkedRoleIds" class="role-checkbox-group">
        <el-checkbox
          v-for="role in allRoles"
          :key="role.roleId"
          :label="role.roleId"
          :disabled="roleDisabled(role)"
          class="role-checkbox"
        >
          {{ role.roleName }}
        </el-checkbox>
      </el-checkbox-group>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveRoles">确认保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import { Search, Edit } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)

const dialogVisible = ref(false)
const currentUser = ref({})
const allRoles = ref([])
const checkedRoleIds = ref([])
const userStore = useUserStore()
const roleDisabled = role => userStore.user?.username !== 'admin' && (role.roleCode === 'ADMIN' || currentUser.value.username === 'admin')

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  username: ''
})

onMounted(async () => {
  await fetchAllRoles()
  await fetchUserData()
})

const fetchAllRoles = async () => {
  try {
    const res = await request.get('/api/sys_user_role/roles')
    allRoles.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (error) {
    console.error(error)
    ElMessage.error('获取角色列表失败')
  }
}

const fetchUserData = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/sys_user_role/users', { params: { ...queryParams, keyword: queryParams.username } })
    if (res.code === 200 || res.code === '200') {
      const records = res.data?.records || res.data || []
      total.value = res.data?.total || records.length

      tableData.value = records
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleAssign = async (row) => {
  currentUser.value = { ...row }
  checkedRoleIds.value = (row.roles || []).map(item => item.roleId)
  dialogVisible.value = true
}

const handleSaveRoles = async () => {
  try {
    await request.post('/api/sys_user_role/assign', {
      userId: currentUser.value.id,
      roleIds: checkedRoleIds.value
    })

    ElMessage.success('角色分配成功')
    dialogVisible.value = false
    fetchUserData()
  } catch (error) {
    console.error(error)
    ElMessage.error('角色分配失败')
  }
}

const handleSizeChange = (val) => {
  queryParams.pageSize = val
  fetchUserData()
}

const handleCurrentChange = (val) => {
  queryParams.pageNum = val
  fetchUserData()
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
.role-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.empty-text {
  color: #999;
  font-size: 13px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.dialog-user-info {
  margin-bottom: 18px;
  color: #606266;
}
.highlight {
  color: #10b981;
  font-weight: 600;
}
.role-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
}
.role-checkbox {
  min-width: 100px;
}
</style>
