<template>
  <div class="page-container">
    <el-row :gutter="24">
      <el-col :span="8">
        <div class="user-card form-card">
          <div class="card-header">
            <div class="icon-bg">
              <el-icon><Plus /></el-icon>
            </div>
            <h3>添加新人员</h3>
          </div>
          <el-form label-position="top" class="user-form">
            <el-form-item label="姓名">
              <el-input v-model="addForm.realName" placeholder="请输入姓名" />
            </el-form-item>
            
            <el-form-item label="用户名 (账号)">
              <el-input v-model="addForm.username" placeholder="请输入登录账号" />
            </el-form-item>

            <el-form-item label="手机号">
              <el-input v-model="addForm.phone" placeholder="请输入手机号" />
            </el-form-item>

            <el-form-item label="初始密码">
              <el-input v-model="addForm.password" type="password" show-password maxlength="72" placeholder="8-72 位，不再使用统一默认密码" />
            </el-form-item>
            
            <el-form-item label="角色权限">
              <el-select v-model="addForm.roleIds" multiple collapse-tags placeholder="请选择角色" style="width: 100%">
                <el-option v-for="role in roleOptions" :key="role.roleId" :label="role.roleName" :value="role.roleId" />
              </el-select>
            </el-form-item>
            
            <el-button type="primary" class="submit-btn" @click="handleAdd">立即创建</el-button>
          </el-form>
        </div>
      </el-col>

      <el-col :span="16">
        <div class="user-card list-card">
          <div class="card-header border-bottom">
            <h3>在职人员列表</h3>
            <span class="count-badge">{{ userList.length }} 人</span>
          </div>
          <el-table 
            v-loading="loading"
            :data="userList" 
            :header-cell-style="{ background: '#F9FAFB', color: '#374151', fontWeight: '600' }"
            style="margin-top: 12px"
          >
            <el-table-column prop="userNo" label="人员编号" width="130" />
            <el-table-column prop="realName" label="姓名">
              <template #default="scope">
                <span style="font-weight: 500; color: #111827;">{{ scope.row.realName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="username" label="账号">
              <template #default="scope">
                <span style="color: #6B7280;">@{{ scope.row.username }}</span>
              </template>
            </el-table-column>
            <el-table-column label="角色" min-width="160">
               <template #default="scope">
                 <div class="role-tags">
                   <el-tag v-for="role in scope.row.roles || []" :key="role.roleId" size="small" effect="light">
                     {{ role.roleName }}
                   </el-tag>
                   <span v-if="!(scope.row.roles || []).length" class="empty-text">未分配</span>
                 </div>
               </template>
            </el-table-column>
            <el-table-column prop="phone" label="联系方式" />
            <el-table-column label="状态" width="90"><template #default="scope"><el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">{{ scope.row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
            
            <el-table-column label="操作" width="180">
               <template #default="scope">
                 <el-button type="primary" link size="small" @click="handleEdit(scope.row)">
                   编辑
                 </el-button>
                 <el-button v-if="scope.row.status === 1" type="danger" link size="small" @click="handleDelete(scope.row)">
                   停用
                 </el-button>
                 <el-button v-else type="success" link size="small" @click="handleEnable(scope.row)">启用</el-button>
               </template>
             </el-table-column>

          </el-table>
          <div class="pagination-container">
            <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" :total="total" @size-change="getList" @current-change="getList" />
          </div>
        </div>
      </el-col>
    </el-row>

    <el-dialog title="编辑员工信息" v-model="editDialogVisible" width="400px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="账号">
          <el-input v-model="editForm.username" disabled placeholder="账号不可修改"></el-input>
        </el-form-item>
        
        <el-form-item label="姓名">
          <el-input v-model="editForm.realName"></el-input>
        </el-form-item>
        
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone"></el-input>
        </el-form-item>
        
        <el-form-item label="角色">
          <el-select v-model="editForm.roleIds" multiple collapse-tags style="width: 100%">
             <el-option v-for="role in roleOptions" :key="role.roleId" :label="role.roleName" :value="role.roleId" />
          </el-select>
        </el-form-item>

        <el-divider content-position="center">安全设置</el-divider>

        <el-form-item label="重置密码">
          <el-input 
            v-model="editForm.password" 
            type="password" 
            show-password 
            placeholder="不修改请留空"
          ></el-input>
        </el-form-item>

      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveEdit">保存修改</el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
// 确保你的 api/user.js 里面有 saveUser 方法 (通常 save 既能新增也能更新)
import { changeUserStatus, getUserList, saveUser, deleteUser } from '@/api/user'
import { getRoleList } from '@/api/role'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue' // 确保引入了图标

// === 1. 数据变量 ===
const userList = ref([])
const roleOptions = ref([])
const loading = ref(false)
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })

