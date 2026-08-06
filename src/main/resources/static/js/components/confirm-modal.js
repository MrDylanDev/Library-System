// confirm-modal.js — reusable confirmation modal
// Depends on: h() from lib.js

function showConfirm(title, message, confirmClass) {
  const btnClass = confirmClass || 'btn-danger';
  return new Promise(resolve => {
    const overlay = h('div', { className: 'modal-overlay' });
    const dialog = h('div', { className: 'modal-dialog' });

    const header = h('div', { className: 'modal-header' },
      h('h3', { className: 'modal-title' }, title)
    );

    const body = h('div', { className: 'modal-body' },
      h('p', {}, message)
    );

    const footer = h('div', { className: 'modal-footer' });

    function close(result) {
      document.removeEventListener('keydown', handleKey);
      if (overlay.parentNode) overlay.parentNode.removeChild(overlay);
      resolve(result);
    }

    const cancelBtn = h('button', {
      className: 'btn btn-outline',
      onClick: () => close(false)
    }, 'Cancelar');

    const confirmBtn = h('button', {
      className: 'btn ' + btnClass,
      onClick: () => close(true)
    }, 'Confirmar');

    footer.appendChild(cancelBtn);
    footer.appendChild(confirmBtn);

    dialog.appendChild(header);
    dialog.appendChild(body);
    dialog.appendChild(footer);
    overlay.appendChild(dialog);
    document.body.appendChild(overlay);

    // Focus confirm button
    setTimeout(() => confirmBtn.focus(), 50);

    // Keyboard
    function handleKey(e) {
      if (e.key === 'Escape') { cancelBtn.click(); }
      if (e.key === 'Enter' && !e.shiftKey) { confirmBtn.click(); }
    }
    document.addEventListener('keydown', handleKey);
  });
}
