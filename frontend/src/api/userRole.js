// import request from '@/utils/request'

// export function getUserRoleList(userId) {
//   return request({ url: `/sys_user_role/${userId}`, method: 'get' })
// }

// export function assignRoles(data) {
//   return request({ url: '/sys_user_role', method: 'post', data })
// }


import request from '@/utils/request'

// 获取某个用户的角色关联列表
export function getUserRoles(userId) {
  return request({
    url: `/api/sys_user_role/user/${userId}`,
    method: 'get'
  })
}

// 新增单条用户角色关系
export function addUserRole(data) {
  return request({
    url: '/api/sys_user_role',
    method: 'post',
    data
  })
}

// 删除单条用户角色关系
export function deleteUserRole(id) {
  return request({
    url: `/api/sys_user_role/${id}`,
    method: 'delete'
  })
}

// 批量分配用户角色
export function assignRolesToUser(data) {
  return request({
    url: '/api/sys_user_role/assign',
    method: 'post',
    data
  })
}