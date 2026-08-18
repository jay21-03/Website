import { describe, expect, it } from 'vitest'
import { apiErrorMessage } from './apiError'

describe('business error mapping', () => {
  it('maps checkout inventory conflict', () => expect(apiErrorMessage('INVENTORY_INSUFFICIENT', 'fallback')).toContain('không còn đủ'))
  it('maps checkout inventory conflict in English', () => expect(apiErrorMessage('INVENTORY_INSUFFICIENT', 'fallback', 'en')).toContain('stock'))
  it('localizes checkout not found in Vietnamese', () => expect(apiErrorMessage('CHECKOUT_NOT_FOUND', 'fallback', 'vi')).toBe('Không tìm thấy phiên đặt hàng. Vui lòng thử đặt hàng lại.'))
  it('localizes order cancellation not allowed in Vietnamese', () => expect(apiErrorMessage('ORDER_CANCELLATION_NOT_ALLOWED', 'fallback', 'vi')).toBe('Không thể hủy đơn hàng với trạng thái thanh toán hiện tại.'))
  it('localizes refund not allowed in English', () => expect(apiErrorMessage('REFUND_NOT_ALLOWED', 'fallback', 'en')).toBe('A manual refund can only be recorded for a cancelled paid order.'))
  it('preserves an unknown backend message', () => expect(apiErrorMessage('UNKNOWN', 'Backend detail')).toBe('Backend detail'))
})
