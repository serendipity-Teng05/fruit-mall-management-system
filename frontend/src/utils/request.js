import axios from 'axios'
import { ElMessage } from 'element-plus'

const createRequestId = () => {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()
  return `${Date.now()}-${Math.random().toString(36).slice(2, 12)}`
}

const clearSession = () => {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('user')
  sessionStorage.removeItem('permissionCodes')
}

const redirectToLogin = () => {
  if (window.location.pathname === '/login') return
  const current = `${window.location.pathname}${window.location.search}${window.location.hash}`
  window.location.replace(`/login?redirect=${encodeURIComponent(current)}`)
}

const getErrorMessage = error => {
  const data = error?.response?.data
  let message
  if (data && typeof data === 'object') message = data.msg || data.message || data.error
  if (!message && typeof data === 'string' && data.trim()) message = data.trim()
  if (!message && error?.response?.status) message = `服务请求失败（HTTP ${error.response.status}）`
  if (!message) message = '网络错误，请检查网络连接或后端服务状态'
  const requestId = error?.response?.headers?.['x-request-id'] || error?.config?.headers?.['X-Request-Id']
  return requestId ? `${message}（请求编号：${requestId}）` : message
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/',
  timeout: Number(import.meta.env.VITE_API_TIMEOUT || 15000),
  withCredentials: true
})

let refreshPromise = null
const refreshSession = () => {
  if (!refreshPromise) {
    refreshPromise = service.post('/auth/refresh', null, { silent: true, skipRefresh: true })
      .then(response => {
        const session = response.data || {}
        if (!session.token) throw new Error('刷新登录状态未返回访问令牌')
        sessionStorage.setItem('token', session.token)
        if (session.user) sessionStorage.setItem('user', JSON.stringify(session.user))
        if (session.permissionCodes) sessionStorage.setItem('permissionCodes', JSON.stringify(session.permissionCodes))
        return session.token
      })
      .finally(() => { refreshPromise = null })
  }
  return refreshPromise
}

service.interceptors.request.use(config => {
  config.headers['X-Request-Id'] = config.headers['X-Request-Id'] || createRequestId()
  const token = sessionStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res instanceof Blob) return res
    if (res?.code != null && String(res.code) !== '200') {
      if (!response.config.silent) ElMessage.error(res.msg || '请求失败')
      res.__handled = true
      res.__notified = !response.config.silent
      return Promise.reject(res)
    }
    return res
  },
  async error => {
    if (error?.__handled) return Promise.reject(error)
    const config = error.config || {}
    const status = error.response?.status

    if (status === 401 && config.skipRefresh) {
      error.__handled = true
      return Promise.reject(error)
    }

    const currentToken = sessionStorage.getItem('token')
    if (status === 401 && currentToken && !config._retry && !config.authOptional) {
      config._retry = true
      try {
        const renewedToken = await refreshSession()
        config.headers = config.headers || {}
        config.headers.Authorization = `Bearer ${renewedToken}`
        config.headers['X-Request-Id'] = createRequestId()
        return service(config)
      } catch {
        clearSession()
        if (!config.silent) ElMessage.error('登录已过期，请重新登录')
        redirectToLogin()
        error.__handled = true
        return Promise.reject(error)
      }
    }

    if (status === 401) {
      if (!config.silent) {
        ElMessage.error('未授权或登录已过期，请重新登录')
        error.__notified = true
      }
      if (!config.authOptional) {
        clearSession()
        redirectToLogin()
      }
    } else if (!config.silent) {
      ElMessage.error(getErrorMessage(error))
      error.__notified = true
    }
    error.__handled = true
    return Promise.reject(error)
  }
)

export default service
