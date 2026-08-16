import { apiErrorMessage } from './utils/apiError'

const API_ROOT = '/api/v1'
let csrfToken = null

function messageOf(payload, fallback) {
  return payload?.error?.message || payload?.message || fallback
}

export class ApiError extends Error {
  constructor(message, status, code, fieldErrors = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

export async function request(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase()
  const headers = { ...(options.headers || {}) }
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    if (!csrfToken) {
      const response = await fetch(`${API_ROOT}/auth/csrf`, { credentials: 'include' })
      const payload = await response.json().catch(() => null)
      if (!response.ok) throw new Error(messageOf(payload, 'Không thể khởi tạo phiên bảo mật.'))
      csrfToken = payload.data.token
    }
    headers['X-XSRF-TOKEN'] = csrfToken
  }
  if (options.body && !(options.body instanceof FormData)) headers['Content-Type'] = 'application/json'
  const response = await fetch(`${API_ROOT}${path}`, { ...options, headers, credentials: 'include' })
  const payload = await response.json().catch(() => null)
  if (!response.ok || payload?.success === false) {
    if (response.status === 403 && payload?.error?.code === 'CSRF_INVALID') csrfToken = null
    const code = payload?.error?.code
    throw new ApiError(apiErrorMessage(code, messageOf(payload, 'Không thể xử lý yêu cầu.')), response.status, code, payload?.error?.fieldErrors)
  }
  if (path === '/auth/google' || path === '/auth/logout') csrfToken = null
  return payload?.data
}

export const api = {
  products: (page = 0, size = 100) => request(`/products?page=${page}&size=${size}`),
  collections: () => request('/collections'),
  me: () => request('/me'),
  googleLogin: credential => request('/auth/google', { method: 'POST', body: JSON.stringify({ credential }) }),
  logout: () => request('/auth/logout', { method: 'POST' }),
  cart: () => request('/cart'),
  addCart: (productId, quantity = 1) => request('/cart/items', { method: 'POST', body: JSON.stringify({ productId, quantity }) }),
  updateCart: (itemId, quantity) => request(`/cart/items/${itemId}`, { method: 'PATCH', body: JSON.stringify({ quantity }) }),
  removeCart: itemId => request(`/cart/items/${itemId}`, { method: 'DELETE' }),
  checkout: values => request('/checkout', { method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify(values) }),
  orders: (page = 0, size = 20) => request(`/me/orders?page=${page}&size=${size}`),
  order: id => request(`/me/orders/${id}`),
  adminDashboard: () => request('/admin/dashboard'),
  adminUsers: (params = '') => request(`/admin/users${params ? `?${params}` : ''}`),
  adminUserAction: (id, action) => request(`/admin/users/${id}/${action}`, { method: 'POST' }),
  adminInventory: (params = '') => request(`/admin/inventory${params ? `?${params}` : ''}`),
  adminAdjustInventory: (id, values) => request(`/admin/inventory/${id}/adjust`, { method: 'POST', body: JSON.stringify(values) }),
  adminOrders: (params = '') => request(`/admin/orders${params ? `?${params}` : ''}`),
  adminOrder: id => request(`/admin/orders/${id}`),
  adminOrderStatus: (id, status) => request(`/admin/orders/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  adminCancelOrder: id => request(`/admin/orders/${id}/cancel`, { method: 'POST' }),
  adminNotifications: (params = '') => request(`/admin/notifications${params ? `?${params}` : ''}`),
  adminReadNotification: id => request(`/admin/notifications/${id}/read`, { method: 'PATCH' }),
  createProduct: values => request('/admin/products', { method: 'POST', body: JSON.stringify(values) }),
  updateProduct: (id, values) => request(`/admin/products/${id}`, { method: 'PUT', body: JSON.stringify(values) }),
  deleteProduct: id => request(`/admin/products/${id}`, { method: 'DELETE' }),
  uploadProductImage: (productId, file) => {
    const body = new FormData()
    body.append('file', file)
    return request(`/admin/products/${productId}/images`, { method: 'POST', body })
  },
  setProductThumbnail: (productId, imageId) => request(`/admin/products/${productId}/images/${imageId}/thumbnail`, { method: 'PUT' }),
  deleteProductImage: (productId, imageId) => request(`/admin/products/${productId}/images/${imageId}`, { method: 'DELETE' }),
  createCollection: values => request('/admin/collections', { method: 'POST', body: JSON.stringify(values) }),
  updateCollection: (id, values) => request(`/admin/collections/${id}`, { method: 'PUT', body: JSON.stringify(values) }),
  deleteCollection: id => request(`/admin/collections/${id}`, { method: 'DELETE' }),
  revenueReport: params => request(`/admin/reports/revenue?${params}`),
  bestSellingReport: params => request(`/admin/reports/best-selling?${params}`)
}
