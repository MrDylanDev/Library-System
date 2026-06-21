function Navbar() {
  const nav = h('nav');

  function build() {
    nav.innerHTML = '';
    const isAuth = Store.isAuthenticated;
    const user = Store.get('user');
    const isAdmin = Store.hasRole('LIBRARIAN', 'ADMIN');

    const brand = h('a', { href: '#/', className: 'brand' }, 'LibroMagico');
    const links = h('div', { className: 'nav-links' });

    if (isAuth) {
      links.appendChild(h('a', { href: '#/catalogo' }, 'Catálogo'));
      links.appendChild(h('a', { href: '#/mis-prestamos' }, 'Mis Préstamos'));
      if (isAdmin) {
        links.appendChild(h('a', { href: '#/admin/usuarios' }, 'Usuarios'));
        links.appendChild(h('a', { href: '#/admin/libros/nuevo' }, 'Nuevo Libro'));
      }
      links.appendChild(h('span', { className: 'user-badge' }, user?.email || ''));
      links.appendChild(h('button', { onClick: () => Store.logout() }, 'Salir'));
    } else {
      links.appendChild(h('a', { href: '#/login' }, 'Iniciar Sesión'));
      links.appendChild(h('a', { className: 'btn btn-primary btn-sm', href: '#/registro' }, 'Registrarse'));
    }

    nav.appendChild(brand);
    nav.appendChild(links);
  }

  build();
  Store.onChange('token', () => build());
  Store.onChange('user', () => build());

  return nav;
}
