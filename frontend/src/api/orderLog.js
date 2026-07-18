// import request from '@/utils/request'

// export function getOrderLogList(orderNo) {
//   return request({ url: `/sys_order_log/${orderNo}`, method: 'get' })
// }

import request from '@/utils/request'

// 按订单编号查询日志
export function getOrderLogsByOrderNo(orderNo) {
  return request({
    url: `/api/sys_order_log/order/${orderNo}`,
    method: 'get'
  })
}

// 新增订单日志
export function addOrderLog(data) {
  return request({
    url: '/api/sys_order_log',
    method: 'post',
    data
  })
}