export const GUEST_CART_KEY = 'bautruc.guest-cart.v1'
export const GUEST_CART_MAX_QUANTITY = 99

const emptyCart = () => ({ items: [], totalAmount: 0 })

function normalizeEntry(entry) {
  const productId = Number(entry?.productId)
  const quantity = Math.min(GUEST_CART_MAX_QUANTITY, Math.trunc(Number(entry?.quantity)))
  return Number.isSafeInteger(productId) && productId > 0 && Number.isSafeInteger(quantity) && quantity > 0
    ? { productId, quantity }
    : null
}

export function readGuestCart(storage = window.localStorage) {
  try {
    const parsed = JSON.parse(storage.getItem(GUEST_CART_KEY) || '[]')
    if (!Array.isArray(parsed)) return []
    const totals = new Map()
    parsed.forEach(value => {
      const entry = normalizeEntry(value)
      if (entry) totals.set(entry.productId, Math.min(GUEST_CART_MAX_QUANTITY, (totals.get(entry.productId) || 0) + entry.quantity))
    })
    return [...totals].map(([productId, quantity]) => ({ productId, quantity }))
  } catch {
    return []
  }
}

export function writeGuestCart(entries, storage = window.localStorage) {
  const normalized = entries.map(normalizeEntry).filter(Boolean)
  try {
    if (normalized.length) storage.setItem(GUEST_CART_KEY, JSON.stringify(normalized))
    else storage.removeItem(GUEST_CART_KEY)
  } catch {
    // Keep the in-memory cart usable when browser storage is unavailable.
  }
  return normalized
}

export function setGuestQuantity(entries, productId, quantity) {
  const id = Number(productId)
  const next = entries.filter(entry => Number(entry.productId) !== id)
  const normalized = normalizeEntry({ productId: id, quantity })
  if (normalized) next.push(normalized)
  return next
}

export function addGuestQuantity(entries, productId, amount = 1) {
  const current = entries.find(entry => Number(entry.productId) === Number(productId))?.quantity || 0
  return setGuestQuantity(entries, productId, current + amount)
}

export function productToGuestItem(product, quantity) {
  const productId = Number(product.productId ?? product.id)
  return {
    id: `guest-${productId}`,
    productId,
    nameVi: product.nameVi,
    nameEn: product.nameEn,
    thumbnailUrl: product.thumbnailUrl,
    basePrice: Number(product.basePrice),
    sellingPrice: Number(product.sellingPrice),
    quantity,
    availableQuantity: GUEST_CART_MAX_QUANTITY,
    lineTotal: Number(product.sellingPrice) * quantity,
    guest: true
  }
}

export function guestCartFromProducts(entries, products) {
  const byId = new Map(products.filter(Boolean).map(product => [Number(product.productId ?? product.id), product]))
  const items = entries.flatMap(entry => {
    const product = byId.get(Number(entry.productId))
    return product && product.status !== 'INACTIVE' ? [productToGuestItem(product, entry.quantity)] : []
  })
  return { items, totalAmount: items.reduce((total, item) => total + item.lineTotal, 0) }
}

export async function hydrateGuestCart(productLoader, storage = window.localStorage) {
  const entries = readGuestCart(storage)
  if (!entries.length) return emptyCart()
  const results = await Promise.allSettled(entries.map(entry => productLoader(entry.productId)))
  return guestCartFromProducts(entries, results.map(result => result.status === 'fulfilled' ? result.value : null))
}

export async function mergeGuestCart({ loadCart, addItem, storage = window.localStorage }) {
  let remaining = readGuestCart(storage)
  let cart = await loadCart()
  let merged = 0
  let failed = 0

  for (const entry of [...remaining]) {
    try {
      cart = await addItem(entry.productId, entry.quantity)
      remaining = remaining.filter(value => value.productId !== entry.productId)
      writeGuestCart(remaining, storage)
      merged += 1
    } catch {
      failed += 1
    }
  }
  return { cart, merged, failed }
}
