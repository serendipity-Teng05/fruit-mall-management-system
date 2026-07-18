<template>
  <div>
    <el-table :data="userRoles" v-loading="loading" style="width: 100%">
      <el-table-column prop="username" label="用户名"/>
      <el-table-column prop="roleNames" label="已分配角色"/>
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button type="primary" @click="openAssign(scope.row)">分配角色</el-button>
        </template>
      </el-table-column>
    </el-table>

    <role-form v-model:visible="assignDialogVisible" :roleData="currentUser" @saved="fetchUserRoles"/>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAllUserRoles } from '@/api/userRole'
import RoleForm from '@/components/roles/RoleForm.vue'

const assignDialogVisible = ref(false)
const currentUser = ref(null)
const userRoles = ref([])
const loading = ref(false)

const fetchUserRoles = async () => {
  loading.value = true
  const res = await getAllUserRoles({ pageNum:1, pageSize:100 })
  userRoles.value = res.data.records || []
  loading.value = false
}

onMounted(fetchUserRoles)

const openAssign = (user) => {
  currentUser.value = user
  assignDialogVisible.value = true
}
</script>

<style scoped>
.el-table { width: 100%; }
.el-button { margin-right: 8px; }
</style>