const api = {
  base: '/api',

  async request(method, path, body = null) {
    const headers = { 'Content-Type': 'application/json' };
    const token = Store.get('token');
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const opts = { method, headers };
    if (body) opts.body = JSON.stringify(body);

    const res = await fetch(`${this.base}${path}`, opts);

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
