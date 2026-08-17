import { apiErrorMessage } from './utils/apiError'
import { getCheckoutIdempotencyKey } from './services/checkoutIdempotency'

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

async function ensureCsrfToken() {
  if (csrfToken) return csrfToken
  const response = await fetch(`${API_ROOT}/auth/csrf`, { credentials: 'include' })
  const payload = await response.json().catch(() => null)
  if (!response.ok) throw new Error(messageOf(payload, 'Không thể khởi tạo phiên bảo mật.'))
  csrfToken = payload.data.token
  return csrfToken
}

async function send(path, options, retryOnCsrfInvalid) {
  const method = (options.method || 'GET').toUpperCase()
  const headers = { ...(options.headers || {}) }
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    headers['X-XSRF-TOKEN'] = await ensureCsrfToken()
  }
  if (options.body && !(options.body instanceof FormData)) headers['Content-Type'] = 'application/json'
  const response = await fetch(`${API_ROOT}${path}`, { ...options, headers, credentials: 'include' })
  const payload = await response.json().catch(() => null)
  if (!response.ok || payload?.success === false) {
    if (response.status === 403 && payload?.error?.code === 'CSRF_INVALID') {
      csrfToken = null
      if (retryOnCsrfInvalid) return send(path, options, false)
    }
    const code = payload?.error?.code
    throw new ApiError(apiErrorMessage(code, messageOf(payload, 'Không thể xử lý yêu cầu.')), response.status, code, payload?.error?.fieldErrors)
  }
  if (path === '/auth/google' || path === '/auth/logout') csrfToken = null
  return payload?.data
}

export async function request(path, options = {}) {
  return send(path, options, true)
}

export function resetCsrfTokenForTests() {
  csrfToken = null
}

const queryString = params => {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, value)
  })
  return search.toString()
}

