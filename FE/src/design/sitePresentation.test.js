import { describe, expect, it } from 'vitest'
import { presentationDefaults, presentationText, presentationStyle, visibleCheckoutFields } from './sitePresentation'

describe('site presentation fallback and checkout fields', () => {
  it('uses the configured language and falls back to the built-in page copy', () => {
    expect(presentationText(null, 'checkout', 'title', 'en')).toBe('Checkout')
    expect(presentationText({ pages: { checkout: { title: { vi: 'Đặt hàng mới', en: 'New order' } } } }, 'checkout', 'title', 'vi')).toBe('Đặt hàng mới')
  })

  it('keeps required checkout fields in the configured order when note is hidden', () => {
    const layout = { ...presentationDefaults.checkout, fieldOrder: ['address', 'note', 'email', 'phone', 'receiverName'], showNote: false, showEmail: false }
    expect(visibleCheckoutFields(layout)).toEqual(['address', 'phone', 'receiverName'])
  })

  it('applies a safe font and color only to the selected text field', () => {
    const content = { pages: { brand: { name: { vi: 'Đàng Xem', en: 'Dang Xem', font: 'editorial', color: '#563820' } } } }
    expect(presentationStyle(content, 'brand', 'name')).toEqual({ fontFamily: '"Lora", "Be Vietnam Pro", sans-serif', color: '#563820' })
    expect(presentationStyle(content, 'brand', 'navProducts')).toBeUndefined()
    expect(presentationStyle({ pages: { brand: { name: { font: 'unknown', color: 'red' } } } }, 'brand', 'name')).toEqual({ fontFamily: '"Playfair Display", "Be Vietnam Pro", sans-serif' })
  })
})
