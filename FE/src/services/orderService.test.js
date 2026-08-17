import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CHECKOUT_IDEMPOTENCY_KEY } from './checkoutIdempotency'
import { checkoutOrder, completeCheckoutAttempt, getMyOrder, getMyOrders } from './orderService'

vi.mock('../api', () => ({ request: vi.fn().mockResolvedValue({ orderId: 42 }) }))
import { request } from '../api'

describe('order service contracts', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.stubGlobal('crypto', { randomUUID: vi.fn(() => 'stable-checkout-key') })
    request.mockClear()
  })

  it('sends the exact checkout payload and idempotency key', async () => {
    const payload = { receiverName: 'An', phone: '0901234567', email: '', address: 'Bàu Trúc', note: '' }
    await checkoutOrder(payload)
    await checkoutOrder(payload)
    expect(request).toHaveBeenCalledWith('/checkout', expect.objectContaining({ method: 'POST', body: JSON.stringify(payload), headers: { 'Idempotency-Key': 'stable-checkout-key' } }))
    expect(crypto.randomUUID).toHaveBeenCalledTimes(1)
  })

  it('clears the logical checkout key after finalization', () => {
    sessionStorage.setItem(CHECKOUT_IDEMPOTENCY_KEY, 'done-key')
    completeCheckoutAttempt()
    expect(sessionStorage.getItem(CHECKOUT_IDEMPOTENCY_KEY)).toBeNull()
  })

  it('creates a new checkout key after terminal failure cleanup', async () => {
    crypto.randomUUID.mockReturnValueOnce('failed-key').mockReturnValueOnce('new-key')
    await checkoutOrder({ receiverName: 'An' })
    completeCheckoutAttempt()
    await checkoutOrder({ receiverName: 'An' })
    expect(request).toHaveBeenNthCalledWith(1, '/checkout', expect.objectContaining({ headers: { 'Idempotency-Key': 'failed-key' } }))
    expect(request).toHaveBeenNthCalledWith(2, '/checkout', expect.objectContaining({ headers: { 'Idempotency-Key': 'new-key' } }))
  })

  it('uses backend pagination and owned-order detail routes', async () => {
    await getMyOrders(2, 20); await getMyOrder(9)
    expect(request).toHaveBeenCalledWith('/me/orders?page=2&size=20')
    expect(request).toHaveBeenCalledWith('/me/orders/9')
  })
})
