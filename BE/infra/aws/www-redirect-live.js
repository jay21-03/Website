// Minimal patch for the currently deployed SPA function. Keep in sync with
// the www redirect in edge.yml when the complete SEO edge stack is released.
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

  var lastSegment = uri.substring(uri.lastIndexOf('/') + 1);

  if (uri === '/' || lastSegment.indexOf('.') === -1) {
    request.uri = '/index.html';
  }

  return request;
}
