// @vitest-environment jsdom
import { afterEach, expect, it, vi } from 'vitest'
import { analyticsItem, whenVisible } from './ecommerce'

afterEach(() => vi.unstubAllGlobals())

it('uses real product identifiers and the actual removed quantity', () => {
  expect(analyticsItem({ productId: 7, nameVi: 'Bình gốm', sellingPrice: 100000 }, 2, 'Bình', 3)).toEqual({ item_id: '7', item_name: 'Bình gốm', price: 100000, quantity: 2, item_category: 'Bình', index: 3 })
})

it('reports list impressions only after the list enters the viewport', () => {
  let notify
  const observer = { observe: vi.fn(), disconnect: vi.fn() }
  function MockObserver(callback) { notify = callback; Object.assign(this, observer) }
  vi.stubGlobal('IntersectionObserver', MockObserver)
  const track = vi.fn()
  const element = document.createElement('div')
  const stop = whenVisible(element, track)
  expect(observer.observe).toHaveBeenCalledWith(element)
  notify([{ isIntersecting: false }])
  expect(track).not.toHaveBeenCalled()
  notify([{ isIntersecting: true }])
  expect(track).toHaveBeenCalledTimes(1)
  expect(observer.disconnect).toHaveBeenCalledTimes(1)
  stop()
})
