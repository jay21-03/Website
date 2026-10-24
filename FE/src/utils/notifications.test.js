import { describe, expect, it } from 'vitest'
import { formatNotification } from './notifications'

it('formats new customer chat for Vietnamese admins', () => {
  expect(formatNotification({ type: 'CHAT_MESSAGE', title: 'New chat message', message: 'A customer sent a chat message.' }, 'vi'))
    .toEqual({ title: 'Tin nhắn khách hàng mới', message: 'Khách hàng vừa gửi tin nhắn chat.' })
})

const code = 'ORD-20261022-00000042'

describe('admin notification translations', () => {
  it('shows Vietnamese new-order notifications and preserves English', () => {
    const notification = { type: 'NEW_ORDER', title: 'New order', message: `Order ${code} was created.` }
    expect(formatNotification(notification, 'vi')).toEqual({ title: 'Đơn hàng mới', message: `Đơn hàng ${code} vừa được tạo.` })
    expect(formatNotification(notification, 'en')).toEqual({ title: notification.title, message: notification.message })
  })

  it('translates payment notifications', () => {
    expect(formatNotification({ type: 'PAYMENT_SUCCESS', message: `Payment received for ${code}.` }).message).toBe(`Đã nhận thanh toán cho đơn ${code}.`)
    expect(formatNotification({ type: 'PAYMENT_FAILED', message: `Payment for ${code} is EXPIRED.`, metadata: { actualStatus: 'EXPIRED' } }).message).toBe(`Thanh toán cho đơn ${code} đã hết hạn.`)
  })

  it('translates inventory notifications', () => {
    expect(formatNotification({ type: 'LOW_STOCK', referenceId: 5, metadata: { availableQuantity: 3 } }).message).toBe('Sản phẩm #5 còn 3 sản phẩm khả dụng.')
    expect(formatNotification({ type: 'LOW_STOCK', referenceId: 5 }).message).toBe('Sản phẩm #5 sắp hết hàng.')
    expect(formatNotification({ type: 'OUT_OF_STOCK', referenceId: 5 }).message).toBe('Sản phẩm #5 đã hết hàng.')
  })

  it('handles missing and unknown notification data', () => {
    expect(formatNotification({ type: 'OTHER', title: 'Custom', message: 'Source text' })).toEqual({ title: 'Custom', message: 'Source text' })
    expect(formatNotification({ type: 'NEW_ORDER', message: 'Older message' }).message).toBe('Có đơn hàng mới.')
  })
})
