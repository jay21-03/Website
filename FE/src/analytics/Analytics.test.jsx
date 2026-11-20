// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'

let analytics
const events = name => (window.dataLayer || []).filter(entry => entry[0] === 'event' && entry[1] === name)

beforeEach(async () => {
  vi.resetModules()
  vi.stubEnv('VITE_GA4_MEASUREMENT_ID', 'G-TEST')
  localStorage.clear()
  sessionStorage.clear()
  window.dataLayer = undefined
  window.gtag = undefined
  window.history.replaceState({}, '', '/')
  document.head.querySelectorAll('script[src*="googletagmanager.com"]').forEach(script => script.remove())
  analytics = await import('./Analytics')
})
afterEach(() => vi.unstubAllEnvs())

describe('consent gated analytics', () => {
  it('does not load GA or send events when consent is declined', () => {
    localStorage.setItem('dx.analytics.consent.v1', 'denied')
    analytics.setAnalyticsIdentity(null)
    expect(analytics.trackAnalytics('view_item')).toBe(false)
    expect(document.querySelector('script[src*="googletagmanager.com"]')).toBeNull()
  })

  it('excludes admins and presentation preview', () => {
    localStorage.setItem('dx.analytics.consent.v1', 'granted')
    analytics.setAnalyticsIdentity({ role: 'ADMIN' })
    expect(analytics.trackAnalytics('add_to_cart')).toBe(false)
    analytics.setAnalyticsIdentity(null)
    window.history.replaceState({}, '', '/?presentationPreview=1')
    expect(analytics.trackAnalytics('begin_checkout')).toBe(false)
  })

  it('tracks paid purchases once and never tracks pending payments', () => {
    localStorage.setItem('dx.analytics.consent.v1', 'granted')
    analytics.setAnalyticsIdentity(null)
    const order = { id: 42, orderCode: 'ORDER-42', paymentStatus: 'PENDING', totalAmount: 50000, items: [] }
    expect(analytics.trackPaidPurchase(order)).toBe(false)
    expect(analytics.trackPaidPurchase({ ...order, paymentStatus: 'PAID' })).toBe(true)
    expect(analytics.trackPaidPurchase({ ...order, paymentStatus: 'PAID' })).toBe(false)
    expect(events('purchase')).toHaveLength(1)
    expect(events('purchase')[0][2].transaction_id).toBe('ORDER-42')
  })

  it('tracks phone and Zalo clicks without blocking navigation or duplicating existing events', () => {
    localStorage.setItem('dx.analytics.consent.v1', 'granted')
    analytics.setAnalyticsIdentity(null)
    const { getByText } = render(<MemoryRouter><analytics.AnalyticsRoot><a href="tel:0123456789" onClick={event => event.preventDefault()}>Phone</a><a href="https://zalo.me/123" onClick={event => event.preventDefault()}>Zalo</a></analytics.AnalyticsRoot></MemoryRouter>)
    fireEvent.click(getByText('Phone'))
    fireEvent.click(getByText('Zalo'))
    expect(events('click_phone')).toHaveLength(1)
    expect(events('click_zalo')).toHaveLength(1)
    expect(getByText('Phone').getAttribute('href')).toBe('tel:0123456789')
    expect(events('page_view')).toHaveLength(1)
    for (const name of ['view_item', 'add_to_cart', 'begin_checkout']) expect(analytics.trackAnalytics(name)).toBe(true)
    for (const name of ['view_item', 'add_to_cart', 'begin_checkout']) expect(events(name)).toHaveLength(1)
  })
})
