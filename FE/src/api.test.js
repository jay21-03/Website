import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from './api'

const response = (data, ok = true, status = 200) => ({ ok, status, json: vi.fn().mockResolvedValue(data) })

describe('API client security', () => {
  beforeEach(() => { vi.stubGlobal('fetch', vi.fn()) })

  it('preserves credentials for authenticated reads', async () => {
    fetch.mockResolvedValueOnce(response({ success: true, data: { content: [] } }))
    await request('/me/orders')
    expect(fetch).toHaveBeenCalledWith('/api/v1/me/orders', expect.objectContaining({ credentials: 'include' }))
  })

  it('reuses CSRF flow for checkout mutation', async () => {
    fetch.mockResolvedValueOnce(response({ data: { token: 'memory-token' } })).mockResolvedValueOnce(response({ success: true, data: { orderId: 8 } }))
    await request('/checkout', { method: 'POST', body: '{}' })
    expect(fetch).toHaveBeenNthCalledWith(1, '/api/v1/auth/csrf', { credentials: 'include' })
    expect(fetch).toHaveBeenNthCalledWith(2, '/api/v1/checkout', expect.objectContaining({ credentials: 'include', headers: expect.objectContaining({ 'X-XSRF-TOKEN': 'memory-token' }) }))
  })
})
