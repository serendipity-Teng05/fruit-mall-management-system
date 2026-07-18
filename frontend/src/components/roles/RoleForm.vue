<template>
  <el-dialog :title="role.id ? '编辑角色' : '新增角色'" :visible.sync="visible" width="450px">
    <el-form ref="formRef" :model="role" label-width="80px" :rules="rules">
      <el-form-item label="角色名称" prop="name">
        <el-input v-model="role.name" placeholder="请输入角色名称"/>
      </el-form-item>
      <el-form-item label="角色描述" prop="description">
        <el-input v-model="role.description" placeholder="请输入描述"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { saveRole } from '@/api/role'

const props = defineProps({
  visible: Boolean,
  roleData: Object
})

const emit = defineEmits(['update:visible', 'saved'])
const role = ref({ id: null, name: '', description: '' })
const formRef = ref(null)

const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

watch(() => props.roleData, (val) => {
  if(val) role.value = { ...val }
  else role.value = { id: null, name: '', description: '' }
}, { immediate: true })

const submit = async () => {
  await formRef.value.validate(async valid => {
    if(valid){
      await saveRole(role.value)
      ElMessage.success('保存成功')
      emit('saved')
      emit('update:visible', false)
    }
  })
}
</script>

<style scoped>
.el-dialog { max-width: 450px; }
.el-form-item { margin-bottom: 15px; }
.el-button { margin-right: 10px; }
</style>

