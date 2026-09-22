const STORAGE_KEY = 'dangxem-guest-cart'
const DEFAULT_AVAILABLE_QUANTITY = 99

const emptyCart = () => ({ items: [], totalAmount: 0 })

function storage() {
  try {
    return window.localStorage
  } catch {
    return null
  }
}

function toPositiveInteger(value, fallback = 1) {
  const number = Number(value)
  return Number.isInteger(number) && number > 0 ? number : fallback
}

function maxQuantity(product) {
  const available = Number(product?.availableQuantity)
  return Number.isInteger(available) && available > 0 ? available : DEFAULT_AVAILABLE_QUANTITY
}

function toGuestItem(product, quantity = 1) {
  const id = toPositiveInteger(product?.productId || product?.id)
  const availableQuantity = maxQuantity(product)
  const sellingPrice = Number(product?.sellingPrice || product?.basePrice || 0)
  const safeQuantity = Math.min(toPositiveInteger(quantity), availableQuantity)
  return {
    id: `guest-${id}`,
    productId: id,
    nameVi: product?.nameVi || '',
    nameEn: product?.nameEn || product?.nameVi || '',
    thumbnailUrl: product?.thumbnailUrl || null,
    basePrice: Number(product?.basePrice || sellingPrice),
    sellingPrice,
    availableQuantity,
    quantity: safeQuantity,
    lineTotal: sellingPrice * safeQuantity,
    guest: true
  }
}

export function normalizeGuestCart(cart) {
  const items = Array.isArray(cart?.items)
    ? cart.items
        .filter(item => item?.productId || item?.id)
        .map(item => toGuestItem({ ...item, id: item.productId || item.id }, item.quantity))
        .filter(item => item.productId > 0 && item.sellingPrice >= 0)
    : []
  return { items, totalAmount: items.reduce((sum, item) => sum + item.lineTotal, 0) }
}

export function loadGuestCart() {
  const local = storage()
  if (!local) return emptyCart()
  try {
    return normalizeGuestCart(JSON.parse(local.getItem(STORAGE_KEY) || 'null'))
  } catch {
    return emptyCart()
  }
}

export function saveGuestCart(cart) {
  const normalized = normalizeGuestCart(cart)
  const local = storage()
  if (local) local.setItem(STORAGE_KEY, JSON.stringify(normalized))
  return normalized
}

export function clearGuestCart() {
  const local = storage()
  if (local) local.removeItem(STORAGE_KEY)
}

export function addGuestCartItem(cart, product, quantity = 1) {
  const normalized = normalizeGuestCart(cart)
  const productId = toPositiveInteger(product?.id)
  const index = normalized.items.findIndex(item => item.productId === productId)
  if (index < 0) {
    normalized.items.push(toGuestItem(product, quantity))
    return saveGuestCart(normalized)
  }
  const current = normalized.items[index]
  normalized.items[index] = toGuestItem({ ...product, id: productId }, current.quantity + toPositiveInteger(quantity))
  return saveGuestCart(normalized)
}

export function updateGuestCartItem(cart, productId, quantity) {
  const normalized = normalizeGuestCart(cart)
  const id = toPositiveInteger(productId)
  const requested = Number(quantity)
  if (!Number.isInteger(requested) || requested < 1) {
    return saveGuestCart({ items: normalized.items.filter(item => item.productId !== id) })
  }
  return saveGuestCart({
    items: normalized.items.map(item => item.productId === id ? toGuestItem(item, requested) : item)
  })
}
