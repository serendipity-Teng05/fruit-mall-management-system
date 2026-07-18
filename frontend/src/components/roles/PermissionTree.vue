<template>
  <el-tree
    :data="permissions"
    :props="defaultProps"
    show-checkbox
    node-key="id"
    default-expand-all
    ref="treeRef"
  ></el-tree>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getPermissionList } from '@/api/permission'

const props = defineProps({
  roleId: Number,
  assignedIds: Array
})
const treeRef = ref(null)
const permissions = ref([])
const defaultProps = { children: 'children', label: 'name' }

const fetchPermissions = async () => {
  const res = await getPermissionList()
  permissions.value = res.data || []
}

watch(() => props.roleId, () => fetchPermissions(), { immediate: true })
const getCheckedIds = () => treeRef.value.getCheckedKeys()
</script>

<style scoped>
.el-tree { max-height: 400px; overflow-y: auto; }
</style>