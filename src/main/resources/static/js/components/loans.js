async function MyLoansPage() {
  const container = h('div');
  const alertContainer = h('div');

  const card = h('div', { className: 'card' });
  const header = h('div', { className: 'card-header' });
  header.appendChild(h('h2', {}, 'Mis Préstamos'));
  card.appendChild(header);
  card.appendChild(alertContainer);

  const tableContainer = h('div');
  card.appendChild(tableContainer);

  async function loadLoans() {
    tableContainer.innerHTML = '';
    try {
      const user = Store.get('user');
      const loans = await api.get(`/prestamos/usuarios/${user.id}`);
      if (isEmpty(loans)) {
        tableContainer.appendChild(h('div', { className: 'empty-state' }, 'No tenés préstamos.'));
        return;
      }

      const table = h('table');
      const thead = h('tr');
      ['Libro', 'Préstamo', 'Devolución esperada', 'Entrega real', 'Estado', 'Acción'].forEach(th =>
        thead.appendChild(h('th', {}, th)));
      table.appendChild(thead);

      loans.forEach(loan => {
        const tr = h('tr');
        tr.appendChild(h('td', {}, loan.libro?.titulo || '-'));
        tr.appendChild(h('td', {}, loan.fechaPrestamo));
        tr.appendChild(h('td', {}, loan.fechaDevolucion || '-'));
        tr.appendChild(h('td', {}, loan.fechaEntregaReal || 'Pendiente'));

        const statusBadge = loan.estado === 'ACTIVO' ? badge('Activo', 'blue')
          : loan.estado === 'DEVUELTO' ? badge('Devuelto', 'green')
          : badge(loan.estado, 'yellow');
        tr.appendChild(h('td', {}, statusBadge));

        const actionTd = h('td');
        if (loan.estado === 'ACTIVO') {
          actionTd.appendChild(h('button', {
            className: 'btn btn-success btn-sm',
            onClick: async () => {
              try {
                await api.put(`/prestamos/${loan.id}/devolucion`);
                loadLoans();
              } catch (err) { alert(err.message); }
            },
          }, 'Devolver'));
        }
        tr.appendChild(actionTd);
        table.appendChild(tr);
      });

      tableContainer.appendChild(table);
    } catch (err) {
      render(alertContainer, showAlert(err.message, 'error'));
    }
  }

  loadLoans();
  container.appendChild(card);
  return container;
}
