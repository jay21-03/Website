import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import process from 'node:process'
import { runInNewContext } from 'node:vm'
import { describe, expect, it } from 'vitest'

const template = readFileSync(resolve(process.cwd(), '../BE/infra/aws/edge.yml'), 'utf8')
const source = template.match(/ {6}FunctionCode: \|\r?\n([\s\S]*?)\r?\n {2}MainDistribution:/)[1]
  .split(/\r?\n/).map(line => line.replace(/^ {8}/, '')).join('\n')
const handler = runInNewContext(`${source}; handler`)
const request = (uri, host = 'dangxembautruc.com', querystring = {}) => ({ request: { uri, headers: { host: { value: host } }, querystring } })

describe('CloudFront SEO routing', () => {
  it('redirects www and legacy paths in one 301 while preserving query values', () => {
    const response = handler(request('/policy', 'www.dangxembautruc.com', { utm_source: { value: 'facebook' }, tag: { multiValue: [{ value: 'one' }, { value: 'two' }] } }))
    expect(response.statusCode).toBe(301)
    expect(response.headers.location.value).toBe('https://dangxembautruc.com/chinh-sach?utm_source=facebook&tag=one&tag=two')
    expect(handler(request('/products')).headers.location.value).toBe('https://dangxembautruc.com/san-pham')
  })

  it('keeps valid SPA routes and backend requests', () => {
    expect(handler(request('/trai-nghiem')).uri).toBe('/index.html')
    expect(handler(request('/products/4927')).uri).toBe('/index.html')
    expect(handler(request('/san-pham/binh-gom-trang-tri')).uri).toBe('/index.html')
    expect(handler(request('/api/v1/products')).uri).toBe('/api/v1/products')
    expect(handler(request('/robots.txt')).uri).toBe('/robots.txt')
  })

  it('returns a real 404 for unknown paths', () => {
    const response = handler(request('/abc-not-found'))
    expect(response.statusCode).toBe(404)
    expect(response.headers['x-robots-tag'].value).toBe('noindex')
  })
})
