import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { cwd } from 'node:process'
import { describe, expect, it } from 'vitest'

const projectFile = path => resolve(cwd(), path)

describe('brand favicon', () => {
  it('declares the brand icon files in the HTML entry', () => {
    const html = readFileSync(projectFile('index.html'), 'utf8')
    expect(html).toContain('/favicon.svg?v=1')
    expect(html).toContain('/favicon.ico?v=1')
    expect(html).toContain('/apple-touch-icon.png?v=1')
    expect(html).not.toMatch(/vite\.svg|react\.svg/i)
  })

  it('contains valid SVG, ICO and PNG signatures', () => {
    const svg = readFileSync(projectFile('public/favicon.svg'), 'utf8')
    const ico = readFileSync(projectFile('public/favicon.ico'))
    const png = readFileSync(projectFile('public/apple-touch-icon.png'))
    expect(svg).toContain('<svg')
    expect([...ico.subarray(0, 6)]).toEqual([0, 0, 1, 0, 1, 0])
    expect([...png.subarray(0, 8)]).toEqual([137, 80, 78, 71, 13, 10, 26, 10])
  })
})
