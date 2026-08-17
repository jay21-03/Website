import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CHECKOUT_IDEMPOTENCY_KEY, clearCheckoutIdempotencyKey, getCheckoutIdempotencyKey } from './checkoutIdempotency'

describe('checkout idempotency key', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.stubGlobal('crypto', { randomUUID: vi.fn(() => 'checkout-key-1') })
  })

  it('creates one key per logical checkout and reuses it across retries', () => {
    expect(getCheckoutIdempotencyKey()).toBe('checkout-key-1')
    expect(getCheckoutIdempotencyKey()).toBe('checkout-key-1')
    expect(crypto.randomUUID).toHaveBeenCalledTimes(1)
  })

  it('clears key after finalized checkout', () => {
    getCheckoutIdempotencyKey()
    clearCheckoutIdempotencyKey()
    expect(sessionStorage.getItem(CHECKOUT_IDEMPOTENCY_KEY)).toBeNull()
  })
})
