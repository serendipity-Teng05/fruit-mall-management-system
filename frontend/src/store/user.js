import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const user = ref(JSON.parse(sessionStorage.getItem('user') || 'null'))
  const token = ref(sessionStorage.getItem('token') || '')

  const isLoggedIn = computed(() => !!token.value)
  const roles = computed(() => user.value?.roles || [])
  const isAdmin = computed(() => roles.value.some(role => role.roleCode === 'ADMIN'))
  const permissionCodes = ref(JSON.parse(sessionStorage.getItem('permissionCodes') || '[]'))
  const permissions = computed(() => permissionCodes.value)

  const hasRole = (roleCode) => {
    return roles.value.some(role => role.roleCode === roleCode)
  }

  const hasPermission = (permissionCode) => {
    return permissions.value.includes(permissionCode)
  }

  function login(userData, tokenValue, permissionsValue = []) {
    user.value = userData
    token.value = tokenValue
    sessionStorage.setItem('user', JSON.stringify(userData))
    sessionStorage.setItem('token', tokenValue)
    permissionCodes.value = permissionsValue
    sessionStorage.setItem('permissionCodes', JSON.stringify(permissionsValue))
  }

  function logout() {
    user.value = null
    token.value = ''
    sessionStorage.removeItem('user')
    sessionStorage.removeItem('token')
    permissionCodes.value = []
    sessionStorage.removeItem('permissionCodes')
  }

  return {
    user,
    token,
    isLoggedIn,
    isAdmin,
    roles,
    permissions,
    hasRole,
    hasPermission,
    login,
    logout
  }
})
