function LoginPage() {
  const form = h('div', { className: 'auth-form' });

  const title = h('h1', {}, 'Iniciar Sesión');
  const alertContainer = h('div');
  const emailInput = h('input', { type: 'email', placeholder: 'correo@ejemplo.com', required: true });
  const passInput = h('input', { type: 'password', placeholder: 'Contraseña', required: true });
  const submitBtn = h('button', { className: 'btn btn-primary', style: { width: '100%' } }, 'Ingresar');

  function showError(msg) {
    render(alertContainer, showAlert(msg, 'error'));
  }

  submitBtn.addEventListener('click', async (e) => {
    e.preventDefault();
    submitBtn.disabled = true;
    submitBtn.textContent = 'Ingresando...';
    try {
      const data = await api.post('/auth/login', {
        email: emailInput.value,
        contrasena: passInput.value,
      });
      Store.login(data.token, { id: data.id, email: data.email, rol: data.rol });
      Router.navigate('/catalogo');
    } catch (err) {
      showError(err.message);
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Ingresar';
    }
  });

  form.appendChild(title);
  form.appendChild(alertContainer);
  form.appendChild(h('div', { className: 'form-group' }, h('label', {}, 'Email'), emailInput));
  form.appendChild(h('div', { className: 'form-group' }, h('label', {}, 'Contraseña'), passInput));
  form.appendChild(submitBtn);
  form.appendChild(h('div', { className: 'form-footer' },
    h('span', {}, '¿No tenés cuenta? '),
    h('a', { href: '#/registro', onClick: (e) => { e.preventDefault(); Router.navigate('/registro'); } }, 'Registrate')
  ));
  form.appendChild(h('div', { className: 'form-footer', style: { marginTop: '0.5rem' } },
    h('a', { href: '#/forgot-password', onClick: (e) => { e.preventDefault(); Router.navigate('/forgot-password'); } }, '¿Olvidaste tu contraseña?')
  ));

  return form;
}

function RegisterPage() {
  const form = h('div', { className: 'auth-form' });

  const title = h('h1', {}, 'Crear Cuenta');
  const alertContainer = h('div');

  const fields = [
    { name: 'nombre', type: 'text', placeholder: 'Nombre completo', required: true },
    { name: 'email', type: 'email', placeholder: 'correo@ejemplo.com', required: true },
    { name: 'contrasena', type: 'password', placeholder: 'Contraseña', required: true },
    { name: 'dni', type: 'text', placeholder: 'DNI (8 dígitos)', pattern: '\\d{8}', required: true },
    { name: 'telefono', type: 'tel', placeholder: '+5491123456789', required: true },
  ];

  const inputs = {};
  fields.forEach(f => {
    const input = h('input', {
      type: f.type, placeholder: f.placeholder,
      ...(f.required && { required: true }),
      ...(f.pattern && { pattern: f.pattern }),
    });
    inputs[f.name] = input;
    form.appendChild(h('div', { className: 'form-group' }, h('label', {}, f.placeholder), input));
  });

  const submitBtn = h('button', { className: 'btn btn-primary', style: { width: '100%' } }, 'Registrarse');

  function showError(msg) {
    render(alertContainer, showAlert(msg, 'error'));
  }

  submitBtn.addEventListener('click', async (e) => {
    e.preventDefault();
    submitBtn.disabled = true;
    submitBtn.textContent = 'Registrando...';
    try {
      const data = await api.post('/auth/register', {
        nombre: inputs.nombre.value,
        email: inputs.email.value,
        contrasena: inputs.contrasena.value,
        dni: inputs.dni.value,
        telefono: inputs.telefono.value,
      });
      Store.login(data.token, { id: data.id, email: data.email, rol: data.rol });
      Router.navigate('/catalogo');
    } catch (err) {
      showError(err.message);
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Registrarse';
    }
  });

  form.appendChild(title);
  form.appendChild(alertContainer);
  form.appendChild(submitBtn);
  form.appendChild(h('div', { className: 'form-footer' },
    h('span', {}, '¿Ya tenés cuenta? '),
    h('a', { href: '#/login', onClick: (e) => { e.preventDefault(); Router.navigate('/login'); } }, 'Iniciá sesión')
  ));

  return form;
}

