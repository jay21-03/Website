const orderCodeFrom = message => /\bORD-\d{8}-\d{8}\b/.exec(message || '')?.[0]

export function formatNotification(item, lang = 'vi') {
  const fallback = { title: item?.title || '', message: item?.message || '' }
  if (!item || lang !== 'vi') return fallback

  const orderCode = orderCodeFrom(item.message)
  const product = item.referenceId != null ? `#${item.referenceId}` : null
  const count = item.metadata?.availableQuantity
  const quantity = Number.isInteger(count) && count >= 0 ? count : null

  switch (item.type) {
    case 'NEW_ORDER':
      return { title: 'Đơn hàng mới', message: orderCode ? `Đơn hàng ${orderCode} vừa được tạo.` : 'Có đơn hàng mới.' }
    case 'PAYMENT_SUCCESS':
      return { title: 'Thanh toán thành công', message: orderCode ? `Đã nhận thanh toán cho đơn ${orderCode}.` : 'Đã nhận thanh toán thành công.' }
    case 'PAYMENT_FAILED': {
      const reason = {
        FAILED: 'thất bại',
        CANCELLED: 'đã bị hủy',
        EXPIRED: 'đã hết hạn',
        REFUNDED: 'đã được hoàn tiền'
      }[item.metadata?.actualStatus] || 'không thành công'
      return { title: 'Thanh toán không thành công', message: orderCode ? `Thanh toán cho đơn ${orderCode} ${reason}.` : `Thanh toán ${reason}.` }
    }
    case 'LOW_STOCK':
      return {
        title: 'Sản phẩm sắp hết hàng',
        message: product
          ? quantity != null ? `Sản phẩm ${product} còn ${quantity} sản phẩm khả dụng.` : `Sản phẩm ${product} sắp hết hàng.`
          : 'Một sản phẩm sắp hết hàng.'
      }
    case 'OUT_OF_STOCK':
      return { title: 'Sản phẩm đã hết hàng', message: product ? `Sản phẩm ${product} đã hết hàng.` : 'Một sản phẩm đã hết hàng.' }
    default:
      return fallback
  }
}
