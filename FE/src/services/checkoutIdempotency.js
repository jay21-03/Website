export const CHECKOUT_IDEMPOTENCY_KEY = 'bautruc.checkout.idempotency-key'
export const checkoutIdempotencyStorageKey = values => values?.productId != null
  ? `${CHECKOUT_IDEMPOTENCY_KEY}.buy-now.${values.productId}.${values.quantity}`
  : CHECKOUT_IDEMPOTENCY_KEY

export function getCheckoutIdempotencyKey(storage = window.sessionStorage, key = CHECKOUT_IDEMPOTENCY_KEY) {
  const existing = storage.getItem(key)
  if (existing) return existing
  const created = crypto.randomUUID()
  storage.setItem(key, created)
  return created
}

export function clearCheckoutIdempotencyKey(storage = window.sessionStorage, key = CHECKOUT_IDEMPOTENCY_KEY) {
  storage.removeItem(key)
}

const terminalCheckoutFailureCodes = new Set(['PAYOS_REQUEST_FAILED'])
const resumableCheckoutCodes = new Set(['CHECKOUT_IN_PROGRESS', 'CHECKOUT_FINALIZATION_PENDING'])

export function shouldClearCheckoutIdempotencyKey(error) {
  if (!error?.code) return false
  if (resumableCheckoutCodes.has(error.code)) return false
  return terminalCheckoutFailureCodes.has(error.code)
}
