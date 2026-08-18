const messages = {
  // =========================
  // AUTH / USER
  // =========================

  AUTH_INVALID_GOOGLE_CREDENTIAL: {
    vi: 'Thông tin đăng nhập Google không hợp lệ.',
    en: 'Google sign-in credential is invalid.'
  },

  AUTH_TOKEN_MISSING: {
    vi: 'Bạn cần đăng nhập để tiếp tục.',
    en: 'Please sign in to continue.'
  },

  AUTH_TOKEN_INVALID: {
    vi: 'Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.',
    en: 'Your session is invalid. Please sign in again.'
  },

  AUTH_TOKEN_EXPIRED: {
    vi: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
    en: 'Your session has expired. Please sign in again.'
  },

  USER_NOT_FOUND: {
    vi: 'Không tìm thấy người dùng.',
    en: 'User not found.'
  },

  USER_BLOCKED: {
    vi: 'Tài khoản đã bị khóa.',
    en: 'Your account is blocked.'
  },

  USER_INACTIVE: {
    vi: 'Tài khoản hiện không hoạt động.',
    en: 'Your account is inactive.'
  },

  USER_INVALID_ROLE_TRANSITION: {
    vi: 'Không thể thay đổi vai trò người dùng theo cách này.',
    en: 'The user role cannot be changed this way.'
  },

  LAST_ADMIN_PROTECTED: {
    vi: 'Không thể khóa hoặc hạ quyền quản trị viên cuối cùng đang hoạt động.',
    en: 'The last active admin cannot be blocked or demoted.'
  },

  ACCESS_DENIED: {
    vi: 'Bạn không có quyền thực hiện thao tác này.',
    en: 'You do not have permission to perform this action.'
  },

  CSRF_INVALID: {
    vi: 'Yêu cầu bảo mật không hợp lệ. Vui lòng tải lại trang và thử lại.',
    en: 'Security token is invalid. Please reload the page and try again.'
  },

  UNAUTHORIZED: {
    vi: 'Bạn cần đăng nhập để thực hiện thao tác này.',
    en: 'You need to sign in to perform this action.'
  },

  FORBIDDEN: {
    vi: 'Bạn không có quyền thực hiện thao tác này.',
    en: 'You do not have permission to perform this action.'
  },


  // =========================
  // PRODUCT / COLLECTION / DISCOUNT / IMAGE
  // =========================

  PRODUCT_NOT_FOUND: {
    vi: 'Không tìm thấy sản phẩm.',
    en: 'Product not found.'
  },

  PRODUCT_NOT_PURCHASABLE: {
    vi: 'Sản phẩm hiện không thể mua.',
    en: 'This product is currently unavailable for purchase.'
  },

  COLLECTION_NOT_FOUND: {
    vi: 'Không tìm thấy bộ sưu tập.',
    en: 'Collection not found.'
  },

  COLLECTION_NOT_EMPTY: {
    vi: 'Không thể xóa bộ sưu tập vẫn còn sản phẩm.',
    en: 'Cannot delete a collection that still contains products.'
  },

  DISCOUNT_NOT_FOUND: {
    vi: 'Không tìm thấy chương trình giảm giá.',
    en: 'Discount not found.'
  },

  DISCOUNT_INVALID: {
    vi: 'Thông tin giảm giá không hợp lệ.',
    en: 'The discount configuration is invalid.'
  },

  PRODUCT_IMAGE_NOT_FOUND: {
    vi: 'Không tìm thấy hình ảnh sản phẩm.',
    en: 'Product image not found.'
  },

  PRODUCT_IMAGE_LIMIT_EXCEEDED: {
    vi: 'Mỗi sản phẩm chỉ được có tối đa 10 hình ảnh.',
    en: 'Each product can have at most 10 images.'
  },

  PRODUCT_IMAGE_EMPTY: {
    vi: 'Vui lòng chọn hình ảnh để tải lên.',
    en: 'Please choose an image to upload.'
  },

  PRODUCT_IMAGE_TOO_LARGE: {
    vi: 'Hình ảnh vượt quá dung lượng cho phép.',
    en: 'Image exceeds the allowed size.'
  },

  PRODUCT_IMAGE_TYPE_UNSUPPORTED: {
    vi: 'Định dạng hình ảnh không được hỗ trợ.',
    en: 'Image format is not supported.'
  },

  PRODUCT_IMAGE_SIGNATURE_INVALID: {
    vi: 'Nội dung hình ảnh không hợp lệ.',
    en: 'Image content is invalid.'
  },

  PRODUCT_IMAGE_ORDER_INVALID: {
    vi: 'Thứ tự hình ảnh không hợp lệ.',
    en: 'Image order is invalid.'
  },

  S3_UPLOAD_FAILED: {
    vi: 'Không thể tải hình ảnh lên kho lưu trữ. Vui lòng thử lại.',
    en: 'Unable to upload the image to storage. Please try again.'
  },


  // =========================
  // CART / INVENTORY
  // =========================

  CART_EMPTY: {
    vi: 'Giỏ hàng đang trống.',
    en: 'Your cart is empty.'
  },

  CART_INVALID: {
    vi: 'Giỏ hàng đã thay đổi. Vui lòng kiểm tra lại.',
    en: 'Your cart has changed. Please review it.'
  },

  CART_ITEM_NOT_FOUND: {
    vi: 'Không tìm thấy sản phẩm trong giỏ hàng.',
    en: 'Cart item not found.'
  },

  INVENTORY_NOT_FOUND: {
    vi: 'Không tìm thấy thông tin tồn kho.',
    en: 'Inventory information not found.'
  },

  INVENTORY_INSUFFICIENT: {
    vi: 'Một hoặc nhiều sản phẩm không còn đủ số lượng.',
    en: 'One or more products do not have enough stock.'
  },

  INVENTORY_INVALID_ADJUSTMENT: {
    vi: 'Điều chỉnh tồn kho không hợp lệ.',
    en: 'Inventory adjustment is invalid.'
  },

  INVENTORY_STATE_CONFLICT: {
    vi: 'Tồn kho vừa thay đổi. Vui lòng kiểm tra lại giỏ hàng.',
    en: 'Inventory has changed. Please review your cart.'
  },


  // =========================
  // CHECKOUT
  // =========================

  IDEMPOTENCY_KEY_REQUIRED: {
    vi: 'Thiếu mã định danh cho yêu cầu đặt hàng. Vui lòng thử lại.',
    en: 'Checkout request identifier is missing. Please try again.'
  },

  IDEMPOTENCY_KEY_REUSED: {
    vi: 'Yêu cầu đặt hàng này đã được dùng cho một lần xử lý khác.',
    en: 'This checkout request was already used for another operation.'
  },

  CHECKOUT_IN_PROGRESS: {
    vi: 'Yêu cầu đặt hàng đang được xử lý. Vui lòng chờ.',
    en: 'Your checkout request is being processed. Please wait.'
  },

  CHECKOUT_FINALIZATION_PENDING: {
    vi: 'Đơn hàng đang được hoàn tất. Vui lòng kiểm tra lại sau.',
    en: 'Your order is being finalized. Please check again shortly.'
  },

  CHECKOUT_NOT_FOUND: {
    vi: 'Không tìm thấy phiên đặt hàng. Vui lòng thử đặt hàng lại.',
    en: 'Checkout session not found. Please try checking out again.'
  },

  PAYOS_REQUEST_FAILED: {
    vi: 'Không thể tạo yêu cầu thanh toán. Vui lòng thử lại.',
    en: 'Unable to create the payment request. Please try again.'
  },


  // =========================
  // ORDER
  // =========================

  ORDER_NOT_FOUND: {
    vi: 'Không tìm thấy đơn hàng.',
    en: 'Order not found.'
  },

  ORDER_NOT_OWNER: {
    vi: 'Bạn không có quyền xem đơn hàng này.',
    en: 'You do not have permission to view this order.'
  },

  ORDER_INVALID_TRANSITION: {
    vi: 'Không thể chuyển đơn hàng sang trạng thái này.',
    en: 'The order cannot be changed to this status.'
  },

  ORDER_NOT_PAID: {
    vi: 'Đơn hàng chưa được thanh toán.',
    en: 'The order has not been paid.'
  },

  ORDER_CANCELLATION_NOT_ALLOWED: {
    vi: 'Không thể hủy đơn hàng với trạng thái thanh toán hiện tại.',
    en: 'The order cannot be cancelled with its current payment status.'
  },


  // =========================
  // PAYMENT
  // =========================

  PAYMENT_NOT_FOUND: {
    vi: 'Không tìm thấy thông tin thanh toán.',
    en: 'Payment information not found.'
  },

  PAYMENT_INVALID_STATE: {
    vi: 'Trạng thái thanh toán hiện tại không hợp lệ cho thao tác này.',
    en: 'The current payment status does not allow this action.'
  },

  PAYMENT_AMOUNT_MISMATCH: {
    vi: 'Số tiền thanh toán không khớp với đơn hàng.',
    en: 'Payment amount does not match the order.'
  },

  PAYMENT_EXTERNAL_ID_CONFLICT: {
    vi: 'Thông tin thanh toán bên ngoài đã được sử dụng.',
    en: 'External payment information is already in use.'
  },

  PAYOS_WEBHOOK_INVALID: {
    vi: 'Sự kiện thanh toán không hợp lệ.',
    en: 'Payment event is invalid.'
  },

  PAYMENT_LATE_SUCCESS_REQUIRES_REVIEW: {
    vi: 'Thanh toán đến muộn và cần quản trị viên kiểm tra.',
    en: 'Late payment success requires admin review.'
  },

  REFUND_NOT_ALLOWED: {
    vi: 'Chỉ có thể ghi nhận hoàn tiền cho đơn hàng đã hủy và đã thanh toán.',
    en: 'A manual refund can only be recorded for a cancelled paid order.'
  },


  // =========================
  // WORKSHOP / SUPPORT
  // =========================

  WORKSHOP_NOT_FOUND: {
    vi: 'Không tìm thấy workshop.',
    en: 'Workshop not found.'
  },

  WORKSHOP_NOT_BOOKABLE: {
    vi: 'Workshop hiện không thể đặt lịch.',
    en: 'This workshop is currently unavailable for booking.'
  },

  WORKSHOP_BOOKING_NOT_FOUND: {
    vi: 'Không tìm thấy lịch đặt workshop.',
    en: 'Workshop booking not found.'
  },

  WORKSHOP_INVALID_SCHEDULE: {
    vi: 'Thời gian đặt workshop không hợp lệ.',
    en: 'Workshop schedule is invalid.'
  },

  SUPPORT_SETTINGS_NOT_FOUND: {
    vi: 'Chưa có thông tin hỗ trợ.',
    en: 'Support information is not available.'
  },


  // =========================
  // NOTIFICATION / REPORT
  // =========================

  NOTIFICATION_NOT_FOUND: {
    vi: 'Không tìm thấy thông báo.',
    en: 'Notification not found.'
  },

  REPORT_DATE_RANGE_INVALID: {
    vi: 'Khoảng thời gian báo cáo không hợp lệ.',
    en: 'The report date range is invalid.'
  },


  // =========================
  // COMMON
  // =========================

  VALIDATION_FAILED: {
    vi: 'Thông tin gửi lên chưa hợp lệ.',
    en: 'The submitted information is invalid.'
  },

  RESOURCE_NOT_FOUND: {
    vi: 'Không tìm thấy dữ liệu yêu cầu.',
    en: 'Requested resource was not found.'
  },

  CONFLICT: {
    vi: 'Dữ liệu vừa thay đổi hoặc bị trùng. Vui lòng kiểm tra lại.',
    en: 'The data has changed or conflicts with existing data. Please review it.'
  },

  INTERNAL_ERROR: {
    vi: 'Hệ thống đang gặp lỗi. Vui lòng thử lại sau.',
    en: 'The system encountered an error. Please try again later.'
  }
}

function currentLanguage() {
  if (
    typeof document !== 'undefined' &&
    document.documentElement.lang === 'en'
  ) {
    return 'en'
  }

  if (
    typeof localStorage !== 'undefined' &&
    localStorage.getItem('dxLang') === 'en'
  ) {
    return 'en'
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
  const message = messages[code]

  if (!message) {
    return fallback
  }

  return message[lang] ?? message.vi ?? fallback
}
