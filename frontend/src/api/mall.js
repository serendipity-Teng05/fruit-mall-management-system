import request from '@/utils/request'

export const getMallProducts = params => request({
  url: '/api/mall/products', method: 'get', params, authOptional: true, silent: true
})

export const getMallProduct = id => request({
  url: `/api/mall/products/${id}`, method: 'get', authOptional: true, silent: true
})

export const getMallCategories = () => request({
  url: '/api/mall/products/categories', method: 'get', authOptional: true, silent: true
})

export const getAddresses = () => request({
  url: '/api/mall/addresses', method: 'get'
})

export const createAddress = data => request({
  url: '/api/mall/addresses', method: 'post', data
})

export const updateAddress = (id, data) => request({
  url: `/api/mall/addresses/${id}`, method: 'put', data
})

export const deleteAddress = id => request({
  url: `/api/mall/addresses/${id}`, method: 'delete'
})

export const setDefaultAddress = id => request({
  url: `/api/mall/addresses/${id}/default`, method: 'put'
})

export const createMallOrder = data => request({
  url: '/api/mall/orders', method: 'post', data
})

export const getMyOrders = params => request({
  url: '/api/mall/orders', method: 'get', params
})

export const getMyOrder = orderNo => request({
  url: `/api/mall/orders/${orderNo}`, method: 'get'
})

export const cancelMyOrder = orderNo => request({
  url: `/api/mall/orders/${orderNo}/cancel`, method: 'post'
})

export const confirmReceipt = orderNo => request({
  url: `/api/mall/orders/${orderNo}/confirm-receipt`, method: 'post'
})
