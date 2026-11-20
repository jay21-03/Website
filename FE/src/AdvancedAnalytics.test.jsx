// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import App from './App'
import { ChatWidget } from './ChatWidget'
import { api } from './api'
import { trackAnalytics } from './analytics/Analytics'
import { GUEST_CART_KEY } from './services/guestCart'

const product = { id: 7, productId: 7, nameVi: 'Bình gốm', nameEn: 'Pottery vase', sellingPrice: 100000, basePrice: 100000, collectionId: 3, quantity: 2, lineTotal: 200000, availableQuantity: 10 }
const cart = { items: [{ ...product, id: 51 }], totalAmount: 200000 }
const page = { content: [product], totalElements: 1, totalPages: 1, page: 0, first: true, last: true }

vi.mock('./api', () => ({ api: {
  home: vi.fn(), sitePresentation: vi.fn(), products: vi.fn(), collections: vi.fn(),
  supportSettings: vi.fn(), me: vi.fn(), product: vi.fn(), cart: vi.fn(),
  updateCart: vi.fn(), removeCart: vi.fn(), chatSession: vi.fn(), chatSend: vi.fn()
} }))
vi.mock('./analytics/Analytics', async importOriginal => ({
  ...await importOriginal(), setAnalyticsIdentity: vi.fn(), trackAnalytics: vi.fn(() => true), useAnalyticsConsent: () => true
}))

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
  sessionStorage.clear()
  window.matchMedia = vi.fn(() => ({ matches: false, addListener: vi.fn(), removeListener: vi.fn(), addEventListener: vi.fn(), removeEventListener: vi.fn() }))
  window.scrollTo = vi.fn()
  Element.prototype.scrollIntoView = vi.fn()
  api.home.mockResolvedValue({})
  api.sitePresentation.mockResolvedValue({})
  api.products.mockResolvedValue(page)
  api.collections.mockResolvedValue([{ id: 3, nameVi: 'Bình', nameEn: 'Vases' }])
  api.supportSettings.mockResolvedValue({})
  api.me.mockRejectedValue(new Error('guest'))
  api.product.mockResolvedValue(product)
  api.cart.mockResolvedValue(cart)
  api.chatSession.mockResolvedValue({ conversation: null, messages: [] })
})
afterEach(() => cleanup())

function mount(path) {
  return render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}><MemoryRouter initialEntries={[path]}><App /></MemoryRouter></QueryClientProvider>)
}

const events = name => trackAnalytics.mock.calls.filter(([event]) => event === name)

