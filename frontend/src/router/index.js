import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/layout/index.vue'
import MallLayout from '@/layout/MallLayout.vue'
import { getInfo } from '@/api/auth'
import { useUserStore } from '@/store/user'

const adminChildren = [
  { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '数据概览', description: '查看商城经营、订单与库存核心数据', admin: true } },
  { path: 'product', name: 'Product', component: () => import('@/views/product/index.vue'), meta: { title: '商品管理', description: '维护商品档案、库存和上下架状态', permission: 'PRODUCT_MANAGE' } },
  { path: 'order', name: 'Order', component: () => import('@/views/order/index.vue'), meta: { title: '订单管理', description: '处理订单状态与履约流程', permission: 'ORDER_MANAGE' } },
  { path: 'payment', name: 'Payment', component: () => import('@/views/payment/index.vue'), meta: { title: '支付流水', description: '核对支付渠道与交易状态', permission: 'PAYMENT_MANAGE' } },
  { path: 'user', name: 'User', component: () => import('@/views/user/index.vue'), meta: { title: '人员管理', description: '维护员工账号与在职信息', permission: 'USER_MANAGE' } },
  { path: 'roles/role-management', name: 'RoleManagement', component: () => import('@/views/roles/RoleManagement.vue'), meta: { title: '角色管理', description: '配置系统角色与职责范围', permission: 'ROLE_MANAGE' } },
  { path: 'roles/role-permission', name: 'RolePermission', component: () => import('@/views/roles/RolePermission.vue'), meta: { title: '角色权限', description: '按角色分配后台功能权限', permission: 'ROLE_PERMISSION' } },
  { path: 'users/user-role-assignment', name: 'UserRoleAssignment', component: () => import('@/views/users/UserRoleAssignment.vue'), meta: { title: '用户角色分配', description: '协调用户、角色和权限关系', permission: 'USER_ROLE_ASSIGN' } },
  { path: 'orders/order-log', name: 'OrderLog', component: () => import('@/views/orders/OrderLog.vue'), meta: { title: '订单日志', description: '追踪订单状态变更与操作记录', permission: 'ORDER_LOG' } },
  { path: 'system/sys-log', name: 'SysLog', component: () => import('@/views/system/SysLog.vue'), meta: { title: '系统日志', description: '查看关键系统操作与异常记录', permission: 'SYS_LOG' } }
]

const routes = [
  { path: '/', redirect: '/mall' },
  { path: '/login', name: 'Login', component: () => import('@/views/login/index.vue'), meta: { public: true } },
  { path: '/register', name: 'Register', component: () => import('@/views/register/index.vue'), meta: { public: true } },
  {
    path: '/mall',
    component: MallLayout,
    children: [
      { path: '', name: 'MallHome', component: () => import('@/views/mall/Home.vue'), meta: { public: true } },
      { path: 'products/:id', name: 'MallProductDetail', component: () => import('@/views/mall/ProductDetail.vue'), meta: { public: true } },
      { path: 'cart', name: 'MallCart', component: () => import('@/views/mall/Cart.vue'), meta: { public: true } },
      { path: 'checkout', name: 'MallCheckout', component: () => import('@/views/mall/Checkout.vue'), meta: { requiresAuth: true } },
      { path: 'orders', name: 'MallOrders', component: () => import('@/views/mall/MyOrders.vue'), meta: { requiresAuth: true } },
      { path: 'addresses', name: 'MallAddresses', component: () => import('@/views/mall/Addresses.vue'), meta: { requiresAuth: true } }
    ]
  },
  {
    path: '/',
    component: AdminLayout,
    children: adminChildren
  },
  { path: '/403', name: 'Forbidden', component: () => import('@/views/error/403.vue') },
  { path: '/:pathMatch(.*)*', redirect: '/mall' }
]

const router = createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })

router.beforeEach(async to => {
  const token = sessionStorage.getItem('token')
  const hasToken = Boolean(token && token !== 'undefined' && token !== 'null')
  let permissionCodes = []
  try { permissionCodes = JSON.parse(sessionStorage.getItem('permissionCodes') || '[]') } catch { permissionCodes = [] }

  if (to.path === '/login' && hasToken) {
    const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : ''
    return redirect || (permissionCodes.length ? '/dashboard' : '/mall')
  }
  if (to.meta.public) return true
  if (!hasToken) return { path: '/login', query: { redirect: to.fullPath } }

  try {
    const session = await getInfo()
    permissionCodes = session.data?.permissionCodes || []
    useUserStore().login(session.data?.user || {}, token, permissionCodes)
  } catch {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const needPermission = to.meta.permission
  if (needPermission && !permissionCodes.includes(needPermission)) return '/403'
  if (to.meta.admin && permissionCodes.length === 0) return '/403'
  return true
})

export default router
