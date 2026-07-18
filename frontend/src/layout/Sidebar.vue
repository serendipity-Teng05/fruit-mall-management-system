<template>
  <div class="sidebar-container">
    <div class="logo-container">
      <div class="logo-bg">
        <el-icon><Apple /></el-icon>
      </div>
      <div class="brand-copy">
        <strong>鲜果集管理台</strong>
        <span>商城运营与支付管理</span>
      </div>
    </div>

    <el-menu
      active-text-color="#06723a"
      background-color="#ffffff"
      class="el-menu-vertical"
      :default-active="activeMenu"
      text-color="#4f5b54"
      router
    >
      <el-menu-item-group title="运营中心">
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>数据概览</span>
        </el-menu-item>

        <el-menu-item v-if="hasPermission('PRODUCT_MANAGE')" index="/product">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>

        <el-menu-item v-if="hasPermission('ORDER_MANAGE')" index="/order">
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </el-menu-item>

        <el-menu-item v-if="hasPermission('PAYMENT_MANAGE')" index="/payment">
          <el-icon><CreditCard /></el-icon>
          <span>支付流水</span>
        </el-menu-item>
      </el-menu-item-group>

      <el-menu-item-group v-if="canManageUsers" title="组织与权限">
        <el-menu-item v-if="hasPermission('USER_MANAGE')" index="/user">
          <el-icon><User /></el-icon>
          <span>人员管理</span>
        </el-menu-item>

        <el-menu-item v-if="hasPermission('ROLE_MANAGE')" index="/roles/role-management">
          <el-icon><Key /></el-icon>
          <span>角色管理</span>
        </el-menu-item>

        <el-menu-item v-if="hasPermission('ROLE_PERMISSION')" index="/roles/role-permission">
          <el-icon><Lock /></el-icon>
          <span>角色权限</span>
        </el-menu-item>

        <el-menu-item v-if="hasPermission('USER_ROLE_ASSIGN')" index="/users/user-role-assignment">
          <el-icon><Avatar /></el-icon>
          <span>用户角色分配</span>
        </el-menu-item>
      </el-menu-item-group>

      <el-menu-item-group v-if="canViewLogs" title="审计与日志">
        <el-menu-item v-if="hasPermission('ORDER_LOG')" index="/orders/order-log">
          <el-icon><Document /></el-icon>
          <span>订单日志</span>
        </el-menu-item>

        <el-menu-item v-if="hasPermission('SYS_LOG')" index="/system/sys-log">
          <el-icon><Setting /></el-icon>
          <span>系统日志</span>
        </el-menu-item>
      </el-menu-item-group>
    </el-menu>

    <div class="sidebar-footer">
      <div class="user-info">
        <div class="avatar-circle">
          <el-icon><UserFilled /></el-icon>
        </div>
        <div class="user-details">
          <div class="user-name" :title="displayName">{{ displayName }}</div>
          <div class="user-role" :title="userRole">{{ userRole }}</div>
        </div>
        <el-tooltip content="修改登录密码" placement="top">
          <button type="button" class="security-button" aria-label="修改登录密码" @click="openPasswordDialog">
            <el-icon><Lock /></el-icon>
          </button>
        </el-tooltip>
      </div>

      <button type="button" class="logout-btn" @click="handleLogout">
        <el-icon><SwitchButton /></el-icon>
        <span>退出登录</span>
      </button>
    </div>

    <el-dialog
      v-model="passwordDialogVisible"
      title="修改登录密码"
      width="min(420px, calc(100vw - 32px))"
      append-to-body
      align-center
    >
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="88px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password maxlength="72" placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password maxlength="72" placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password maxlength="72" placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdatePassword">确定修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Apple,
  Avatar,
  CreditCard,
  Document,
  Goods,
  Key,
  List,
  Lock,
  Odometer,
  Setting,
  SwitchButton,
  User,
  UserFilled
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { logout as logoutApi } from '@/api/auth'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const passwordDialogVisible = ref(false)
const passwordFormRef = ref(null)

const user = computed(() => userStore.user || {})
const displayName = computed(() => user.value.name || user.value.realName || user.value.username || '用户')
const userRoles = computed(() => user.value.roles || user.value.user?.roles || [])
const userRole = computed(() => {
  const names = userRoles.value.map(role => role.roleName).filter(Boolean)
  return names.length ? names.join('、') : '未分配角色'
})

