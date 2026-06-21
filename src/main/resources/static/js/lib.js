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
  _state: { token: null, user: null },
  _listeners: {},

  init() {
    const token = localStorage.getItem('token');
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    this._state = { token, user };
  },

  get(key) { return this._state[key]; },
  get isAuthenticated() { return !!this._state.token; },
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

  login(token, user) {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
    this.set('token', token);
    this.set('user', user);
  },

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    this.set('token', null);
    this.set('user', null);
    Router.navigate('/login');
  }
};

function h(tag, attrs = {}, ...children) {
  const el = document.createElement(tag);
  Object.entries(attrs).forEach(([k, v]) => {
    if (k === 'className') el.className = v;
    else if (k === 'htmlContent') el.innerHTML = v;
    else if (k.startsWith('on') && typeof v === 'function') {
      el.addEventListener(k.slice(2).toLowerCase(), v);
    } else if (k === 'style' && typeof v === 'object') {
      Object.assign(el.style, v);
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
