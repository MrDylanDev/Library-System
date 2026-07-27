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

    const cancelBtn = h('button', {
      className: 'btn btn-outline',
      onClick: () => { document.body.removeChild(overlay); resolve(false); }
    }, 'Cancelar');

    const confirmBtn = h('button', {
      className: 'btn ' + btnClass,
      onClick: () => { document.body.removeChild(overlay); resolve(true); }
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
    // Clean up listener when modal closes
    const origCancel = cancelBtn.onClick;
    const origConfirm = confirmBtn.onClick;
    cancelBtn.onClick = (e) => {
      document.removeEventListener('keydown', handleKey);
      origCancel(e);
    };
    confirmBtn.onClick = (e) => {
      document.removeEventListener('keydown', handleKey);
      origConfirm(e);
    };
  });
}
