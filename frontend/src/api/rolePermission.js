// import request from '@/utils/request'

// export function getRolePermissionList(roleId) {
//   return request({ url: `/sys_role_permission/${roleId}`, method: 'get' })
// }

// export function assignPermissions(data) {
//   return request({ url: '/sys_role_permission', method: 'post', data })
// }



import request from '@/utils/request'

// 获取某个角色的权限关联列表
export function getRolePermissions(roleId) {
  return request({
    url: `/api/sys_role_permission/role/${roleId}`,
    method: 'get'
  })
}

// 新增单条角色权限关系
export function addRolePermission(data) {
  return request({
    url: '/api/sys_role_permission',
    method: 'post',
    data
  })
}

// 删除单条角色权限关系
export function deleteRolePermission(id) {
  return request({
    url: `/api/sys_role_permission/${id}`,
    method: 'delete'
  })
}

// 批量分配角色权限
export function assignPermissionsToRole(data) {
  return request({
    url: '/api/sys_role_permission/assign',
    method: 'post',
    data
  })
}

// 兼容旧调用名
export const assignPermissions = assignPermissionsToRole