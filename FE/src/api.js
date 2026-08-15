const API_ROOT = '/api/v1'
let csrfToken = null

function messageOf(payload, fallback) {
  return payload?.error?.message || payload?.message || fallback
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
    const error = new Error(messageOf(payload, 'Không thể xử lý yêu cầu.'))
    error.status = response.status
    error.code = payload?.error?.code
    throw error
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
  order: id => request(`/me/orders/${id}`)
}
