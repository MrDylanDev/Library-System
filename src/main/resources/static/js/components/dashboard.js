async function AdminDashboardPage() {
  const container = h('div');

  try {
    const stats = await api.get('/admin/dashboard');

    const header = h('div', { className: 'card-header' });
    header.appendChild(h('h2', {}, 'Dashboard'));
    container.appendChild(header);

    const grid = h('div', { className: 'dashboard-grid' });

    const cards = [
      { label: 'Libros', value: stats.totalLibros, color: 'blue' },
      { label: 'Disponibles', value: stats.librosDisponibles, color: 'green' },
      { label: 'Prestados', value: stats.librosPrestados, color: 'yellow' },
      { label: 'Perdidos', value: stats.librosPerdidos, color: 'red' },
      { label: 'Usuarios', value: stats.totalUsuarios, color: 'purple' },
      { label: 'Préstamos activos', value: stats.prestamosActivos, color: 'blue' },
      { label: 'Préstamos atrasados', value: stats.prestamosAtrasados, color: 'red' },
      { label: 'Multas pendientes', value: stats.multasPendientes, color: 'yellow' },
      { label: 'Multas pagadas', value: stats.multasPagadas, color: 'green' },
    ];

    cards.forEach(c => {
      const card = h('div', { className: `dashboard-card dc-${c.color}` });
      card.appendChild(h('div', { className: 'dc-value' }, String(c.value)));
      card.appendChild(h('div', { className: 'dc-label' }, c.label));
      grid.appendChild(card);
    });

    container.appendChild(grid);
  } catch (err) {
    container.appendChild(showAlert(err.message, 'error'));
  }

  return container;
}
