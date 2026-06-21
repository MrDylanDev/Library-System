function LandingPage() {
  const wrapper = h('div');

  // ══════ HERO ══════
  const hero = h('div', { className: 'landing-hero' });

  const shapes = h('div', { className: 'hero-shapes', htmlContent: `
    <div class="hero-shape s1"></div>
    <div class="hero-shape s2"></div>
    <div class="hero-shape s3"></div>
    <div class="hero-shape s4"></div>
  `});
  hero.appendChild(shapes);

  const heroInner = h('div', { className: 'hero-inner' });
  const heroText = h('div', { className: 'hero-text' });

  heroText.appendChild(h('h1', { htmlContent: 'Tu biblioteca, <span class="hero-highlight">sin planillas</span>' }));
  heroText.appendChild(h('p', { className: 'hero-subtitle' },
    'Dejá atrás los papeles y las hojas de cálculo. Gestioná préstamos, ' +
    'devoluciones y multas desde una herramienta simple, hecha para tu día a día.'
  ));

  const heroActions = h('div', { className: 'hero-actions' });
  if (!Store.isAuthenticated) {
    heroActions.appendChild(h('a', {
      href: '#/registro', className: 'btn-hero-primary',
      onClick: (e) => { e.preventDefault(); Router.navigate('/registro'); },
    }, 'Crear cuenta', h('span', { htmlContent: ' &rarr;' })));
    heroActions.appendChild(h('a', {
      href: '#/login', className: 'btn-hero-ghost',
      onClick: (e) => { e.preventDefault(); Router.navigate('/login'); },
    }, 'Ya tengo cuenta'));
  } else {
    heroActions.appendChild(h('a', {
      href: '#/catalogo', className: 'btn-hero-primary',
      onClick: (e) => { e.preventDefault(); Router.navigate('/catalogo'); },
    }, 'Ir al catálogo', h('span', { htmlContent: ' &rarr;' })));
  }
  heroText.appendChild(heroActions);
  heroInner.appendChild(heroText);

  // Books illustration (CSS)
  const heroVisual = h('div', { className: 'hero-visual' });
  heroVisual.appendChild(h('div', { className: 'bookshelf', htmlContent: `
    <div class="book b1"><div class="book-spine"></div></div>
    <div class="book b2"><div class="book-spine"></div></div>
    <div class="book b3"><div class="book-spine"></div></div>
    <div class="book b4"><div class="book-spine"></div></div>
    <div class="book b5"><div class="book-spine"></div></div>
  `}));
  heroInner.appendChild(heroVisual);

  hero.appendChild(heroInner);
  wrapper.appendChild(hero);

  // ══════ FEATURES ══════
  const features = h('div', { className: 'landing-section' });
  const fInner = h('div', { className: 'landing-inner' });
  fInner.appendChild(h('p', { className: 'section-eyebrow' }, 'Lo que hacemos'));
  fInner.appendChild(h('h2', {}, 'Todo lo que necesitás para el día a día'));
  fInner.appendChild(h('p', { className: 'section-subtitle' },
    'Desde que un lector pide un libro hasta que lo devuelve. Simple y sin vueltas.'
  ));

  const fGrid = h('div', { className: 'feature-grid' });
  const featuresData = [
    {
      num: '01', emoji: '&#128218;', title: 'Catálogo',
      desc: 'Buscá por título, autor o ISBN. Sabé al instante si un libro está disponible o prestado.',
      items: ['Búsqueda rápida', 'Disponibilidad en tiempo real', 'Categorización'],
    },
    {
      num: '02', emoji: '&#128214;', title: 'Préstamos',
      desc: 'Registrá un préstamo en un clic. La fecha de devolución se calcula automáticamente.',
      items: ['Fecha automática', 'Registro de entregas', 'Historial por lector'],
    },
    {
      num: '03', emoji: '&#9200;', title: 'Multas',
      desc: 'Si un libro se devuelve tarde, la multa se calcula sola. Sin errores, sin discusiones.',
      items: ['Cálculo automático', 'Seguimiento de pagos', 'Control de morosos'],
    },
  ];

  featuresData.forEach(f => {
    const card = h('div', { className: 'feature-card ' + (f.num === '02' ? 'featured' : '') });
    const emoji = h('div', { className: 'feature-emoji', htmlContent: f.emoji });
    card.appendChild(emoji);
    card.appendChild(h('h3', {}, f.title));
    card.appendChild(h('p', { className: 'feature-desc' }, f.desc));
    const list = h('ul', { className: 'feature-list' });
    f.items.forEach(item => {
      list.appendChild(h('li', { htmlContent: '<span class="check">&#10003;</span> ' + item }));
    });
    card.appendChild(list);
    fGrid.appendChild(card);
  });

  fInner.appendChild(fGrid);
  features.appendChild(fInner);
  wrapper.appendChild(features);

  // ══════ HOW IT WORKS ══════
  const how = h('div', { className: 'landing-section section-dark' });
  const hInner = h('div', { className: 'landing-inner' });
  hInner.appendChild(h('p', { className: 'section-eyebrow dark-eyebrow' }, 'Así de simple'));
  hInner.appendChild(h('h2', { className: 'dark-title' }, 'Tres pasos y tu biblioteca ya funciona'));
  hInner.appendChild(h('p', { className: 'section-subtitle dark-subtitle' },
    'No necesitás saber de sistemas. Si sabés usar una biblioteca, ya sabés usar LibroMagico.'
  ));

  const steps = h('div', { className: 'steps' });
  const stepsData = [
    { step: '1', emoji: '&#128100;', title: 'Registrate', desc: 'Creá tu cuenta con nombre, email y DNI. En segundos ya estás adentro.' },
    { step: '2', emoji: '&#128190;', title: 'Cargá tus libros', desc: 'Agregalos por ISBN y completá autor, categoría y cantidad de copias.' },
    { step: '3', emoji: '&#128077;', title: 'Empezá a prestar', desc: 'Los lectores exploran el catálogo. Vos gestionás todo desde el panel.' },
  ];

  stepsData.forEach(s => {
    const card = h('div', { className: 'step-card' });
    const icon = h('div', { className: 'step-icon', htmlContent: s.emoji });
    card.appendChild(icon);
    card.appendChild(h('div', { className: 'step-badge' }, 'Paso ' + s.step));
    card.appendChild(h('h4', {}, s.title));
    card.appendChild(h('p', {}, s.desc));
    steps.appendChild(card);
  });

  hInner.appendChild(steps);
  how.appendChild(hInner);
  wrapper.appendChild(how);

  // ══════ CTA ══════
  const cta = h('div', { className: 'landing-section' });
  const ctaInner = h('div', { className: 'landing-inner' });
  const ctaBox = h('div', { className: 'cta-box' });
  ctaBox.appendChild(h('div', { className: 'cta-emoji', htmlContent: '&#128218;' }));
  ctaBox.appendChild(h('h2', {}, 'Tu biblioteca merece algo mejor que un Excel'));
  ctaBox.appendChild(h('p', {}, 'Probala. Sin costo. Sin compromiso. Sin instalar nada.'));
  if (!Store.isAuthenticated) {
    ctaBox.appendChild(h('a', {
      href: '#/registro', className: 'btn-hero-primary',
      onClick: (e) => { e.preventDefault(); Router.navigate('/registro'); },
    }, 'Empezar ahora'));
  } else {
    ctaBox.appendChild(h('a', {
      href: '#/catalogo', className: 'btn-hero-primary',
      onClick: (e) => { e.preventDefault(); Router.navigate('/catalogo'); },
    }, 'Ir al catálogo'));
  }
  ctaInner.appendChild(ctaBox);
  cta.appendChild(ctaInner);
  wrapper.appendChild(cta);

  // ══════ FOOTER ══════
  const footer = h('footer', { className: 'landing-footer' });
  const footerTop = h('div', { className: 'footer-top' });
  const footerBrand = h('div', { className: 'footer-brand' });
  footerBrand.appendChild(h('div', { className: 'footer-logo' }, 'LibroMagico'));
  footerBrand.appendChild(h('p', {}, 'Software de gestión de préstamos para bibliotecas.'));
  footerTop.appendChild(footerBrand);

  const footerLinks = h('div', { className: 'footer-links' });
  const col = h('div', { className: 'footer-col' });
  col.appendChild(h('strong', {}, 'Navegación'));
  col.appendChild(h('a', { href: '#/', onClick: (e) => e.preventDefault() }, 'Inicio'));
  col.appendChild(h('a', { href: '#/catalogo', onClick: (e) => { e.preventDefault(); Router.navigate('/catalogo'); } }, 'Catálogo'));
  col.appendChild(h('a', { href: '#/login', onClick: (e) => { e.preventDefault(); Router.navigate('/login'); } }, 'Iniciar sesión'));
  footerLinks.appendChild(col);
  footerTop.appendChild(footerLinks);
  footer.appendChild(footerTop);

  footer.appendChild(h('div', { className: 'footer-bottom' }, h('p', {},
    'Hecho con cuidado. Porque gestionar una biblioteca ya es bastante trabajo.'
  )));
  wrapper.appendChild(footer);

  return wrapper;
}