describe('catalog and cart analytics', () => {
  it('tracks a guest add only when the stored quantity increases', async () => {
    localStorage.setItem(GUEST_CART_KEY, JSON.stringify([{ productId: 7, quantity: 99 }]))
    mount('/products/7')
    const add = await screen.findByRole('button', { name: 'Thêm vào giỏ hàng' })
    fireEvent.click(add)
    expect(events('add_to_cart')).toHaveLength(0)
    expect(JSON.parse(localStorage.getItem(GUEST_CART_KEY))[0].quantity).toBe(99)
    localStorage.setItem(GUEST_CART_KEY, JSON.stringify([{ productId: 7, quantity: 98 }]))
    fireEvent.click(add)
    expect(events('add_to_cart')).toHaveLength(1)
    expect(JSON.parse(localStorage.getItem(GUEST_CART_KEY))[0].quantity).toBe(99)
  })

  it('tracks managed featured products and their selection on the homepage', async () => {
    api.home.mockResolvedValue({ featuredProducts: [product, { ...product, id: 8 }, { ...product, id: 9 }] })
    mount('/')
    await waitFor(() => expect(events('view_item_list').some(([, params]) => params.item_list_id === 'home_featured')).toBe(true))
    fireEvent.click(document.querySelector('a[href="/products/7"]'))
    expect(events('select_item').some(([, params]) => params.item_list_id === 'home_featured')).toBe(true)
  })

  it('tracks a displayed list and selected product once, with real category metadata', async () => {
    mount('/products')
    await waitFor(() => expect(events('view_item_list')).toHaveLength(1))
    expect(events('view_item_list')[0][1].items[0]).toMatchObject({ item_id: '7', item_name: 'Bình gốm', item_category: 'Bình', price: 100000 })
    fireEvent.click(document.querySelector('.catalog-more'))
    expect(events('select_item')).toHaveLength(1)
    expect(events('select_item')[0][1].item_list_id).toBe('catalog')
  })

  it('debounces search and tracks completed results, user filters and sorting', async () => {
    mount('/products')
    await waitFor(() => expect(events('view_item_list')).toHaveLength(1))
    const search = document.querySelector('.filters input')
    fireEvent.change(search, { target: { value: 'g' } })
    fireEvent.change(search, { target: { value: 'gố' } })
    fireEvent.change(search, { target: { value: 'gốm' } })
    await waitFor(() => expect(events('view_search_results')).toHaveLength(1), { timeout: 2000 })
    expect(events('view_search_results')[0][1]).toEqual({ search_term: 'gốm' })
    expect(api.products.mock.calls.filter(([filters]) => filters?.keyword === 'gốm')).toHaveLength(1)
    fireEvent.click(document.querySelectorAll('.collection-tabs button')[1])
    expect(events('filter_products')[0][1]).toEqual({ filter_type: 'collection', filter_value: '3' })
    const minPrice = document.querySelectorAll('.filters input')[1]
    fireEvent.change(minPrice, { target: { value: '100000' } })
    fireEvent.blur(minPrice)
    expect(events('filter_products')[1][1]).toEqual({ filter_type: 'price', filter_value: 'active' })
    fireEvent.change(document.querySelectorAll('.filters select')[1], { target: { value: 'sellingPrice,asc' } })
    expect(events('sort_products')[0][1]).toEqual({ sort_order: 'sellingPrice,asc' })
  })

  it('does not send a personal search query even when its result is empty', async () => {
    api.products.mockImplementation(filters => Promise.resolve(filters?.keyword ? { ...page, content: [], totalElements: 0 } : page))
    mount('/products')
    await waitFor(() => expect(events('view_item_list')).toHaveLength(1))
    fireEvent.change(document.querySelector('.filters input'), { target: { value: 'guest@example.test' } })
    await waitFor(() => expect(api.products.mock.calls.some(([filters]) => filters?.keyword === 'guest@example.test')).toBe(true), { timeout: 2000 })
    expect(events('view_search_results')).toHaveLength(0)
  })

  it('tracks guest cart view, quantity reduction and deletion with actual removed values', async () => {
    localStorage.setItem(GUEST_CART_KEY, JSON.stringify([{ productId: 7, quantity: 2 }]))
    mount('/cart')
    await waitFor(() => expect(events('view_cart')).toHaveLength(1))
    fireEvent.click(screen.getByRole('button', { name: 'Giảm số lượng' }))
    expect(events('remove_from_cart')[0][1]).toMatchObject({ value: 100000, items: [{ item_id: '7', quantity: 1 }] })
    fireEvent.click(screen.getByRole('button', { name: 'Xóa' }))
    expect(events('remove_from_cart')).toHaveLength(2)
    expect(events('view_cart')).toHaveLength(1)
  })

  it('tracks authenticated cart reduction only after API success', async () => {
    api.me.mockResolvedValue({ id: 1, role: 'USER', fullName: 'Customer' })
    api.updateCart.mockResolvedValue({ items: [{ ...cart.items[0], quantity: 1, lineTotal: 100000 }], totalAmount: 100000 })
    api.removeCart.mockRejectedValue(new Error('failed'))
    mount('/cart')
    await waitFor(() => expect(events('view_cart')).toHaveLength(1))
    fireEvent.click(screen.getByRole('button', { name: 'Giảm số lượng' }))
    await waitFor(() => expect(events('remove_from_cart')).toHaveLength(1))
    fireEvent.click(screen.getByRole('button', { name: 'Xóa' }))
    await waitFor(() => expect(api.removeCart).toHaveBeenCalledTimes(1))
    expect(events('remove_from_cart')).toHaveLength(1)
  })
})

describe('chat analytics', () => {
  it('tracks opening and accepted messages without sending message text', async () => {
    api.chatSend.mockResolvedValue({ conversation: { id: 1 }, messages: [] })
    render(<QueryClientProvider client={new QueryClient()}><ChatWidget user={null} /></QueryClientProvider>)
    fireEvent.click(screen.getByRole('button', { name: 'Chat với cửa hàng' }))
    expect(events('chat_open')).toHaveLength(1)
    await waitFor(() => expect(api.chatSession).toHaveBeenCalled())
    fireEvent.change(screen.getByRole('textbox', { name: 'Tin nhắn' }), { target: { value: 'private message' } })
    fireEvent.click(screen.getByRole('button', { name: 'Gửi tin nhắn' }))
    await waitFor(() => expect(events('chat_message_sent')).toHaveLength(1))
    expect(JSON.stringify(events('chat_message_sent'))).not.toContain('private message')
  })

  it('does not report a failed message as sent', async () => {
    api.chatSend.mockRejectedValue(new Error('network failed'))
    render(<QueryClientProvider client={new QueryClient()}><ChatWidget user={null} /></QueryClientProvider>)
    fireEvent.click(screen.getByRole('button', { name: 'Chat với cửa hàng' }))
    await waitFor(() => expect(api.chatSession).toHaveBeenCalled())
    fireEvent.change(screen.getByRole('textbox', { name: 'Tin nhắn' }), { target: { value: 'private message' } })
    fireEvent.click(screen.getByRole('button', { name: 'Gửi tin nhắn' }))
    await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('network failed'))
    expect(events('chat_message_sent')).toHaveLength(0)
  })
})
