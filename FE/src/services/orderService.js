import { request } from '../api'

export const checkoutOrder = values => request('/checkout', {
  method: 'POST',
  headers: { 'Idempotency-Key': crypto.randomUUID() },
  body: JSON.stringify(values)
})

export const getMyOrders = (page = 0, size = 20) => request(`/me/orders?page=${page}&size=${size}`)
export const getMyOrder = id => request(`/me/orders/${encodeURIComponent(id)}`)
