import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'

const ownerFromSession = () => {
  try { return JSON.parse(sessionStorage.getItem('user') || 'null')?.id || 'guest' } catch { return 'guest' }
}
const storageKey = owner => `fruitMallCart:${owner}`
const readCart = owner => {
  try {
    const stored = localStorage.getItem(storageKey(owner))
      || (owner === 'guest' ? localStorage.getItem('fruitMallCart') : null)
      || '[]'
    const value = JSON.parse(stored)
    if (owner === 'guest' && localStorage.getItem('fruitMallCart')) localStorage.removeItem('fruitMallCart')
    return Array.isArray(value) ? value : []
  } catch { return [] }
}

export const useCartStore = defineStore('cart', () => {
  const owner = ref(ownerFromSession())
  const items = ref(readCart(owner.value))
  const count = computed(() => items.value.reduce((sum, item) => sum + item.count, 0))
  const total = computed(() => items.value.reduce((sum, item) => sum + Number(item.price || 0) * item.count, 0))
  const hasInvalidItems = computed(() => items.value.some(item => item.invalid || item.stock <= 0))

  const add = (product, quantity = 1) => {
    if (!product || product.status === 0 || Number(product.stock || 0) <= 0) return false
    const safeQuantity = Math.max(1, Number(quantity) || 1)
    const existing = items.value.find(item => item.id === product.id)
    const nextCount = Math.min(Number(product.stock), (existing?.count || 0) + safeQuantity)
    if (existing) Object.assign(existing, { count: nextCount, stock: product.stock, price: product.price, invalid: false })
    else items.value.push({ id: product.id, name: product.name, price: product.price, unit: product.unit,
      image: product.image, stock: product.stock, count: nextCount, invalid: false })
    return true
  }

  const setCount = (id, quantity) => {
    const item = items.value.find(entry => entry.id === id)
    if (!item || item.invalid) return
    item.count = Math.min(Number(item.stock || 0), Math.max(1, Number(quantity) || 1))
  }
  const remove = id => { items.value = items.value.filter(item => item.id !== id) }
  const clear = () => { items.value = [] }

  const switchOwner = userId => {
    const nextOwner = userId || 'guest'
    if (String(nextOwner) === String(owner.value)) return
    localStorage.setItem(storageKey(owner.value), JSON.stringify(items.value))
    const nextCart = readCart(nextOwner)
    if (owner.value === 'guest' && nextOwner !== 'guest') {
      for (const guestItem of items.value) {
        const existing = nextCart.find(item => item.id === guestItem.id)
        if (existing) existing.count = Math.min(Number(existing.stock || guestItem.stock || 0), existing.count + guestItem.count)
        else nextCart.push(guestItem)
      }
      localStorage.removeItem(storageKey('guest'))
    }
    owner.value = nextOwner
    items.value = nextCart
  }

  const refresh = async loader => {
    const refreshed = await Promise.all(items.value.map(async item => {
      try {
        const response = await loader(item.id)
        const product = response?.data || response
        return { ...item, name: product.name, price: product.price, unit: product.unit,
          image: product.image, stock: product.stock, count: Math.min(item.count, product.stock), invalid: product.status !== 1 || product.stock <= 0 }
      } catch { return { ...item, invalid: true } }
    }))
    items.value = refreshed
  }

  watch(items, value => localStorage.setItem(storageKey(owner.value), JSON.stringify(value)), { deep: true })
  return { items, count, total, hasInvalidItems, add, setCount, remove, clear, switchOwner, refresh }
})
