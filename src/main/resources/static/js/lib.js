const Router = {
  routes: [],
  notFound: null,

  register(pattern, handler) {
    const keys = [];
    const regex = pattern.replace(/:(\w+)/g, (_, key) => {
      keys.push(key);
      return '([^/]+)';
    });
    this.routes.push({ regex: new RegExp(`^${regex}$`), keys, handler });
  },

  navigate(path) {
    window.location.hash = path;
  },

  start() {
    window.addEventListener('hashchange', () => this._resolve());
    if (!window.location.hash) {
      window.location.hash = '#/';
    }
    this._resolve();
  },

  _resolve() {
    const hash = window.location.hash.slice(1) || '/';
    for (const route of this.routes) {
      const match = hash.match(route.regex);
      if (match) {
        const params = {};
        route.keys.forEach((key, i) => params[key] = match[i + 1]);
        route.handler(params);
        return;
      }
    }
    if (this.notFound) this.notFound();
  }
};

const Store = {
  _state: { user: null },
  _listeners: {},

  init() {
    this._state.user = JSON.parse(localStorage.getItem('user') || 'null');
  },

  async boot() {
    try {
      const res = await fetch('/api/auth/me', { credentials: 'include' });
      if (!res.ok) {
        this._state.user = null;
        localStorage.removeItem('user');
        this.set('user', null);
        return false;
      }
      const user = await res.json();
      this._state.user = user;
      localStorage.setItem('user', JSON.stringify(user));
      this.set('user', user);
      return true;
    } catch (err) {
      this._state.user = null;
      localStorage.removeItem('user');
      this.set('user', null);
      return false;
    }
  },

  get(key) { return this._state[key]; },
  get isAuthenticated() { return !!this._state.user; },
  get roles() { return this._state.user?.rol ? [this._state.user.rol] : []; },
  get hasRole() {
    return (...roles) => roles.some(r => this.roles.includes(r));
  },

  set(key, value) {
    this._state[key] = value;
    (this._listeners[key] || []).forEach(fn => fn(value));
  },

  onChange(key, fn) {
    (this._listeners[key] = this._listeners[key] || []).push(fn);
  },

  login(user) {
    localStorage.setItem('user', JSON.stringify(user));
    this.set('user', user);
  },

  async logout() {
    localStorage.removeItem('user');
    this.set('user', null);
    try {
      await api.post('/auth/logout');
    } catch (err) {
      // El cierre de sesión no debe bloquear la navegación.
    }
    Router.navigate('/login');
  }
};

function h(tag, attrs = {}, ...children) {
  const el = document.createElement(tag);
  const BOOL_ATTRS = ['disabled', 'required', 'readonly', 'checked', 'selected', 'hidden', 'multiple', 'autofocus'];
  Object.entries(attrs).forEach(([k, v]) => {
    if (k === 'className') el.className = v;
    else if (k === 'htmlContent') el.innerHTML = v;
    else if (k.startsWith('on') && typeof v === 'function') {
      el.addEventListener(k.slice(2).toLowerCase(), v);
    } else if (k === 'style' && typeof v === 'object') {
      Object.assign(el.style, v);
    } else if (BOOL_ATTRS.includes(k)) {
      if (v) el.setAttribute(k, '');
      else el.removeAttribute(k);
    } else {
      el.setAttribute(k, v);
    }
  });
  children.forEach(child => {
    if (typeof child === 'string') el.appendChild(document.createTextNode(child));
    else if (child instanceof Node) el.appendChild(child);
  });
  return el;
}

function render(container, element) {
  if (typeof container === 'string') container = document.querySelector(container);
  container.innerHTML = '';
  if (element) container.appendChild(element);
}

function showAlert(message, type = 'error') {
  const el = h('div', { className: `alert alert-${type}` }, message);
  return el;
}

function badge(text, color) {
  return h('span', { className: `badge badge-${color}` }, text);
}

function isEmpty(obj) {
  return !obj || (Array.isArray(obj) && obj.length === 0);
}
