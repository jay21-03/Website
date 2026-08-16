import { describe, expect, it, vi } from 'vitest'
import { checkoutOrder, getMyOrder, getMyOrders } from './orderService'

vi.mock('../api', () => ({ request: vi.fn().mockResolvedValue({ orderId: 42 }) }))
import { request } from '../api'

describe('order service contracts', () => {
  it('sends the exact checkout payload and idempotency key', async () => {
    const payload = { receiverName: 'An', phone: '0901234567', email: '', address: 'Bàu Trúc', note: '' }
    await checkoutOrder(payload)
    expect(request).toHaveBeenCalledWith('/checkout', expect.objectContaining({ method: 'POST', body: JSON.stringify(payload), headers: { 'Idempotency-Key': expect.any(String) } }))
  })

  it('uses backend pagination and owned-order detail routes', async () => {
    await getMyOrders(2, 20); await getMyOrder(9)
    expect(request).toHaveBeenCalledWith('/me/orders?page=2&size=20')
    expect(request).toHaveBeenCalledWith('/me/orders/9')
  })
})
