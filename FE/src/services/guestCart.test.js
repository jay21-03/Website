import { describe, expect, it, vi } from 'vitest'
import {
  GUEST_CART_KEY,
  addGuestQuantity,
  guestCartFromProducts,
  mergeGuestCart,
  readGuestCart,
  setGuestQuantity,
  writeGuestCart
} from './guestCart'

function memoryStorage(initial = {}) {
  const values = new Map(Object.entries(initial))
  return {
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: key => values.delete(key)
  }
}

describe('guest cart', () => {
  it('sanitizes, combines and updates persisted quantities', () => {
    const storage = memoryStorage({ [GUEST_CART_KEY]: JSON.stringify([{ productId: 10, quantity: 2 }, { productId: '10', quantity: 3 }, { productId: 'x', quantity: 1 }]) })
    const entries = readGuestCart(storage)
    expect(entries).toEqual([{ productId: 10, quantity: 5 }])
    expect(addGuestQuantity(entries, 10)).toEqual([{ productId: 10, quantity: 6 }])
    expect(setGuestQuantity(entries, 10, 0)).toEqual([])
  })

  it('calculates display totals from current product prices', () => {
    const cart = guestCartFromProducts([{ productId: 10, quantity: 2 }], [{ id: 10, nameVi: 'Bình', nameEn: 'Vase', basePrice: 250000, sellingPrice: 210000 }])
    expect(cart.totalAmount).toBe(420000)
    expect(cart.items[0]).toMatchObject({ productId: 10, quantity: 2, sellingPrice: 210000, guest: true })
  })

  it('removes only successfully validated items while merging', async () => {
    const storage = memoryStorage()
    writeGuestCart([{ productId: 10, quantity: 2 }, { productId: 11, quantity: 1 }], storage)
    const addItem = vi.fn(async productId => {
      if (productId === 11) throw new Error('Insufficient inventory')
      return { items: [{ productId: 10, quantity: 2 }], totalAmount: 420000 }
    })
    const result = await mergeGuestCart({ loadCart: async () => ({ items: [], totalAmount: 0 }), addItem, storage })
    expect(result).toMatchObject({ merged: 1, failed: 1 })
    expect(readGuestCart(storage)).toEqual([{ productId: 11, quantity: 1 }])
    expect(addItem).toHaveBeenCalledWith(10, 2)
  })
})
