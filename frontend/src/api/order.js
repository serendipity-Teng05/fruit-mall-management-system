// import request from '@/utils/request'

// export function getOrderList(params) {
//   return request({ url: '/sys_order/list', method: 'get', params })
// }

// export function updateOrderStatus(orderNo, status) {
//   return request({
//     url: '/sys_order/status',
//     method: 'post',
//     params: { orderNo, status }
//   })
// }

// export function deleteOrder(orderNo) {
//   return request({ url: `/sys_order/${orderNo}`, method: 'delete' })
// }
import request from '@/utils/request'

// 1. 获取订单列表 (支持分页和搜索)
export function getOrderList(params) {
  return request({
    url: '/sys_order/list',
    method: 'get',
    params
  })
}

// 2. 更新订单状态 (发货、完成等)
// 注意：你的数据库主键是 order_no，所以这里传 orderNo 更准确
export function updateOrderStatus(orderNo, status) {
  return request({
    url: '/sys_order/status',
    method: 'post',
    // 使用 params 将参数拼接到 URL 后 (例如: /order/status?orderNo=xxxx&status=1)
    params: { 
      orderNo: orderNo, 
      status: status 
    }
  })
}

// 3. 删除订单 (可选功能)
export function deleteOrder(orderNo) {
  return request({
    url: '/sys_order/delete',
    method: 'post',
    params: { orderNo }
  })
}

// 获取订单详情 (包含商品列表)
export function getOrderDetail(orderNo) {
  return request({
    url: '/sys_order/detail/' + orderNo,
    method: 'get'
  })
}



