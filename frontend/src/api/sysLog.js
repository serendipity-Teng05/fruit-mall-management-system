// import request from '@/utils/request'

// export function getSysLogList(userId) {
//   return request({ url: `/sys_log/${userId}`, method: 'get' })
// }



import request from '@/utils/request'

// 获取系统日志列表
export function getSysLogs(params) {
  return request({
    url: '/api/sys-log/list',
    method: 'get',
    params
  })
}

// 按用户查询日志（保留单独方法也方便用）
export function getSysLogsByUser(userId) {
  return request({
    url: `/api/sys-log/user/${userId}`,
    method: 'get'
  })
}

// 新增系统日志
export function addSysLog(data) {
  return request({
    url: '/api/sys-log',
    method: 'post',
    data
  })
}

// 删除系统日志
export function deleteSysLog(logId) {
  return request({
    url: '/api/sys-log/delete',
    method: 'post',
    data: { logId }
  })
}

// 清空系统日志
export function clearSysLogs() {
  return request({
    url: '/api/sys-log/clear',
    method: 'post'
  })
}
