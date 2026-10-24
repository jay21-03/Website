// @vitest-environment jsdom
import { act, render, screen } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import RealtimeAnalytics from './RealtimeAnalytics'
import { api } from '../api'

vi.mock('../api', () => ({ api: { adminAnalyticsRealtime: vi.fn() } }))

afterEach(() => { vi.useRealTimers(); vi.resetAllMocks(); Object.defineProperty(document, 'hidden', { configurable: true, value: false }) })

test('polls once per 30 seconds only while visible and stops on unmount', async () => {
  vi.useFakeTimers()
  api.adminAnalyticsRealtime.mockResolvedValue({ status: 'LIVE', fetchedAt: new Date().toISOString(), metrics: { activeUsers: 1 }, trend: [] })
  const view = render(<RealtimeAnalytics lang="en" />)
  await act(async () => { await Promise.resolve() })
  expect(api.adminAnalyticsRealtime).toHaveBeenCalledTimes(1)
  expect(screen.getByText('Active users')).toBeTruthy()
  await act(async () => { await vi.advanceTimersByTimeAsync(30000) })
  expect(api.adminAnalyticsRealtime).toHaveBeenCalledTimes(2)
  Object.defineProperty(document, 'hidden', { configurable: true, value: true })
  act(() => document.dispatchEvent(new Event('visibilitychange')))
  await act(async () => { await vi.advanceTimersByTimeAsync(60000) })
  expect(api.adminAnalyticsRealtime).toHaveBeenCalledTimes(2)
  Object.defineProperty(document, 'hidden', { configurable: true, value: false })
  act(() => document.dispatchEvent(new Event('visibilitychange')))
  await act(async () => { await Promise.resolve() })
  expect(api.adminAnalyticsRealtime).toHaveBeenCalledTimes(3)
  view.unmount()
  await act(async () => { await vi.advanceTimersByTimeAsync(60000) })
  expect(api.adminAnalyticsRealtime).toHaveBeenCalledTimes(3)
})
