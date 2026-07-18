// import request from '@/utils/request'

// export function getUserList(params) {
//   return request({ url: '/sys_user/list', method: 'get', params })
// }

// export function addUser(data) {
//   return request({ url: '/sys_user', method: 'post', data })
// }

// export function updateUser(data) {
//   return request({ url: '/sys_user', method: 'put', data })
// }

// export function deleteUser(userId) {
//   return request({ url: `/sys_user/${userId}`, method: 'delete' })
// }

import request from '@/utils/request'

// // ==========================================
// // 1. 登录相关接口 (对应 AuthController)
// // ==========================================

// // 登录
// export function login(data) {
//   // 对应后端 AuthController 的 /auth/login 接口
//   return request({
//     url: '/auth/login', 
//     method: 'post',
//     data
//   })
// }

// // 获取当前用户信息 (可选，取决于你后端有没有写 /auth/info)
// export function getInfo() {
//   return request({
//     url: '/auth/info',
//     method: 'get'
//   })
// }

// // 退出登录 (可选)
// export function logout() {
//   return request({
//     url: '/auth/logout',
//     method: 'post'
//   })
// }

// ==========================================
// 2. 用户管理接口 (对应 UserController)
// ==========================================

// 获取用户列表 (支持分页和搜索)
export function getUserList(params) {
  return request({
    url: '/sys_user/list',
    method: 'get',
    params
  })
}

// 保存用户 (新增或修改)
export function saveUser(data) {
  return request({
    url: '/sys_user/save',
    method: 'post',
    data
  })
}

// 删除用户
export function deleteUser(id) {
  return request({
    url: '/sys_user/delete',
    method: 'post', // 如果后端用的是 @DeleteMapping，这里改成 'delete'
    params: { id }
  })
}

export function changeUserStatus(id, status) {
  return request({ url: '/sys_user/status', method: 'post', params: { id, status } })
}
