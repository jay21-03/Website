import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import process from 'node:process'
import { runInNewContext } from 'node:vm'
import { describe, expect, it } from 'vitest'

const template = readFileSync(resolve(process.cwd(), '../BE/infra/aws/edge.yml'), 'utf8')
const source = template.match(/ {6}FunctionCode: \|\r?\n([\s\S]*?)\r?\n {2}MainDistribution:/)[1]
  .split(/\r?\n/).map(line => line.replace(/^ {8}/, '')).join('\n')
const liveSource = readFileSync(resolve(process.cwd(), '../BE/infra/aws/seo-edge-live.js'), 'utf8')
const origins = []
const handler = runInNewContext(`${source.replace("import cf from 'cloudfront';", '')}; handler`, { cf: { selectRequestOriginById: id => origins.push(id) } })
const liveHandler = runInNewContext(`${liveSource.replace("import cf from 'cloudfront';", '')}; handler`, { cf: { selectRequestOriginById: id => origins.push(id) } })
const request = (uri, host = 'dangxembautruc.com', querystring = {}) => ({ request: { uri, headers: { host: { value: host } }, querystring } })

describe('CloudFront SEO routing', () => {
  it('redirects www and legacy paths in one 301 while preserving query values', () => {
    const response = handler(request('/policy', 'www.dangxembautruc.com', { utm_source: { value: 'facebook' }, tag: { multiValue: [{ value: 'one' }, { value: 'two' }] } }))
    expect(response.statusCode).toBe(301)
    expect(response.headers.location.value).toBe('https://dangxembautruc.com/chinh-sach?utm_source=facebook&tag=one&tag=two')
    expect(handler(request('/products')).headers.location.value).toBe('https://dangxembautruc.com/san-pham')
    for (const [oldPath, newPath] of Object.entries({
      '/products': '/san-pham', '/workshop': '/trai-nghiem', '/support': '/lien-he',
      '/policy': '/chinh-sach', '/about': '/ve-chung-toi', '/collections': '/san-pham'
    })) {
      for (const route of [handler, liveHandler]) {
        expect(route(request(oldPath)).statusCode).toBe(301)
        expect(route(request(oldPath)).headers.location.value).toBe(`https://dangxembautruc.com${newPath}`)
      }
    }
  })

  it('routes SEO HTML to the existing backend origin and preserves API and assets', () => {
    for (const uri of ['/trai-nghiem', '/products/4927', '/san-pham/binh-gom-trang-tri', '/san-pham/binh-gom-4927', '/blog/lang-gom-bau-truc-o-dau', '/sitemap.xml']) {
      expect(handler(request(uri)).uri).toBe(uri)
    }
    expect(origins).toEqual(Array(6).fill('BackendElasticBeanstalkOrigin'))
    expect(handler(request('/api/v1/products')).uri).toBe('/api/v1/products')
    expect(handler(request('/robots.txt')).uri).toBe('/robots.txt')
    for (const route of [handler, liveHandler]) {
      expect(route(request('/')).uri).toBe('/seo-home')
      expect(route(request('/admin')).uri).toBe('/index.html')
      expect(route(request('/orders/123')).uri).toBe('/index.html')
      expect(route(request('/assets/index-abc.js')).uri).toBe('/assets/index-abc.js')
      expect(route(request('/index.html')).uri).toBe('/index.html')
    }
  })

  it('returns a real 404 for unknown paths', () => {
    for (const route of [handler, liveHandler]) {
      for (const uri of ['/abc-not-found', '/unknown.html', '/products/not-a-number']) {
        const response = route(request(uri))
        expect(response.statusCode).toBe(404)
        expect(response.headers['x-robots-tag'].value).toBe('noindex')
      }
    }
  })
})