// 左侧“添加”用的表单
const addForm = reactive({
  realName: '',      
  username: '',  
  phone: '',     
  password: '',
  roleIds: []       
})

// ✨ 弹窗“编辑”用的表单 (独立开来，防止冲突)
const editDialogVisible = ref(false)
const editForm = reactive({
  id: null,
  realName: '',
  username: '',
  phone: '',
  roleIds: [],
  password: '' // 专门用于重置密码
})

// === 2. 获取列表 ===
const getList = async () => {
  loading.value = true
  try {
    const res = await getUserList({ ...query, includeDisabled: true })
    userList.value = res.data?.records || res.data || []
    total.value = Number(res.data?.total || userList.value.length)
  } finally {
    loading.value = false
  }
}

const getRoles = async () => {
  const res = await getRoleList()
  roleOptions.value = Array.isArray(res) ? res : (res?.data || [])
}

onMounted(async () => {
  await Promise.all([getRoles(), getList()])
})

// === 3. 提交添加 (左侧) ===
const handleAdd = async () => {
  if (!addForm.realName || !addForm.username || !addForm.password || addForm.password.length < 8 || addForm.password.length > 72 || !addForm.roleIds.length) {
    ElMessage.warning('请填写姓名、账号、8-72 位初始密码并选择角色')
    return
  }
  await saveUser(addForm)
  ElMessage.success('创建成功')
  getList()
  // 清空
  Object.assign(addForm, { realName: '', username: '', phone: '', password: '', roleIds: [] })
}

// === 4. 打开编辑弹窗 (点击列表里的编辑按钮) ===
const handleEdit = (row) => {
  // 把当前行的数据复制给 editForm
  editForm.id = row.id
  editForm.realName = row.realName
  editForm.username = row.username
  editForm.phone = row.phone
  editForm.roleIds = (row.roles || []).map(role => role.roleId)
  
  // 🌟 重要：密码字段默认置空，代表“不修改”
  editForm.password = ''
  
  editDialogVisible.value = true
}

// === 5. 保存编辑 (弹窗里的确定按钮) ===
const handleSaveEdit = async () => {
  // 调用同一个 saveUser 接口 (通常后端根据 id 是否存在来判断是新增还是更新)
  // 这里的 editForm 里包含了 id，所以后端会走 update 逻辑
  await saveUser(editForm)
  
  ElMessage.success('修改成功')
  editDialogVisible.value = false
  getList()
}

// === 6. 删除用户 ===
const handleDelete = (row) => {
  if (!row.id) {
    ElMessage.error('停用失败：缺少人员标识');
    return;
  }
  ElMessageBox.confirm(`确认停用人员“${row.realName || row.username}”吗？历史订单和日志会保留。`, '停用确认', { type: 'warning' })
    .then(async () => {
      await deleteUser(row.id)
      ElMessage.success('人员已停用')
      getList()
    })
}
const handleEnable = async row => { await changeUserStatus(row.id, 1); ElMessage.success('人员已启用'); getList() }
</script>

<style scoped>
.page-container { padding: 0; }
.user-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  border: 1px solid #F3F4F6;
  height: 100%;
}
.card-header { display: flex; align-items: center; margin-bottom: 24px; }
.card-header.border-bottom { border-bottom: 1px solid #F3F4F6; padding-bottom: 16px; margin-bottom: 0; }
.card-header h3 { margin: 0; font-size: 18px; color: #1F2937; font-weight: 600; }
.icon-bg {
  width: 36px; height: 36px; background-color: #ECFDF5; color: #10B981;
  border-radius: 8px; display: flex; align-items: center; justify-content: center;
  margin-right: 12px; font-size: 18px;
}
.count-badge {
  background-color: #F3F4F6; color: #6B7280; padding: 2px 10px;
  border-radius: 99px; font-size: 12px; margin-left: auto;
}
.user-form { margin-top: 8px; }
.submit-btn {
  width: 100%; height: 40px; border-radius: 8px; font-weight: 500; margin-top: 16px;
}
.role-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.empty-text { color: #9ca3af; font-size: 12px; }
.pagination-container { display: flex; justify-content: flex-end; margin-top: 18px; }
</style>
