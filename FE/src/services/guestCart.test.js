import { beforeEach, describe, expect, it } from 'vitest'
import { addGuestCartItem, clearGuestCart, loadGuestCart, updateGuestCartItem } from './guestCart'

const product = {
  id: 7,
  nameVi: 'Bình gốm',
  nameEn: 'Pottery vase',
  thumbnailUrl: '/vase.jpg',
  basePrice: 120000,
  sellingPrice: 90000
}

describe('guest cart', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('adds products to localStorage and totals the cart', () => {
    const cart = addGuestCartItem(loadGuestCart(), product)
    const stored = loadGuestCart()

    expect(cart.items).toHaveLength(1)
    expect(stored.items[0]).toMatchObject({ productId: 7, quantity: 1, lineTotal: 90000, guest: true })
    expect(stored.totalAmount).toBe(90000)
  })

  it('increments, updates, and removes guest cart items', () => {
    const added = addGuestCartItem(loadGuestCart(), product, 2)
    const incremented = addGuestCartItem(added, product, 1)
    const updated = updateGuestCartItem(incremented, 7, 2)
    const removed = updateGuestCartItem(updated, 7, 0)

    expect(incremented.items[0].quantity).toBe(3)
    expect(updated.items[0].lineTotal).toBe(180000)
    expect(removed.items).toHaveLength(0)
    expect(loadGuestCart().items).toHaveLength(0)
  })

  it('clears guest cart storage', () => {
    addGuestCartItem(loadGuestCart(), product)
    clearGuestCart()

    expect(loadGuestCart().items).toHaveLength(0)
  })
})
