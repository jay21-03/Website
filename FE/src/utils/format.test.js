import { describe, expect, it } from 'vitest'
import { formatCurrencyVnd } from './format'
import { orderStatusLabel, paymentStatusLabel } from './status'

describe('order display utilities', () => {
  it('formats integer VND without decimal cents', () => expect(formatCurrencyVnd(123456)).toMatch(/123[.,]456\s?₫/))
  it('maps every order status', () => expect(['NEW', 'CONFIRMED', 'COMPLETED', 'CANCELLED'].map(status => orderStatusLabel(status))).toEqual(['Mới', 'Đã xác nhận', 'Hoàn thành', 'Đã hủy']))
  it('maps every payment status', () => expect(['PENDING', 'PAID', 'FAILED', 'CANCELLED', 'EXPIRED', 'REFUNDED'].map(status => paymentStatusLabel(status))).toHaveLength(6))
})