function ForgotPasswordPage() {
  const form = h('div', { className: 'auth-form' });

  const title = h('h1', {}, 'Recuperar Contraseña');
  const subtitle = h('p', { style: { color: '#6b7280', marginBottom: '1.5rem' } }, 'Ingresá tu email y te enviaremos un enlace para restablecer tu contraseña.');
  const alertContainer = h('div');
  const emailInput = h('input', { type: 'email', placeholder: 'correo@ejemplo.com', required: true });
  const submitBtn = h('button', { className: 'btn btn-primary', style: { width: '100%' } }, 'Enviar enlace');

  submitBtn.addEventListener('click', async (e) => {
    e.preventDefault();
    submitBtn.disabled = true;
    submitBtn.textContent = 'Enviando...';
    try {
      const data = await api.post('/auth/forgot-password', { email: emailInput.value });
      render(alertContainer, showAlert(data.message, 'success'));
      submitBtn.style.display = 'none';
      emailInput.disabled = true;
    } catch (err) {
      render(alertContainer, showAlert(err.message, 'error'));
      submitBtn.disabled = false;
      submitBtn.textContent = 'Enviar enlace';
    }
  });

  form.appendChild(title);
  form.appendChild(subtitle);
  form.appendChild(alertContainer);
  form.appendChild(h('div', { className: 'form-group' }, h('label', {}, 'Email'), emailInput));
  form.appendChild(submitBtn);
  form.appendChild(h('div', { className: 'form-footer' },
    h('a', { href: '#/login', onClick: (e) => { e.preventDefault(); Router.navigate('/login'); } }, 'Volver al inicio de sesión')
  ));

  return form;
}

function ResetPasswordPage(params) {
  const form = h('div', { className: 'auth-form' });
  const token = params.token;

  const title = h('h1', {}, 'Nueva Contraseña');
  const alertContainer = h('div');
  const passInput = h('input', { type: 'password', placeholder: 'Nueva contraseña (mín 6 caracteres)', required: true, minLength: '6' });
  const confirmInput = h('input', { type: 'password', placeholder: 'Confirmar contraseña', required: true });
  const submitBtn = h('button', { className: 'btn btn-primary', style: { width: '100%' } }, 'Actualizar contraseña');

  submitBtn.addEventListener('click', async (e) => {
    e.preventDefault();
    if (passInput.value !== confirmInput.value) {
      render(alertContainer, showAlert('Las contraseñas no coinciden', 'error'));
      return;
    }
    if (passInput.value.length < 6) {
      render(alertContainer, showAlert('La contraseña debe tener al menos 6 caracteres', 'error'));
      return;
    }
    submitBtn.disabled = true;
    submitBtn.textContent = 'Actualizando...';
    try {
      const data = await api.post('/auth/reset-password', {
        token: token,
        newPassword: passInput.value,
      });
      render(alertContainer, showAlert(data.message, 'success'));
      submitBtn.style.display = 'none';
      passInput.disabled = true;
      confirmInput.disabled = true;
      setTimeout(() => Router.navigate('/login'), 2000);
    } catch (err) {
      render(alertContainer, showAlert(err.message, 'error'));
      submitBtn.disabled = false;
      submitBtn.textContent = 'Actualizar contraseña';
    }
  });

  form.appendChild(title);
  form.appendChild(alertContainer);
  form.appendChild(h('div', { className: 'form-group' }, h('label', {}, 'Nueva contraseña'), passInput));
  form.appendChild(h('div', { className: 'form-group' }, h('label', {}, 'Confirmar contraseña'), confirmInput));
  form.appendChild(submitBtn);

  return form;
}
