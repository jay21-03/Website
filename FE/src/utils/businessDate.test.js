import { describe, expect, it } from 'vitest'
import { localBusinessDate, monthStartBusinessDate } from './businessDate'

describe('business date utilities', () => {
  it('does not convert local midnight to the previous UTC day', () => {
    const midnightVietnam = new Date('2026-07-31T17:00:00.000Z')
    expect(localBusinessDate(midnightVietnam)).toBe('2026-08-01')
  })

  it('computes month start from local calendar fields', () => {
    expect(monthStartBusinessDate(new Date('2026-08-16T17:00:00.000Z'))).toBe('2026-08-01')
  })
})
