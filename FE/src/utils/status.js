const orderLabels = {
  NEW: ['Mới', 'New'], CONFIRMED: ['Đã xác nhận', 'Confirmed'],
  COMPLETED: ['Hoàn thành', 'Completed'], CANCELLED: ['Đã hủy', 'Cancelled']
}
const paymentLabels = {
  PENDING: ['Chờ thanh toán', 'Pending'], PAID: ['Đã thanh toán', 'Paid'],
  FAILED: ['Thất bại', 'Failed'], CANCELLED: ['Đã hủy', 'Cancelled'],
  EXPIRED: ['Hết hạn', 'Expired'], REFUNDED: ['Đã hoàn tiền', 'Refunded']
}

const label = (map, status, lang) => map[status]?.[lang === 'vi' ? 0 : 1] ?? status
export const orderStatusLabel = (status, lang = 'vi') => label(orderLabels, status, lang)
export const paymentStatusLabel = (status, lang = 'vi') => label(paymentLabels, status, lang)