const hasPermission = code => userStore.permissions.includes(code)
const canManageUsers = computed(() => ['USER_MANAGE', 'ROLE_MANAGE', 'ROLE_PERMISSION', 'USER_ROLE_ASSIGN'].some(hasPermission))
const canViewLogs = computed(() => ['ORDER_LOG', 'SYS_LOG'].some(hasPermission))
const activeMenu = computed(() => route.path)

const handleLogout = async () => {
  try {
    await logoutApi()
  } catch (_) {
    // 服务端不可用时仍允许清理本地登录状态。
  }
  userStore.logout()
  sessionStorage.removeItem('adminReturnPath')
  router.replace('/login')
}

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) callback(new Error('两次输入的密码不一致'))
  else callback()
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 72, message: '密码长度应为 8–72 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const openPasswordDialog = () => {
  Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
  passwordDialogVisible.value = true
}

const handleUpdatePassword = async () => {
  if (!passwordFormRef.value) return
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  const res = await request.put('/sys_user/password', {
    oldPassword: passwordForm.oldPassword,
    newPassword: passwordForm.newPassword
  })
  if (String(res.code) === '200') {
    ElMessage.success('修改成功，请重新登录')
    passwordDialogVisible.value = false
    userStore.logout()
    sessionStorage.removeItem('adminReturnPath')
    router.replace('/login')
  }
}
</script>

<style scoped>
.sidebar-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.logo-container {
  min-height: 72px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 18px;
  border-bottom: 1px solid #edf1ee;
}

.logo-bg {
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  display: grid;
  place-items: center;
  border-radius: 11px;
  background: linear-gradient(145deg, #0caf5b, #078c47);
  box-shadow: 0 8px 18px rgba(7, 148, 71, 0.2);
  color: #fff;
  font-size: 23px;
}

.brand-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.brand-copy strong {
  overflow: hidden;
  color: #17221c;
  font-size: 16px;
  line-height: 1.15;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-copy span {
  color: #96a099;
  font-size: 10px;
  letter-spacing: 0.04em;
}

.el-menu-vertical {
  flex: 1;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 12px 10px 18px;
  border-right: 0;
}

.el-menu-vertical::-webkit-scrollbar { width: 4px; }
.el-menu-vertical::-webkit-scrollbar-thumb { border-radius: 4px; background: #dce6df; }

:deep(.el-menu-item-group__title) {
  padding: 14px 13px 7px !important;
  color: #a0aaa4;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.08em;
}

:deep(.el-menu-item) {
  height: 44px;
  margin: 3px 0;
  padding: 0 13px !important;
  border-radius: 11px;
  font-size: 14px;
  transition: color 0.2s ease, background 0.2s ease, transform 0.2s ease;
}

:deep(.el-menu-item .el-icon) {
  margin-right: 11px;
  font-size: 18px;
}

:deep(.el-menu-item:hover) {
  background: #f3f7f4 !important;
  color: #173d28 !important;
  transform: translateX(1px);
}

:deep(.el-menu-item.is-active) {
  background: #eaf7ef !important;
  color: #06723a !important;
  font-weight: 700;
  box-shadow: inset 3px 0 0 #079447;
}

.sidebar-footer {
  padding: 14px;
  border-top: 1px solid #edf1ee;
  background: #fff;
}

.user-info {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) 30px;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid #e8eeea;
  border-radius: 13px;
  background: #f8faf9;
}

.avatar-circle {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 11px;
  background: #dff3e7;
  color: #078744;
  font-size: 18px;
}

.user-details { min-width: 0; }
.user-name, .user-role { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.user-name { color: #26322b; font-size: 13px; font-weight: 700; }
.user-role { margin-top: 4px; color: #8b958f; font-size: 11px; }

.security-button {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #8b958f;
  cursor: pointer;
}

.security-button:hover { background: #eaf7ef; color: #079447; }

.logout-btn {
  width: 100%;
  min-height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 9px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: #d64a4a;
  cursor: pointer;
  font-size: 13px;
}

.logout-btn:hover { background: #fff1f1; }

@media (max-height: 760px) {
  :deep(.el-menu-item-group__title) { padding-top: 10px !important; }
  :deep(.el-menu-item) { height: 40px; }
  .sidebar-footer { padding: 10px 14px; }
}
</style>
