async function MisMultasPage() {
  const container = h('div');
  const alertContainer = h('div');

  const card = h('div', { className: 'card' });
  const header = h('div', { className: 'card-header' });
  header.appendChild(h('h2', {}, 'Mis Multas'));
  card.appendChild(header);
  card.appendChild(alertContainer);

  const tableContainer = h('div');
  card.appendChild(tableContainer);

  async function loadMultas() {
    tableContainer.innerHTML = '';
    render(alertContainer, null);
    try {
      const data = await api.get('/multas/mis-multas');
      const multas = data.content || [];

      if (multas.length === 0) {
        tableContainer.appendChild(
          h('div', { className: 'empty-state' }, 'No tenés multas pendientes.')
        );
        return;
      }

      const table = h('table');
      const thead = h('tr');
      ['Libro', 'Monto', 'Estado', 'Acción'].forEach(th =>
        thead.appendChild(h('th', {}, th)));
      table.appendChild(thead);

      multas.forEach(multa => {
        const tr = h('tr');
        tr.id = `multa-${multa.id}`;
        tr.appendChild(h('td', {}, multa.libroTitulo || '-'));
        tr.appendChild(h('td', {}, `$${multa.monto}`));

        const statusBadge = multa.estado === 'PENDIENTE'
          ? badge('Pendiente', 'yellow')
          : badge(multa.estado, 'green');
        tr.appendChild(h('td', {}, statusBadge));

        const actionTd = h('td');
        if (multa.estado === 'PENDIENTE') {
          actionTd.appendChild(h('button', {
            className: 'btn btn-success btn-sm',
            onClick: async () => {
              try {
                await api.put(`/multas/${multa.id}/pagar`);
                render(alertContainer, showAlert('Multa pagada', 'success'));
                loadMultas();
              } catch (err) {
                render(alertContainer, showAlert('No se pudo procesar el pago', 'error'));
              }
            },
          }, 'Pagar'));
        }
        tr.appendChild(actionTd);
        table.appendChild(tr);
      });

      tableContainer.appendChild(table);
    } catch (err) {
      render(alertContainer, showAlert(err.message, 'error'));
    }
  }

  loadMultas();
  container.appendChild(card);
  return container;
}
