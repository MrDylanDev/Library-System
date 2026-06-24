function LandingPage() {
  const wrapper = h('div');

  const featuredBooks = [
    { isbn: '9780141439518', title: 'Orgullo y prejuicio', author: 'Jane Austen' },
    { isbn: '9788497592208', title: 'Cien años de soledad', author: 'Gabriel García Márquez' },
    { isbn: '9788437604947', title: 'Rayuela', author: 'Julio Cortázar' },
    { isbn: '9788420471839', title: 'La ciudad y los perros', author: 'Mario Vargas Llosa' },
    { isbn: '9789505111881', title: 'El Aleph', author: 'Jorge Luis Borges' },
    { isbn: '9788432216060', title: 'Crónica de una muerte', author: 'Gabriel García Márquez' },
  ];

  function bookCover(isbn, title) {
    const cover = h('div', { className: 'book-cover' });
    const img = h('img', {
      src: `https://covers.openlibrary.org/b/isbn/${isbn}-M.jpg`,
      alt: title,
      loading: 'lazy',
    });
    img.addEventListener('error', () => {
      img.style.display = 'none';
      cover.classList.add('book-cover-fallback');
      cover.appendChild(h('span', { className: 'book-cover-title' }, title));
    });
    cover.appendChild(img);
    return cover;
  }

  // ══════ HERO ══════
  const hero = h('div', { className: 'landing-hero' });

  const heroBg = h('div', { className: 'hero-bg' });
  heroBg.appendChild(h('div', { className: 'hero-pattern' }));
  heroBg.appendChild(h('div', { className: 'hero-glow hg1' }));
  heroBg.appendChild(h('div', { className: 'hero-glow hg2' }));
  heroBg.appendChild(h('div', { className: 'hero-ornament ho1' }));
  heroBg.appendChild(h('div', { className: 'hero-ornament ho2' }));
  heroBg.appendChild(h('div', { className: 'hero-ornament ho3' }));
  hero.appendChild(heroBg);

  const heroInner = h('div', { className: 'hero-inner' });
  const heroText = h('div', { className: 'hero-text' });

  const badge = h('div', { className: 'hero-badge' });
  badge.appendChild(h('span', { className: 'badge-dot' }));
  badge.appendChild(h('span', {}, 'Sistema de gestión bibliotecaria'));
  heroText.appendChild(badge);

  heroText.appendChild(h('h1', { htmlContent: 'Libro<span class="hero-highlight">Mágico</span>' }));
  heroText.appendChild(h('p', { className: 'hero-subtitle' },
    'Todos tus libros, lectores y préstamos en un solo lugar. ' +
    'Dejá atrás las planillas y descubrí lo simple que puede ser gestionar tu biblioteca.'
  ));

  const heroActions = h('div', { className: 'hero-actions' });
  if (!Store.isAuthenticated) {
    heroActions.appendChild(h('a', {
      href: '#/registro', className: 'btn-hero-primary',
      onClick: (e) => { e.preventDefault(); Router.navigate('/registro'); },
    }, 'Comenzar ahora', h('span', { className: 'btn-arrow', htmlContent: '→' })));
    heroActions.appendChild(h('a', {
      href: '#/login', className: 'btn-hero-ghost',
      onClick: (e) => { e.preventDefault(); Router.navigate('/login'); },
    }, 'Iniciar sesión'));
  } else {
    heroActions.appendChild(h('a', {
      href: '#/catalogo', className: 'btn-hero-primary',
      onClick: (e) => { e.preventDefault(); Router.navigate('/catalogo'); },
    }, 'Ver catálogo', h('span', { className: 'btn-arrow', htmlContent: '→' })));
  }
  heroText.appendChild(heroActions);

  const heroStats = h('div', { className: 'hero-stats' });
  [
    { value: '+500', label: 'Libros' },
    { value: '200+', label: 'Lectores' },
    { value: '24/7', label: 'Disponible' },
  ].forEach(s => {
    heroStats.appendChild(h('div', { className: 'hero-stat' },
      h('span', { className: 'stat-value' }, s.value),
      h('span', { className: 'stat-label' }, s.label),
    ));
  });
  heroText.appendChild(heroStats);
  heroInner.appendChild(heroText);

  const heroShelves = h('div', { className: 'hero-shelves' });
  const shelf = h('div', { className: 'hero-shelf' });
  featuredBooks.slice(0, 5).forEach(b => shelf.appendChild(bookCover(b.isbn, b.title)));
  heroShelves.appendChild(shelf);
  heroShelves.appendChild(h('div', { className: 'hero-shelf-shadow' }));
  heroInner.appendChild(heroShelves);

  hero.appendChild(heroInner);
  wrapper.appendChild(hero);

  // ══════ FEATURED ══════
  const featured = h('div', { className: 'landing-section' });
  const fInner = h('div', { className: 'landing-inner' });
  fInner.appendChild(h('p', { className: 'section-eyebrow' }, 'Catálogo'));
  fInner.appendChild(h('h2', {}, 'Libros destacados'));
  fInner.appendChild(h('p', { className: 'section-subtitle' },
    'Una selección de nuestro catálogo. Clásicos, contemporáneos y más, esperando ser descubiertos.'
  ));

  const coversRow = h('div', { className: 'covers-row' });
  const coversTrack = h('div', { className: 'covers-track' });
  featuredBooks.forEach(b => {
    const item = h('div', { className: 'cover-item' });
    const coverWrapper = h('div', { className: 'cover-wrapper' });
    coverWrapper.appendChild(bookCover(b.isbn, b.title));
    coverWrapper.appendChild(h('div', { className: 'cover-shine' }));
    item.appendChild(coverWrapper);
    item.appendChild(h('p', { className: 'cover-title' }, b.title));
    item.appendChild(h('p', { className: 'cover-author' }, b.author));
    coversTrack.appendChild(item);
  });
  coversRow.appendChild(coversTrack);
  fInner.appendChild(coversRow);
  featured.appendChild(fInner);
  wrapper.appendChild(featured);

  // ══════ SERVICES ══════
  const services = h('div', { className: 'landing-section section-alt' });
  const sInner = h('div', { className: 'landing-inner' });
  sInner.appendChild(h('p', { className: 'section-eyebrow' }, 'Servicios'));
  sInner.appendChild(h('h2', {}, 'Todo lo que tu biblioteca necesita'));
  sInner.appendChild(h('p', { className: 'section-subtitle' },
    'Herramientas pensadas para el día a día de una biblioteca real.'
  ));

  const sGrid = h('div', { className: 'service-grid' });
  const servicesData = [
    {
      icon: h('svg', { width: '22', height: '22', viewBox: '0 0 24 24', fill: 'none', stroke: '#92400e', 'stroke-width': '2', htmlContent: '<path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>' }),
      title: 'Catálogo inteligente',
      desc: 'Buscá por título, autor o ISBN. Sabé al instante qué libros están disponibles y cuáles prestados.',
      color: 'sv1',
    },
    {
      icon: h('svg', { width: '22', height: '22', viewBox: '0 0 24 24', fill: 'none', stroke: '#6b21a8', 'stroke-width': '2', htmlContent: '<rect x="3" y="4" width="18" height="16" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>' }),
      title: 'Préstamos automáticos',
      desc: 'Registrá un préstamo en segundos. Las fechas de devolución se calculan solas, sin errores.',
      color: 'sv2',
    },
    {
      icon: h('svg', { width: '22', height: '22', viewBox: '0 0 24 24', fill: 'none', stroke: '#1e40af', 'stroke-width': '2', htmlContent: '<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/>' }),
      title: 'Control de multas',
      desc: 'Si un libro vuelve tarde, la multa se genera automáticamente. Llevá el control sin discusiones.',
      color: 'sv3',
    },
    {
      icon: h('svg', { width: '22', height: '22', viewBox: '0 0 24 24', fill: 'none', stroke: '#166534', 'stroke-width': '2', htmlContent: '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>' }),
      title: 'Gestión de lectores',
      desc: 'Registrá nuevos lectores, gestioná sus permisos y llevá el historial de cada uno.',
      color: 'sv4',
    },
  ];

  servicesData.forEach(s => {
    const card = h('div', { className: 'service-card ' + s.color });
    const iconWrap = h('div', { className: 'service-icon' });
    iconWrap.appendChild(s.icon);
    card.appendChild(iconWrap);
    const body = h('div', { className: 'service-body' });
    body.appendChild(h('h3', {}, s.title));
    body.appendChild(h('p', {}, s.desc));
    card.appendChild(body);
    sGrid.appendChild(card);
  });

  sInner.appendChild(sGrid);
  services.appendChild(sInner);
  wrapper.appendChild(services);

  // ══════ TESTIMONIALS ══════
  const testimonials = h('div', { className: 'landing-section' });
  const tInner = h('div', { className: 'landing-inner' });
  tInner.appendChild(h('p', { className: 'section-eyebrow' }, 'Testimonios'));
  tInner.appendChild(h('h2', {}, 'Lo que dicen nuestros bibliotecarios'));
  tInner.appendChild(h('p', { className: 'section-subtitle' },
    'Porque una herramienta se mide por quienes la usan todos los días.'
  ));

  const tGrid = h('div', { className: 'testimonial-grid' });
  [
    {
      quote: '"Antes llevaba todo en cuadernos. Ahora en dos clics sé quién tiene cada libro y cuándo debe devolverlo. Un alivio."',
      name: 'María Elena', role: 'Bibliotecaria escolar',
    },
    {
      quote: '"Lo que más valoro es lo simple que es. No tuve que aprender nada nuevo, es como usar la biblioteca de siempre pero mejor."',
      name: 'Carlos', role: 'Encargado de biblioteca popular',
    },
    {
      quote: '"Las multas automáticas me salvaron. Antes era un lío calcularlas, ahora el sistema lo hace solo y los lectores lo entienden mejor."',
      name: 'Laura', role: 'Bibliotecaria universitaria',
    },
  ].forEach(t => {
    const card = h('div', { className: 'testimonial-card' });
    card.appendChild(h('div', { className: 'testimonial-stars', htmlContent: '★★★★★' }));
    card.appendChild(h('p', { className: 'testimonial-quote' }, t.quote));
    card.appendChild(h('p', { className: 'testimonial-name' }, t.name));
    card.appendChild(h('p', { className: 'testimonial-role' }, t.role));
    tGrid.appendChild(card);
  });
  tInner.appendChild(tGrid);
  testimonials.appendChild(tInner);
  wrapper.appendChild(testimonials);

  // ══════ CTA ══════
  const cta = h('div', { className: 'landing-section section-cta' });
  const ctaInner = h('div', { className: 'landing-inner' });
  const ctaBox = h('div', { className: 'cta-box' });

  ctaBox.appendChild(h('div', { className: 'cta-decor' }));

  ctaBox.appendChild(h('h2', {}, '¿Listo para transformar tu biblioteca?'));
  ctaBox.appendChild(h('p', {}, 'Creá tu cuenta gratis y empezá a gestionar libros, préstamos y lectores sin complicaciones.'));

  if (!Store.isAuthenticated) {
    ctaBox.appendChild(h('div', { className: 'cta-actions' },
      h('a', {
        href: '#/registro', className: 'btn-hero-primary',
        onClick: (e) => { e.preventDefault(); Router.navigate('/registro'); },
      }, 'Crear cuenta gratis'),
      h('a', {
        href: '#/login', className: 'btn-hero-ghost btn-hero-ghost-dark',
        onClick: (e) => { e.preventDefault(); Router.navigate('/login'); },
      }, 'Ya tengo cuenta'),
    ));
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
  const footerInner = h('div', { className: 'footer-inner' });
  const footerBrand = h('div', { className: 'footer-brand' });
  footerBrand.appendChild(h('div', { className: 'footer-logo' }, 'LibroMágico'));
  footerBrand.appendChild(h('p', {}, 'Software de gestión para bibliotecas. Simple, rápido y sin papeles.'));
  footerInner.appendChild(footerBrand);

  const footerCols = h('div', { className: 'footer-cols' });
  [
    {
      title: 'Navegación',
      links: [
        { label: 'Catálogo', href: '#/catalogo' },
        { label: 'Iniciar sesión', href: '#/login' },
        { label: 'Crear cuenta', href: '#/registro' },
      ],
    },
    {
      title: 'Biblioteca',
      links: [
        { label: 'Libros', href: '#/catalogo' },
        { label: 'Préstamos', href: '#/mis-prestamos' },
        { label: 'Panel admin', href: '#/admin/usuarios' },
      ],
    },
  ].forEach(group => {
    const col = h('div', { className: 'footer-col' });
    col.appendChild(h('strong', {}, group.title));
    group.links.forEach(link => {
      col.appendChild(h('a', { href: link.href, onClick: (e) => { e.preventDefault(); Router.navigate(link.href.replace('#', '')); } }, link.label));
    });
    footerCols.appendChild(col);
  });
  footerInner.appendChild(footerCols);
  footer.appendChild(footerInner);

  const footerBottom = h('div', { className: 'footer-bottom' });
  footerBottom.appendChild(h('p', {}, '© 2026 LibroMágico. Todos los derechos reservados.'));
  footer.appendChild(footerBottom);

  wrapper.appendChild(footer);

  // Animations on scroll
  requestAnimationFrame(() => {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
        }
      });
    }, { threshold: 0.15 });

    wrapper.querySelectorAll('.service-card, .testimonial-card, .cover-item, .section-eyebrow, .landing-section h2, .section-subtitle')
      .forEach(el => observer.observe(el));
  });

  return wrapper;
}
