// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useCheckout } from './useOrders'
import { checkoutOrder, completeCheckoutAttempt } from '../services/orderService'
import { trackAnalytics } from '../analytics/Analytics'

vi.mock('../services/orderService', () => ({ checkoutOrder: vi.fn(), completeCheckoutAttempt: vi.fn(), clearFailedCheckoutAttempt: vi.fn() }))
vi.mock('../analytics/Analytics', () => ({ trackAnalytics: vi.fn(() => true) }))

const wrapper = ({ children }) => <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })}>{children}</QueryClientProvider>

beforeEach(() => { vi.clearAllMocks(); sessionStorage.clear(); localStorage.clear() })

describe('checkout analytics', () => {
  it('emits one order_created event after successful checkout and deduplicates a replayed order', async () => {
    checkoutOrder.mockResolvedValue({ orderId: 42 })
    const { result } = renderHook(() => useCheckout(), { wrapper })
    await act(async () => { await result.current.mutateAsync({ productId: 7 }) })
    await act(async () => { await result.current.mutateAsync({ productId: 7 }) })
    expect(trackAnalytics).toHaveBeenCalledTimes(1)
    expect(trackAnalytics).toHaveBeenCalledWith('order_created', { checkout_type: 'buy_now' })
  })

  it('does not emit success on checkout failure', async () => {
    checkoutOrder.mockRejectedValue(new Error('checkout failed'))
    const { result } = renderHook(() => useCheckout(), { wrapper })
    await act(async () => { await expect(result.current.mutateAsync({})).rejects.toThrow('checkout failed') })
    expect(trackAnalytics).not.toHaveBeenCalled()
  })

  it('still completes checkout when analytics storage is unavailable', async () => {
    checkoutOrder.mockResolvedValue({ orderId: 43 })
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('storage blocked') })
    const { result } = renderHook(() => useCheckout(), { wrapper })
    await act(async () => { await expect(result.current.mutateAsync({})).resolves.toEqual({ orderId: 43 }) })
    expect(completeCheckoutAttempt).toHaveBeenCalledTimes(1)
    vi.restoreAllMocks()
  })
})
