import request from '@/utils/request'

export function createPayment(orderNo, channel = 'DEMO') {
  return request({
    url: '/api/payment/create',
    method: 'post',
    params: { orderNo, channel }
  })
}

export function getPaymentCapabilities() {
  return request({
    url: '/api/payment/capabilities',
    method: 'get',
    silent: true
  })
}

export function getPaymentStatus(orderNo) {
  return request({
    url: '/api/payment/status',
    method: 'get',
    params: { orderNo }
  })
}

export function confirmDemoPayment(paymentNo) {
  return request({
    url: '/api/payment/demo/confirm',
    method: 'post',
    params: { paymentNo }
  })
}

export function getPaymentList(params) {
  return request({
    url: '/api/payment/list',
    method: 'get',
    params
  })
}
