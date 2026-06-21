async function AdminUsersPage() {
  const container = h('div');
  const alertContainer = h('div');

  const card = h('div', { className: 'card' });
  const header = h('div', { className: 'card-header' });
  header.appendChild(h('h2', {}, 'Usuarios'));
  card.appendChild(header);
  card.appendChild(alertContainer);

  const tableContainer = h('div');
  card.appendChild(tableContainer);

  async function loadUsers() {
    tableContainer.innerHTML = '';
    try {
      const users = await api.get('/usuarios');
      if (isEmpty(users)) {
        tableContainer.appendChild(h('div', { className: 'empty-state' }, 'No hay usuarios.'));
        return;
      }

      const table = h('table');
      const thead = h('tr');
      ['ID', 'Nombre', 'Email', 'DNI', 'Rol', 'Estado'].forEach(th =>
        thead.appendChild(h('th', {}, th)));
      table.appendChild(thead);

      users.forEach(user => {
        const tr = h('tr');
        tr.appendChild(h('td', {}, String(user.id)));
        tr.appendChild(h('td', {}, user.nombre));
        tr.appendChild(h('td', {}, user.email));
        tr.appendChild(h('td', {}, user.dni || '-'));
        tr.appendChild(h('td', {}, badge(user.rol, user.rol === 'ADMIN' ? 'red' : 'blue')));
        tr.appendChild(h('td', {}, badge(user.estado, user.estado === 'ACTIVO' ? 'green' : 'red')));
        table.appendChild(tr);
      });

      tableContainer.appendChild(table);
    } catch (err) {
      render(alertContainer, showAlert(err.message, 'error'));
    }
  }

  loadUsers();
  container.appendChild(card);
  return container;
}

async function BookFormPage(params) {
  const isEdit = !!params.isbn;
  const container = h('div');
  const alertContainer = h('div');

  const card = h('div', { className: 'card' });
  const header = h('div', { className: 'card-header' });
  header.appendChild(h('h2', {}, isEdit ? 'Editar Libro' : 'Nuevo Libro'));
  card.appendChild(header);
  card.appendChild(alertContainer);

  const form = h('div');
  const fields = [
    { name: 'isbn', label: 'ISBN', type: 'text', required: true, disabled: isEdit },
    { name: 'titulo', label: 'Título', type: 'text', required: true },
    { name: 'autor', label: 'Autor', type: 'text', required: true },
    { name: 'categoria', label: 'Categoría', type: 'text' },
    { name: 'añoPub', label: 'Año de publicación', type: 'number' },
    { name: 'editorial', label: 'Editorial', type: 'text' },
    { name: 'copiasDisponibles', label: 'Copias disponibles', type: 'number', value: '1' },
  ];

  const inputs = {};
  fields.forEach(f => {
    const input = h('input', {
      type: f.type,
      value: f.value || '',
      required: f.required || false,
      disabled: f.disabled || false,
    });
    inputs[f.name] = input;
    form.appendChild(h('div', { className: 'form-group' }, h('label', {}, f.label), input));
  });

  if (isEdit) {
    try {
      const book = await api.get(`/libros/${params.isbn}`);
      inputs.isbn.value = book.isbn;
      inputs.titulo.value = book.titulo;
      inputs.autor.value = book.autor;
      inputs.categoria.value = book.categoria || '';
      inputs.añoPub.value = book.añoPub || '';
      inputs.editorial.value = book.editorial || '';
      inputs.copiasDisponibles.value = book.copiasDisponibles || 1;
      inputs.isbn.disabled = true;
    } catch (err) {
      render(alertContainer, showAlert(err.message, 'error'));
    }
  }

  const submitBtn = h('button', {
    className: 'btn btn-primary',
    style: { marginTop: '0.5rem' },
  }, isEdit ? 'Guardar Cambios' : 'Crear Libro');

  submitBtn.addEventListener('click', async (e) => {
    e.preventDefault();
    submitBtn.disabled = true;
    submitBtn.textContent = 'Guardando...';

    const body = {
      isbn: inputs.isbn.value,
      titulo: inputs.titulo.value,
      autor: inputs.autor.value,
      categoria: inputs.categoria.value || null,
      añoPub: inputs.añoPub.value ? parseInt(inputs.añoPub.value) : null,
      editorial: inputs.editorial.value || null,
      copiasDisponibles: parseInt(inputs.copiasDisponibles.value) || 1,
      estado: 'DISPONIBLE',
    };

    try {
      if (isEdit) {
        await api.put(`/libros/${params.isbn}`, body);
      } else {
        await api.post('/libros', body);
      }
      Router.navigate('/catalogo');
    } catch (err) {
      render(alertContainer, showAlert(err.message, 'error'));
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = isEdit ? 'Guardar Cambios' : 'Crear Libro';
    }
  });

  form.appendChild(submitBtn);

  const backBtn = h('a', {
    href: '#/catalogo', className: 'btn btn-outline',
    style: { marginTop: '0.5rem', marginLeft: '0.5rem' },
    onClick: (e) => { e.preventDefault(); Router.navigate('/catalogo'); },
  }, 'Cancelar');
  form.appendChild(backBtn);

  card.appendChild(form);
  container.appendChild(card);
  return container;
}
