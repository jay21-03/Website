import { describe, expect, it } from 'vitest'
import { validateCheckout } from './checkout'

describe('checkout validation', () => {
  it('rejects missing required fields', () => {
    const result = validateCheckout({ receiverName: '', phone: '1', email: 'bad', address: '', note: '' })
    expect(result.data).toBeNull()
    expect(result.errors).toMatchObject({ receiverName: expect.any(String), phone: expect.any(String), email: expect.any(String), address: expect.any(String) })
  })

  it('returns English validation messages when lang is en', () => {
    const result = validateCheckout({ receiverName: '', phone: '1', email: 'bad', address: '', note: '' }, 'en')
    expect(result.errors.receiverName).toContain('full name')
    expect(result.errors.phone).toContain('8 characters')
    expect(result.errors.email).toContain('Invalid email')
    expect(result.errors.address).toContain('address')
  })

  it('returns the exact backend payload fields', () => {
    const values = { receiverName: 'Nguyễn An', phone: '0901234567', email: 'an@example.com', address: 'Ninh Phước', note: '' }
    expect(validateCheckout(values)).toEqual({ data: values, errors: {} })
  })
})
