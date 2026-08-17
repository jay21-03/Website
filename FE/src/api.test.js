import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request, resetCsrfTokenForTests } from './api'

const response = (data, ok = true, status = 200) => ({ ok, status, json: vi.fn().mockResolvedValue(data) })

describe('API client security', () => {
  beforeEach(() => {
    resetCsrfTokenForTests()
    vi.stubGlobal('fetch', vi.fn())
  })

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

  it('builds authoritative product query parameters for backend filtering', async () => {
    fetch.mockResolvedValueOnce(response({ success: true, data: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true } }))
    const { api } = await import('./api')
    await api.products({ keyword: 'binh', collectionId: 7, minPrice: 1000, maxPrice: 2000, page: 1, size: 20, sort: 'sellingPrice,asc' })
    expect(fetch).toHaveBeenCalledWith('/api/v1/products?page=1&size=20&keyword=binh&collectionId=7&minPrice=1000&maxPrice=2000&sort=sellingPrice%2Casc', expect.objectContaining({ credentials: 'include' }))
  })

  it('sends multipart uploads with CSRF header without forcing JSON content type', async () => {
    const body = new FormData()
    body.append('file', new Blob(['image']), 'image.png')
    fetch.mockResolvedValueOnce(response({ data: { token: 'upload-token' } })).mockResolvedValueOnce(response({ success: true, data: { id: 3 } }))

    await request('/admin/products/1/images', { method: 'POST', body })

    expect(fetch).toHaveBeenNthCalledWith(2, '/api/v1/admin/products/1/images', expect.objectContaining({
      credentials: 'include',
      body,
      headers: expect.objectContaining({ 'X-XSRF-TOKEN': 'upload-token' })
    }))
    expect(fetch.mock.calls[1][1].headers['Content-Type']).toBeUndefined()
  })

  it('refreshes CSRF token once and retries when a mutation gets CSRF_INVALID', async () => {
    fetch
      .mockResolvedValueOnce(response({ data: { token: 'old-token' } }))
      .mockResolvedValueOnce(response({ success: false, error: { code: 'CSRF_INVALID', message: 'Invalid CSRF token.' } }, false, 403))
      .mockResolvedValueOnce(response({ data: { token: 'fresh-token' } }))
      .mockResolvedValueOnce(response({ success: true, data: { id: 4 } }))

    await request('/admin/products/1/images', { method: 'POST', body: new FormData() })

    expect(fetch).toHaveBeenNthCalledWith(2, '/api/v1/admin/products/1/images', expect.objectContaining({
      headers: expect.objectContaining({ 'X-XSRF-TOKEN': 'old-token' })
    }))
    expect(fetch).toHaveBeenNthCalledWith(3, '/api/v1/auth/csrf', { credentials: 'include' })
    expect(fetch).toHaveBeenNthCalledWith(4, '/api/v1/admin/products/1/images', expect.objectContaining({
      headers: expect.objectContaining({ 'X-XSRF-TOKEN': 'fresh-token' })
    }))
  })
})
