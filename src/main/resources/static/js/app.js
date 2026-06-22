function App() {
  const content = document.getElementById('content');

  function guard(roles, fn) {
    return (params) => {
      if (!Store.isAuthenticated) {
        Router.navigate('/login');
        return;
      }
      if (roles.length > 0 && !Store.hasRole(...roles)) {
        const wrapper = h('div', { className: 'container' });
        wrapper.appendChild(showAlert('No tenés permisos para acceder a esta página.', 'error'));
        render(content, wrapper);
        return;
      }
      render(content, null);
      fn(params).then(el => {
        const wrapper = h('div', { className: 'container' });
        wrapper.appendChild(el);
        render(content, wrapper);
      });
    };
  }

  Router.register('/login', async () => {
    if (Store.isAuthenticated) Router.navigate('/catalogo');
    else render(content, LoginPage());
  });

  Router.register('/registro', async () => {
    if (Store.isAuthenticated) Router.navigate('/catalogo');
    else render(content, RegisterPage());
  });

  Router.register('/', async () => render(content, LandingPage()));

  Router.register('/catalogo', guard(['USER', 'LIBRARIAN', 'ADMIN'], CatalogPage));
  Router.register('/libros/:isbn', guard(['USER', 'LIBRARIAN', 'ADMIN'], BookDetailPage));
  Router.register('/mis-prestamos', guard(['USER', 'LIBRARIAN', 'ADMIN'], MyLoansPage));
  Router.register('/admin/libros/nuevo', guard(['LIBRARIAN', 'ADMIN'], BookFormPage));
  Router.register('/admin/libros/:isbn/editar', guard(['LIBRARIAN', 'ADMIN'], BookFormPage));
  Router.register('/admin/usuarios', guard(['LIBRARIAN', 'ADMIN'], AdminUsersPage));
  Router.register('/admin/multas', guard(['LIBRARIAN', 'ADMIN'], AdminMultasPage));

  Router.notFound = () => {
    const wrapper = h('div', { className: 'container' });
    wrapper.appendChild(h('div', { className: 'card', style: { textAlign: 'center' } },
      h('h2', {}, '404'),
      h('p', { style: { margin: '1rem 0' } }, 'Página no encontrada.'),
      h('a', { href: '#/', className: 'btn btn-primary', onClick: (e) => { e.preventDefault(); Router.navigate('/'); } }, 'Volver al inicio'),
    ));
    render(content, wrapper);
  };

  Router.start();
}

document.addEventListener('DOMContentLoaded', () => {
  Store.init();
  document.getElementById('navbar').appendChild(Navbar());
  App();
});