export const api = {
  products: (params = {}, size) => {
    const normalized = typeof params === 'number' ? { page: params, size: size ?? 20 } : params
    return request(`/products?${queryString({ page: 0, size: 20, ...normalized })}`)
  },
  product: id => request(`/products/${encodeURIComponent(id)}`),
  collections: () => request('/collections'),
  workshops: () => request('/workshops'),
  supportSettings: () => request('/support/settings'),
  me: () => request('/me'),
  googleLogin: credential => request('/auth/google', { method: 'POST', body: JSON.stringify({ credential }) }),
  logout: () => request('/auth/logout', { method: 'POST' }),
  workshopBooking: values => request('/workshop/bookings', { method: 'POST', body: JSON.stringify(values) }),
  cart: () => request('/cart'),
  addCart: (productId, quantity = 1) => request('/cart/items', { method: 'POST', body: JSON.stringify({ productId, quantity }) }),
  updateCart: (itemId, quantity) => request(`/cart/items/${itemId}`, { method: 'PATCH', body: JSON.stringify({ quantity }) }),
  removeCart: itemId => request(`/cart/items/${itemId}`, { method: 'DELETE' }),
  checkout: values => request('/checkout', { method: 'POST', headers: { 'Idempotency-Key': getCheckoutIdempotencyKey() }, body: JSON.stringify(values) }),
  orders: (page = 0, size = 20) => request(`/me/orders?page=${page}&size=${size}`),
  order: id => request(`/me/orders/${id}`),
  adminDashboard: () => request('/admin/dashboard'),
  adminProducts: (params = '') => request(`/admin/products${params ? `?${params}` : ''}`),
  adminProduct: id => request(`/admin/products/${encodeURIComponent(id)}`),
  adminCollections: (params = '') => request(`/admin/collections${params ? `?${params}` : ''}`),
  adminCollection: id => request(`/admin/collections/${encodeURIComponent(id)}`),
  adminUsers: (params = '') => request(`/admin/users${params ? `?${params}` : ''}`),
  adminUserAction: (id, action) => request(`/admin/users/${id}/${action}`, { method: 'POST' }),
  adminSupportSettings: () => request('/admin/support/settings'),
  updateSupportSettings: values => request('/admin/support/settings', { method: 'PUT', body: JSON.stringify(values) }),
  adminInventory: (params = '') => request(`/admin/inventory${params ? `?${params}` : ''}`),
  adminInventoryHistory: (productId, params = '') => request(`/admin/inventory/${productId}/transactions${params ? `?${params}` : ''}`),
  adminAdjustInventory: (id, values) => request(`/admin/inventory/${id}/adjust`, { method: 'POST', body: JSON.stringify(values) }),
  adminOrders: (params = '') => request(`/admin/orders${params ? `?${params}` : ''}`),
  adminOrder: id => request(`/admin/orders/${id}`),
  adminOrderStatus: (id, status) => request(`/admin/orders/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  adminCancelOrder: id => request(`/admin/orders/${id}/cancel`, { method: 'POST' }),
  adminManualRefund: (paymentId, note) => request(`/admin/payments/${paymentId}/manual-refund`, { method: 'POST', body: JSON.stringify({ note }) }),
  adminNotifications: (params = '') => request(`/admin/notifications${params ? `?${params}` : ''}`),
  adminReadNotification: id => request(`/admin/notifications/${id}/read`, { method: 'PATCH' }),
  adminNotificationStreamUrl: () => `${API_ROOT}/admin/notifications/stream`,
  adminWorkshops: () => request('/admin/workshops'),
  createWorkshop: values => request('/admin/workshops', { method: 'POST', body: JSON.stringify(values) }),
  updateWorkshop: (id, values) => request(`/admin/workshops/${id}`, { method: 'PUT', body: JSON.stringify(values) }),
  deleteWorkshop: id => request(`/admin/workshops/${id}`, { method: 'DELETE' }),
  adminWorkshopBookings: (params = '') => request(`/admin/workshop/bookings${params ? `?${params}` : ''}`),
  adminWorkshopStatus: (id, status) => request(`/admin/workshop/bookings/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  createProduct: values => request('/admin/products', { method: 'POST', body: JSON.stringify(values) }),
  updateProduct: (id, values) => request(`/admin/products/${id}`, { method: 'PUT', body: JSON.stringify(values) }),
  updateProductStatus: (id, status) => request(`/admin/products/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  deleteProduct: id => request(`/admin/products/${id}`, { method: 'DELETE' }),
  updateDiscount: (productId, values) => request(`/admin/products/${productId}/discount`, { method: 'PUT', body: JSON.stringify(values) }),
  toggleDiscount: (productId, isActive) => request(`/admin/products/${productId}/discount/active`, { method: 'PATCH', body: JSON.stringify({ isActive }) }),
  deleteDiscount: productId => request(`/admin/products/${productId}/discount`, { method: 'DELETE' }),
  uploadProductImage: (productId, file) => {
    const body = new FormData()
    body.append('file', file)
    return request(`/admin/products/${productId}/images`, { method: 'POST', body })
  },
  setProductThumbnail: (productId, imageId) => request(`/admin/products/${productId}/images/${imageId}/thumbnail`, { method: 'PUT' }),
  reorderProductImages: (productId, imageIds) => request(`/admin/products/${productId}/images/order`, { method: 'PUT', body: JSON.stringify({ imageIds }) }),
  deleteProductImage: (productId, imageId) => request(`/admin/products/${productId}/images/${imageId}`, { method: 'DELETE' }),
  createCollection: values => request('/admin/collections', { method: 'POST', body: JSON.stringify(values) }),
  updateCollection: (id, values) => request(`/admin/collections/${id}`, { method: 'PUT', body: JSON.stringify(values) }),
  updateCollectionStatus: (id, status) => request(`/admin/collections/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  deleteCollection: id => request(`/admin/collections/${id}`, { method: 'DELETE' }),
  revenueReport: params => request(`/admin/reports/revenue?${params}`),
  bestSellingReport: params => request(`/admin/reports/best-selling?${params}`)
}
