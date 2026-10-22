import { request } from '../api'
import { checkoutIdempotencyStorageKey, clearCheckoutIdempotencyKey, getCheckoutIdempotencyKey } from './checkoutIdempotency'

export const checkoutOrder = values => request('/checkout', {
  method: 'POST',
  headers: { 'Idempotency-Key': getCheckoutIdempotencyKey(window.sessionStorage, checkoutIdempotencyStorageKey(values)) },
  body: JSON.stringify(values)
})

export const completeCheckoutAttempt = values => clearCheckoutIdempotencyKey(window.sessionStorage, checkoutIdempotencyStorageKey(values))
export const clearFailedCheckoutAttempt = completeCheckoutAttempt

export const getMyOrders = (page = 0, size = 20) => request(`/me/orders?page=${page}&size=${size}`)
export const getMyOrder = id => request(`/me/orders/${encodeURIComponent(id)}`)
