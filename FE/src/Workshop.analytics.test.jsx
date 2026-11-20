// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import App from './App'
import { api } from './api'
import { trackAnalytics } from './analytics/Analytics'

vi.mock('./api', () => ({ api: {
  home: vi.fn().mockResolvedValue({}), sitePresentation: vi.fn().mockResolvedValue({}),
  products: vi.fn().mockResolvedValue({ content: [] }), collections: vi.fn().mockResolvedValue([]),
  supportSettings: vi.fn().mockResolvedValue({}), me: vi.fn().mockRejectedValue(new Error('guest')),
  workshops: vi.fn().mockResolvedValue([]), workshopBooking: vi.fn()
} }))
vi.mock('./analytics/Analytics', () => ({ setAnalyticsIdentity: vi.fn(), trackAnalytics: vi.fn(), useAnalyticsConsent: () => false }))

beforeEach(() => {
  vi.clearAllMocks()
  api.workshops.mockResolvedValue([])
  window.matchMedia = vi.fn(() => ({ matches: false, addListener: vi.fn(), removeListener: vi.fn(), addEventListener: vi.fn(), removeEventListener: vi.fn() }))
  window.scrollTo = vi.fn()
})
afterEach(() => cleanup())

function mount() {
  return render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}><MemoryRouter initialEntries={['/workshop']}><App /></MemoryRouter></QueryClientProvider>)
}

async function submit() {
  const form = document.querySelector('.workshop-form')
  const fields = { fullName: 'Guest', email: 'guest@example.test', phone: '0123456789', preferredAt: '2026-12-20T10:00', participants: '2' }
  for (const [name, value] of Object.entries(fields)) fireEvent.change(form.elements.namedItem(name), { target: { value } })
  fireEvent.submit(form)
}

describe('workshop lead', () => {
  it('tracks a genuine offering selection without treating it as a booking', async () => {
    api.workshops.mockResolvedValue([{ id: 7, title: 'Gốm Chăm', description: '', priceAmount: 100000, durationMinutes: 60, maxParticipants: 5 }])
    mount()
    fireEvent.click(await screen.findByRole('button', { name: 'Chọn gói này' }))
    expect(trackAnalytics).toHaveBeenCalledWith('select_content', { content_type: 'workshop', content_id: 'workshop_7' })
    expect(trackAnalytics).not.toHaveBeenCalledWith('generate_lead', expect.anything())
  })

  it('tracks exactly once after the booking API succeeds', async () => {
    api.workshopBooking.mockResolvedValue({ id: 123 })
    mount()
    await submit()
    await waitFor(() => expect(trackAnalytics).toHaveBeenCalledWith('generate_lead', { lead_type: 'workshop_booking' }))
    expect(trackAnalytics).toHaveBeenCalledTimes(1)
    expect(api.workshopBooking).toHaveBeenCalledTimes(1)
    expect((await screen.findByRole('status')).textContent).toContain('123')
  })

  it('does not track a failed request', async () => {
    api.workshopBooking.mockRejectedValue(new Error('booking failed'))
    mount()
    await submit()
    await waitFor(() => expect(api.workshopBooking).toHaveBeenCalledTimes(1))
    expect(trackAnalytics).not.toHaveBeenCalled()
  })

  it('ignores another submit while the first booking is pending', async () => {
    let finish
    api.workshopBooking.mockImplementation(() => new Promise(resolve => { finish = resolve }))
    mount()
    await submit()
    await submit()
    expect(api.workshopBooking).toHaveBeenCalledTimes(1)
    finish({ id: 124 })
    await waitFor(() => expect(trackAnalytics).toHaveBeenCalledTimes(1))
  })
})
