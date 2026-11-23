// Interim live function: keep www redirects and serve public SEO HTML from
// the existing backend. Reconcile with edge.yml in the complete SEO release.
import cf from 'cloudfront';
function handler(event) {
  var request = event.request;
  var uri = request.uri;
  var host = request.headers.host && request.headers.host.value;
  var redirects = {
    '/products': '/san-pham',
    '/workshop': '/trai-nghiem',
    '/support': '/lien-he',
    '/policy': '/chinh-sach',
    '/about': '/ve-chung-toi',
    '/collections': '/san-pham'
  };
  var canonical = redirects[uri.replace(/\/$/, '')] || uri;

  if ((host && host.toLowerCase() === 'www.dangxembautruc.com') || canonical !== uri) {
    var query = request.querystring || {};
    var parts = [];
    Object.keys(query).forEach(function (key) {
      var values = query[key].multiValue || [query[key]];
      values.forEach(function (entry) {
        parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(entry.value || ''));
      });
    });
    return {
      statusCode: 301,
      statusDescription: 'Moved Permanently',
      headers: {
        location: { value: 'https://dangxembautruc.com' + canonical + (parts.length ? '?' + parts.join('&') : '') }
      }
    };
  }

  if (
    uri === '/api' ||
    uri.indexOf('/api/') === 0 ||
    uri === '/actuator' ||
    uri.indexOf('/actuator/') === 0
  ) {
    return request;
  }

  if (
    uri === '/' ||
    uri === '/sitemap.xml' ||
    ['/san-pham', '/trai-nghiem', '/ve-chung-toi', '/lien-he', '/faq', '/chinh-sach', '/blog'].indexOf(uri) >= 0 ||
    /^\/san-pham\/[a-z0-9-]+$/.test(uri) ||
    /^\/blog\/[a-z0-9-]+$/.test(uri) ||
    /^\/products\/\d+$/.test(uri)
  ) {
    if (uri === '/') request.uri = '/seo-home';
    cf.selectRequestOriginById('BackendElasticBeanstalkOrigin');
    return request;
  }

  if (['/login', '/account', '/notifications', '/cart', '/checkout', '/orders', '/admin'].indexOf(uri) >= 0 ||
      /^\/orders\/\d+$/.test(uri)) {
    request.uri = '/index.html';
    return request;
  }

  if (uri === '/index.html' || uri === '/robots.txt' || uri === '/favicon.ico' ||
      uri === '/favicon.svg' || uri === '/apple-touch-icon.png' ||
      uri === '/google56aa56f6204ccb9e.html' || uri.indexOf('/assets/') === 0) return request;

  return {
    statusCode: 404,
    statusDescription: 'Not Found',
    headers: { 'content-type': { value: 'text/html; charset=utf-8' }, 'x-robots-tag': { value: 'noindex' } },
    body: '<!doctype html><html lang="vi"><meta charset="utf-8"><title>Kh&ocirc;ng t&igrave;m th&#7845;y trang</title><main><h1>Kh&ocirc;ng t&igrave;m th&#7845;y trang</h1><a href="/">V&#7873; trang ch&#7911;</a></main></html>'
  };
}
