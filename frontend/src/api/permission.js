// import request from '@/utils/request'

// export function getPermissionList() {
//   return request({ url: '/sys_permission', method: 'get' })
// }

// export function addPermission(data) {
//   return request({ url: '/sys_permission', method: 'post', data })
// }

// export function updatePermission(data) {
//   return request({ url: '/sys_permission', method: 'put', data })
// }

// export function deletePermission(permissionId) {
//   return request({ url: `/sys_permission/${permissionId}`, method: 'delete' })
// }


import request from '@/utils/request'

// 获取权限列表
export function getPermissionList() {
  return request({
    url: '/api/sys_permission',
    method: 'get'
  })
}

// 新增权限
export function addPermission(data) {
  return request({
    url: '/api/sys_permission',
    method: 'post',
    data
  })
}

// 编辑权限
export function updatePermission(data) {
  return request({
    url: '/api/sys_permission',
    method: 'put',
    data
  })
}

// 删除权限
export function deletePermission(permissionId) {
  return request({
    url: `/api/sys_permission/${permissionId}`,
    method: 'delete'
  })
}