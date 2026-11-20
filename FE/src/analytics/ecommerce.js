export function analyticsItem(product, quantity = 1, category, index) {
  const item = {
    item_id: String(product.productId ?? product.id),
    item_name: product.productNameVi ?? product.nameVi,
    quantity
  }
  if (product.sellingPrice != null) item.price = Number(product.sellingPrice)
  if (category) item.item_category = category
  if (index != null) item.index = index
  return item
}

export function whenVisible(element, callback) {
  if (!element) return undefined
  if (!('IntersectionObserver' in window)) { callback(); return undefined }
  try {
    const observer = new IntersectionObserver(entries => {
      if (entries.some(entry => entry.isIntersecting)) { observer.disconnect(); callback() }
    }, { threshold: 0.1 })
    observer.observe(element)
    return () => observer.disconnect()
  } catch { return undefined }
}
