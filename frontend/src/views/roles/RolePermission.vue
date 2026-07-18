<template>
  <div class="page-container">
    <div class="main-card">
      <div class="toolbar">
        <div class="search-area">
          <el-select
            v-model="selectedRoleId"
            placeholder="请选择角色"
            style="width: 260px"
            clearable
            @change="handleRoleChange"
          >
            <el-option
              v-for="item in roleList"
              :key="item.roleId"
              :label="item.roleName"
              :value="item.roleId"
            />
          </el-select>

          <el-button type="success" plain @click="refreshAllData">
            <el-icon style="margin-right: 5px"><Refresh /></el-icon>
            刷新权限
          </el-button>
        </div>

        <el-button type="primary" :disabled="!selectedRoleId || permissionLocked" @click="handleSave">
          <el-icon style="margin-right: 5px"><Check /></el-icon>
          保存权限分配
        </el-button>
      </div>

      <div class="content-wrap" v-loading="loading">
        <div class="role-info" v-if="currentRole">
          <div class="role-title">当前角色：{{ currentRole.roleName }}</div>
          <div class="role-desc">{{ currentRole.roleDesc || '暂无描述' }}</div>
        </div>
        <el-alert v-if="permissionLocked" :title="lockMessage" type="warning" :closable="false" show-icon style="margin-bottom:16px" />

        <div v-if="!selectedRoleId" class="empty-box">
          <el-empty description="请先选择角色" />
        </div>

        <div v-else class="permission-panel">
          <el-tree
            ref="treeRef"
            :data="permissionTree"
            node-key="permissionId"
            show-checkbox
            :disabled="permissionLocked"
            default-expand-all
            :props="defaultProps"
            class="permission-tree"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import { Refresh, Check } from '@element-plus/icons-vue'

const loading = ref(false)
const roleList = ref([])
const permissionList = ref([])
const permissionTree = ref([])
const selectedRoleId = ref(null)
const treeRef = ref(null)
const userStore = useUserStore()

const defaultProps = {
  children: 'children',
  label: 'permissionName'
}

const currentRole = computed(() => {
  return roleList.value.find(item => item.roleId === selectedRoleId.value) || null
})
const permissionLocked = computed(() => currentRole.value?.roleCode === 'CUSTOMER' || (currentRole.value?.roleCode === 'ADMIN' && userStore.user?.username !== 'admin'))
const lockMessage = computed(() => currentRole.value?.roleCode === 'CUSTOMER' ? '顾客权限由订单和地址的数据归属校验控制，不分配后台权限。' : '只有超级管理员 admin 可以修改 ADMIN 角色权限。')

onMounted(async () => {
  await refreshAllData()
})

const refreshAllData = async () => {
  loading.value = true
  try {
    await Promise.all([fetchRoleData(), fetchPermissionData()])

    await nextTick()

    if (selectedRoleId.value) {
      await fetchRolePermissions(selectedRoleId.value)
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const fetchRoleData = async () => {
  try {
    const res = await request.get('/api/sys_role_permission/roles')
    roleList.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (error) {
    console.error(error)
    roleList.value = []
    ElMessage.error('获取角色列表失败')
  }
}

const fetchPermissionData = async () => {
  try {
    const res = await request.get('/api/sys_role_permission/permissions')
    permissionList.value = Array.isArray(res) ? res : (res?.data || [])
    permissionTree.value = buildPermissionTree(permissionList.value)
  } catch (error) {
    console.error(error)
    permissionList.value = []
    permissionTree.value = []
    ElMessage.error('获取权限列表失败')
  }
}

const fetchRolePermissions = async (roleId) => {
  try {
    const res = await request.get(`/api/sys_role_permission/role/${roleId}`)
    const relationList = Array.isArray(res) ? res : (res?.data || [])
    const checkedIds = relationList.map(item => item.permissionId)

    await nextTick()
    treeRef.value?.setCheckedKeys(checkedIds)
  } catch (error) {
    console.error(error)
    ElMessage.error('获取角色权限失败')
  }
}

const handleRoleChange = async (roleId) => {
  if (!roleId) {
    treeRef.value?.setCheckedKeys([])
    return
  }
  await fetchRolePermissions(roleId)
}

const handleSave = async () => {
  if (!selectedRoleId.value) {
    ElMessage.warning('请先选择角色')
    return
  }

  try {
    const checkedKeys = treeRef.value?.getCheckedKeys() || []

    await request.post('/api/sys_role_permission/assign', {
      roleId: selectedRoleId.value,
      permissionIds: checkedKeys
    })

    ElMessage.success('权限分配成功')
    await fetchRolePermissions(selectedRoleId.value)
  } catch (error) {
    console.error(error)
    ElMessage.error('权限分配失败')
  }
}

const buildPermissionTree = (list) => {
  const map = {}
  const tree = []

  list.forEach(item => {
    map[item.permissionId] = {
      ...item,
      children: []
    }
  })

  list.forEach(item => {
    const parentId = item.parentId
    if (parentId && map[parentId]) {
      map[parentId].children.push(map[item.permissionId])
    } else {
      tree.push(map[item.permissionId])
    }
  })

  return tree
}
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.main-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-sizing: border-box;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.search-area {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.content-wrap {
  min-height: 500px;
}

.role-info {
  margin-bottom: 20px;
  padding: 14px 16px;
  border-radius: 8px;
  background: #f9fafb;
  border: 1px solid #eef2f7;
}

.role-title {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
}

.role-desc {
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.empty-box {
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.permission-panel {
  min-height: 420px;
  padding: 20px 16px;
  border-radius: 10px;
  background: #fcfcfd;
  border: 1px solid #eef2f7;
  box-sizing: border-box;
}

.permission-tree {
  width: 100%;
}

.permission-tree :deep(.el-tree-node__content) {
  height: 38px;
  border-radius: 6px;
}

.permission-tree :deep(.el-tree-node__content:hover) {
  background-color: #f5f7fa;
}

.permission-tree :deep(.el-checkbox) {
  margin-right: 8px;
}
</style>
