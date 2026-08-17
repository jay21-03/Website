import { describe, expect, it } from 'vitest'
import { apiErrorMessage } from './apiError'

describe('business error mapping', () => {
  it('maps checkout inventory conflict', () => expect(apiErrorMessage('INVENTORY_INSUFFICIENT', 'fallback')).toContain('không còn đủ'))
  it('maps checkout inventory conflict in English', () => expect(apiErrorMessage('INVENTORY_INSUFFICIENT', 'fallback', 'en')).toContain('stock'))
  it('preserves an unknown backend message', () => expect(apiErrorMessage('UNKNOWN', 'Backend detail')).toBe('Backend detail'))
})
