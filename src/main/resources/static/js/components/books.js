async function CatalogPage() {
  const container = h('div');

  const header = h('div', { className: 'card-header' });
  header.appendChild(h('h2', {}, 'Catálogo de Libros'));

  const searchBar = h('div', { className: 'search-bar' });
  const searchInput = h('input', { type: 'text', placeholder: 'Buscar por título...' });
  searchBar.appendChild(searchInput);
  searchBar.appendChild(h('button', { className: 'btn btn-primary' }, 'Buscar'));

  const grid = h('div', { className: 'book-grid' });
  const alertContainer = h('div');

  async function loadBooks(query = '') {
    grid.innerHTML = '';
    try {
      const url = query ? `/libros/buscar?titulo=${encodeURIComponent(query)}` : '/libros';
      const books = await api.get(url);
      if (isEmpty(books)) {
        grid.appendChild(h('p', { className: 'empty-state' }, 'No se encontraron libros.'));
        return;
      }
      books.forEach(book => grid.appendChild(BookCard(book)));
    } catch (err) {
      render(alertContainer, showAlert(err.message, 'error'));
    }
  }

  searchBar.querySelector('button').addEventListener('click', () => loadBooks(searchInput.value.trim()));
  searchInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') loadBooks(searchInput.value.trim());
  });

  loadBooks();

  const card = h('div', { className: 'card' });
  card.appendChild(header);
  card.appendChild(searchBar);
  card.appendChild(alertContainer);
  card.appendChild(grid);
  container.appendChild(card);

  return container;
}

function BookCard(book) {
  const card = h('div', { className: 'card book-card' });

  const title = h('h3', {}, book.titulo);
  const author = h('div', { className: 'author' }, book.autor);
  const isbn = h('div', { className: 'author' }, `ISBN: ${book.isbn}`);
  const meta = h('div', { className: 'meta' });

  meta.appendChild(badge(book.estado === 'DISPONIBLE' ? 'Disponible' : 'Prestado',
    book.estado === 'DISPONIBLE' ? 'green' : 'red'));

  const actions = h('div', { className: 'actions' });
  actions.appendChild(h('button', {
    className: 'btn btn-outline btn-sm',
    onClick: () => Router.navigate(`/libros/${book.isbn}`),
  }, 'Detalle'));

  if (book.estado === 'DISPONIBLE') {
    actions.appendChild(h('button', {
      className: 'btn btn-primary btn-sm',
      onClick: async () => {
        try {
          const user = Store.get('user');
          await api.post('/prestamos', { usuarioId: user.id, libroIsbn: book.isbn });
          Router.navigate(`/libros/${book.isbn}`);
        } catch (err) {
          alert(err.message);
        }
      },
    }, 'Prestar'));
  }

  meta.appendChild(actions);

  card.appendChild(title);
  card.appendChild(author);
  card.appendChild(isbn);
  card.appendChild(meta);

  return card;
}

async function BookDetailPage(params) {
  const container = h('div');

  try {
    const book = await api.get(`/libros/${params.isbn}`);

    const card = h('div', { className: 'card' });
    const header = h('div', { className: 'card-header' });
    header.appendChild(h('h2', {}, book.titulo));

    const backLink = h('a', {
      href: '#/catalogo', className: 'btn btn-outline btn-sm',
      onClick: (e) => { e.preventDefault(); Router.navigate('/catalogo'); },
    }, 'Volver');
    header.appendChild(backLink);

    card.appendChild(header);

    const detail = h('div', { className: 'detail-grid' });
    const fields = [
      ['ISBN', book.isbn],
      ['Autor', book.autor],
      ['Categoría', book.categoria || 'Sin categoría'],
      ['Año', book.añoPub || '-'],
      ['Editorial', book.editorial || '-'],
      ['Copias', book.copiasDisponibles],
      ['Estado', book.estado],
    ];
    fields.forEach(([label, value]) => {
      const div = h('div', { className: 'detail-field' });
      div.appendChild(h('div', { className: 'label' }, label));
      div.appendChild(h('div', { className: 'value' }, String(value)));
      detail.appendChild(div);
    });

    card.appendChild(detail);

    const actions = h('div', { className: 'actions', style: { marginTop: '1rem' } });
    if (book.estado === 'DISPONIBLE') {
      actions.appendChild(h('button', {
        className: 'btn btn-primary',
        onClick: async () => {
          try {
            const user = Store.get('user');
            await api.post('/prestamos', { usuarioId: user.id, libroIsbn: book.isbn });
            Router.navigate('/mis-prestamos');
          } catch (err) { alert(err.message); }
        },
      }, 'Prestar este libro'));
    }
    if (Store.hasRole('LIBRARIAN', 'ADMIN')) {
      actions.appendChild(h('button', {
        className: 'btn btn-outline',
        onClick: () => Router.navigate(`/admin/libros/${book.isbn}/editar`),
      }, 'Editar'));
      actions.appendChild(h('button', {
        className: 'btn btn-danger',
        onClick: async () => {
          if (!confirm('¿Eliminar este libro?')) return;
          try {
            await api.del(`/libros/${book.isbn}`);
            Router.navigate('/catalogo');
          } catch (err) { alert(err.message); }
        },
      }, 'Eliminar'));
    }
    card.appendChild(actions);

    container.appendChild(card);
  } catch (err) {
    container.appendChild(showAlert(err.message, 'error'));
    container.appendChild(h('a', {
      href: '#/catalogo', className: 'btn btn-outline',
      onClick: (e) => { e.preventDefault(); Router.navigate('/catalogo'); },
    }, 'Volver al catálogo'));
  }

  return container;
}
