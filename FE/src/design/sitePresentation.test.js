import { describe, expect, it } from 'vitest'
import { presentationDefaults, presentationText, presentationStyle, presentationMode, presentationPalette, presetColors, contrastText, visibleCheckoutFields } from './sitePresentation'

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

  it('keeps light and dark text and six colors independent', () => {
    const light = { ...presentationDefaults, pages: { brand: { name: { vi: 'Sáng', en: 'Light' } } }, colors: presetColors('clay') }
    const content = { ...light, dark: { pages: { brand: { name: { vi: 'Tối', en: 'Dark' } } }, colorPreset: 'night', fontPreset: 'editorial', colors: presetColors('night') } }
    expect(presentationText(presentationMode(content, 'light'), 'brand', 'name', 'vi')).toBe('Sáng')
    expect(presentationText(presentationMode(content, 'dark'), 'brand', 'name', 'vi')).toBe('Tối')
    expect(presentationPalette(presentationMode(content, 'light')).background).toBe('#FBF6EC')
    expect(presentationPalette(presentationMode(content, 'dark')).background).toBe('#1D1713')
    expect(contrastText('#D18B5A')).toBe('#1D1713')
    expect(contrastText('#315641')).toBe('#FFFFFF')
  })

  it('keeps storefront icon settings in the saved presentation with safe defaults', () => {
    expect(presentationText(null, 'home', 'trust1Icon', 'vi')).toBe('pot')
    expect(presentationText(null, 'brand', 'zaloLabel', 'vi')).toBe('Z')
    const saved = { pages: { home: { trust1Icon: { vi: 'heart', en: 'heart', color: '#9D4B2F' } }, brand: { chatIcon: { vi: 'bubble', en: 'bubble', color: '#315641' } } } }
    expect(presentationText(saved, 'home', 'trust1Icon', 'vi')).toBe('heart')
    expect(presentationText(saved, 'brand', 'chatIcon', 'en')).toBe('bubble')
    expect(contrastText(saved.pages.brand.chatIcon.color)).toBe('#FFFFFF')
  })
})
