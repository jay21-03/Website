const messages = {
  CART_EMPTY: {
    vi: 'Giỏ hàng đang trống.',
    en: 'Your cart is empty.'
  },

  CART_INVALID: {
    vi: 'Giỏ hàng đã thay đổi. Vui lòng kiểm tra lại sản phẩm.',
    en: 'Your cart has changed. Please review the products.'
  },

  INVENTORY_INSUFFICIENT: {
    vi: 'Một hoặc nhiều sản phẩm không còn đủ số lượng.',
    en: 'One or more products no longer have enough stock.'
  },

  INVENTORY_STATE_CONFLICT: {
    vi: 'Tồn kho vừa thay đổi. Vui lòng kiểm tra lại giỏ hàng.',
    en: 'Inventory has changed. Please review your cart.'
  },

  PRODUCT_NOT_PURCHASABLE: {
    vi: 'Một sản phẩm trong giỏ không còn được bán.',
    en: 'A product in your cart is no longer available for purchase.'
  },

  CHECKOUT_IN_PROGRESS: {
    vi: 'Yêu cầu đặt hàng đang được xử lý. Vui lòng chờ.',
    en: 'Your checkout request is being processed. Please wait.'
  },

  CHECKOUT_FINALIZATION_PENDING: {
    vi: 'Đơn hàng đang được hoàn tất. Vui lòng kiểm tra danh sách đơn hàng.',
    en: 'Your order is being finalized. Please check your orders.'
  },

  ORDER_NOT_FOUND: {
    vi: 'Không tìm thấy đơn hàng.',
    en: 'Order not found.'
  },

  ORDER_NOT_OWNER: {
    vi: 'Bạn không có quyền xem đơn hàng này.',
    en: 'You do not have permission to view this order.'
  },

  VALIDATION_FAILED: {
    vi: 'Thông tin gửi lên chưa hợp lệ.',
    en: 'The submitted information is invalid.'
  }
}

function currentLanguage() {
  if (typeof document !== 'undefined') {
    if (document.documentElement.lang === 'en') {
      return 'en'
    }
  }

  if (typeof localStorage !== 'undefined') {
    if (localStorage.getItem('dxLang') === 'en') {
      return 'en'
    }
  }

  return 'vi'
}

export function localizedText(
  vi,
  en,
  lang = currentLanguage()
) {
  return lang === 'en' ? en : vi
}

export function apiErrorMessage(
  code,
  fallback,
  lang = currentLanguage()
) {
  return messages[code]?.[lang] ?? fallback
}