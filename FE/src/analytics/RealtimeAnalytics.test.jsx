// @vitest-environment jsdom
import { act, render, screen } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import RealtimeAnalytics, { Breakdown, friendlyName } from './RealtimeAnalytics'
import { api } from '../api'

vi.mock('../api', () => ({ api: { adminAnalyticsRealtime: vi.fn() } }))

afterEach(() => { vi.useRealTimers(); vi.resetAllMocks(); Object.defineProperty(document, 'hidden', { configurable: true, value: false }) })

test('shows common GA4 actions in plain Vietnamese while keeping English labels in English mode', () => {
  expect(['page_view', 'user_engagement', 'scroll', 'session_start', 'view_item', 'first_visit'].map(name => friendlyName(name, 'vi'))).toEqual([
    'Xem trang', 'Tương tác với trang', 'Cuộn gần hết trang', 'Bắt đầu lượt truy cập', 'Xem sản phẩm', 'Lần đầu ghé thăm'
  ])
  expect(friendlyName('session_start', 'en')).toBe('Visit started')
  render(<Breakdown title="Trang và thao tác" items={[{ name: 'user_engagement', eventCount: 16 }, { name: 'first_visit', eventCount: 5 }]} labelKey="name" valueKey="eventCount" lang="vi" translate />)
  expect(screen.getByText('Tương tác với trang')).toBeTruthy()
  expect(screen.getByText('Lần đầu ghé thăm')).toBeTruthy()
})

test('polls once per 30 seconds only while visible and stops on unmount', async () => {
  vi.useFakeTimers()
  api.adminAnalyticsRealtime.mockResolvedValue({ status: 'LIVE', fetchedAt: new Date().toISOString(), metrics: { activeUsers: 1 }, trend: [] })
  const view = render(<RealtimeAnalytics lang="en" />)
  await act(async () => { await Promise.resolve() })
  expect(api.adminAnalyticsRealtime).toHaveBeenCalledTimes(1)
  expect(screen.getByText('Active visitors')).toBeTruthy()
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
