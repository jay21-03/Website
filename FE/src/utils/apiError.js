const messages = {
  CART_EMPTY: 'Giỏ hàng đang trống.',
  CART_INVALID: 'Giỏ hàng đã thay đổi. Vui lòng kiểm tra lại sản phẩm.',
  INVENTORY_INSUFFICIENT: 'Một hoặc nhiều sản phẩm không còn đủ số lượng.',
  INVENTORY_STATE_CONFLICT: 'Tồn kho vừa thay đổi. Vui lòng kiểm tra lại giỏ hàng.',
  PRODUCT_NOT_PURCHASABLE: 'Một sản phẩm trong giỏ không còn được bán.',
  CHECKOUT_IN_PROGRESS: 'Yêu cầu đặt hàng đang được xử lý. Vui lòng chờ.',
  CHECKOUT_FINALIZATION_PENDING: 'Đơn hàng đang được hoàn tất. Vui lòng kiểm tra danh sách đơn hàng.',
  ORDER_NOT_FOUND: 'Không tìm thấy đơn hàng.',
  ORDER_NOT_OWNER: 'Bạn không có quyền xem đơn hàng này.',
  VALIDATION_FAILED: 'Thông tin gửi lên chưa hợp lệ.'
}

export const apiErrorMessage = (code, fallback) => messages[code] ?? fallback
