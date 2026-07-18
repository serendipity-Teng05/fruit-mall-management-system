// src/api/auth.js

import request from '@/utils/request'

// ===================== 登录相关接口 =====================

// 登录接口，对应后端 AuthController /auth/login
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data,
    // 登录页会在表单内展示具体错误，避免页面顶部重复弹出提示。
    silent: true
  })
}

export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

// 获取当前登录用户信息，对应后端 AuthController /auth/info
export function getInfo() {
  return request({
    url: '/auth/info',
    method: 'get'
  })
}

// 退出登录接口，对应后端 AuthController /auth/logout
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

// 获取图形验证码，对应后端 AuthController /auth/captcha
// 返回 blob 格式图片，前端可以直接生成 URL 显示
export function getCaptcha() {
  return request({
    url: '/auth/captcha',
    method: 'get',
    responseType: 'blob',
    silent: true
  })
}
