const api = {
  base: '/api',

  timeoutMs: 15000,

  _csrfToken() {
    const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
    return match ? decodeURIComponent(match[1]) : null;
  },

  async request(method, path, body = null) {
    const headers = { 'Content-Type': 'application/json' };
    if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
      const token = this._csrfToken();
      if (token) headers['X-XSRF-TOKEN'] = token;
    }

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), this.timeoutMs);

    const opts = { method, headers, credentials: 'include', signal: controller.signal };
    if (body) opts.body = JSON.stringify(body);

    let res;
    try {
      res = await fetch(`${this.base}${path}`, opts);
    } catch (err) {
      if (err.name === 'AbortError') {
        throw new Error('La solicitud tardó demasiado. Intentá de nuevo.');
      }
      throw err;
    } finally {
      clearTimeout(timer);
    }

    if (res.status === 401) {
      Store.logout();
      throw new Error('Sesión expirada');
    }

    if (res.status === 204) return null;

    const data = await res.json();
    if (!res.ok) {
      const msg = data.error || data.message || 'Error inesperado';
      throw new Error(msg);
    }
    return data;
  },

  get(path) { return this.request('GET', path); },
  post(path, body) { return this.request('POST', path, body); },
  put(path, body) { return this.request('PUT', path, body); },
  del(path) { return this.request('DELETE', path); },
};
