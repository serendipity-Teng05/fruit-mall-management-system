// import request from '@/utils/request'

// export function getProductList(params) {
//   return request({ url: '/sys_product/list', method: 'get', params })
// }

// export function saveProduct(data) {
//   return request({ url: '/sys_product/save', method: 'post', data })
// }

// export function deleteProduct(id) {
//   return request({ url: `/sys_product/${id}`, method: 'delete' })
// }

// src/api/product.js

import request from '@/utils/request'

/**
 * 1. 获取商品列表
 * @param {Object} params 查询参数
 */
export function getProductList(params) {
  return request({
    url: '/sys_product/list',
    method: 'get',
    params
  })
}

/**
 * 2. 保存商品（新增或修改）
 * @param {Object} data 商品对象
 */
export function saveProduct(data) {
  return request({
    url: '/sys_product/save',
    method: 'post',
    data
  })
}

/**
 * 3. 删除商品
 * @param {Number|String} id 商品ID
 */
export function deleteProduct(id) {
  return request({
    url: '/sys_product/delete',
    method: 'post',
    params: { id }
  })
}

/**
 * 4. 更新库存（增加或扣减）
 * 后端代码需要实现
 * @param {Number|String} id 商品ID
 * @param {Number} quantityChange 改变的数量（正数增加，负数扣减）
 */
export function updateProductStock(id, quantityChange) {
  return request({
    url: '/sys_product/stock',
    method: 'put',
    data: { id, quantityChange }
  })
}

// 低库存数据由 /dashboard/data 统一返回，避免重复请求。

