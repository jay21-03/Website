// Interim live function: keep www redirects and serve public SEO HTML from
// the existing backend. Reconcile with edge.yml in the complete SEO release.
import cf from 'cloudfront';
function handler(event) {
  var request = event.request;
  var uri = request.uri;
  var host = request.headers.host && request.headers.host.value;

  if (host && host.toLowerCase() === 'www.dangxembautruc.com') {
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
        location: { value: 'https://dangxembautruc.com' + uri + (parts.length ? '?' + parts.join('&') : '') }
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

  var lastSegment = uri.substring(uri.lastIndexOf('/') + 1);

  if (uri === '/' || lastSegment.indexOf('.') === -1) {
    request.uri = '/index.html';
  }

  return request;
}
