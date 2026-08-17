import { request } from '../api'
import { clearCheckoutIdempotencyKey, getCheckoutIdempotencyKey } from './checkoutIdempotency'

export const checkoutOrder = values => request('/checkout', {
  method: 'POST',
  headers: { 'Idempotency-Key': getCheckoutIdempotencyKey() },
  body: JSON.stringify(values)
})

export const completeCheckoutAttempt = () => clearCheckoutIdempotencyKey()

export const getMyOrders = (page = 0, size = 20) => request(`/me/orders?page=${page}&size=${size}`)
export const getMyOrder = id => request(`/me/orders/${encodeURIComponent(id)}`)
