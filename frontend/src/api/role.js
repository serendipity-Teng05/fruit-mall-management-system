// import request from '@/utils/request'

// export function getRoleList() {
//   return request({ url: '/sys_role', method: 'get' })
// }

// export function addRole(data) {
//   return request({ url: '/sys_role', method: 'post', data })
// }

// export function updateRole(data) {
//   return request({ url: '/sys_role', method: 'put', data })
// }

// export function deleteRole(roleId) {
//   return request({ url: `/sys_role/${roleId}`, method: 'delete' })
// }

import request from '@/utils/request'

// 获取角色列表
export function getRoleList() {
  return request({
    url: '/api/sys_role/options',
    method: 'get'
  })
}

// 新增角色
export function addRole(data) {
  return request({
    url: '/api/sys_role',
    method: 'post',
    data
  })
}

// 编辑角色
export function updateRole(data) {
  return request({
    url: '/api/sys_role',
    method: 'put',
    data
  })
}

// 删除角色
export function deleteRole(roleId) {
  return request({
    url: `/api/sys_role/${roleId}`,
    method: 'delete'
  })
}
