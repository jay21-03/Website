export const CHECKOUT_IDEMPOTENCY_KEY = 'bautruc.checkout.idempotency-key'

export function getCheckoutIdempotencyKey(storage = window.sessionStorage) {
  const existing = storage.getItem(CHECKOUT_IDEMPOTENCY_KEY)
  if (existing) return existing
  const created = crypto.randomUUID()
  storage.setItem(CHECKOUT_IDEMPOTENCY_KEY, created)
  return created
}

export function clearCheckoutIdempotencyKey(storage = window.sessionStorage) {
  storage.removeItem(CHECKOUT_IDEMPOTENCY_KEY)
}

const terminalCheckoutFailureCodes = new Set(['PAYOS_REQUEST_FAILED'])
const resumableCheckoutCodes = new Set(['CHECKOUT_IN_PROGRESS', 'CHECKOUT_FINALIZATION_PENDING'])

export function shouldClearCheckoutIdempotencyKey(error) {
  if (!error?.code) return false
  if (resumableCheckoutCodes.has(error.code)) return false
  return terminalCheckoutFailureCodes.has(error.code)
}
