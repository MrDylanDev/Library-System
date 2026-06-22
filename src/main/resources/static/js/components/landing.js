function LandingPage() {
  const wrapper = h('div');

  const featuredBooks = [
    { isbn: '9780141439518', title: 'Orgullo y prejuicio', author: 'Jane Austen' },
    { isbn: '9788497592208', title: 'Cien años de soledad', author: 'Gabriel García Márquez' },
    { isbn: '9788437604947', title: 'Rayuela', author: 'Julio Cortázar' },
    { isbn: '9788420471839', title: 'La ciudad y los perros', author: 'Mario Vargas Llosa' },
    { isbn: '9789505111881', title: 'El Aleph', author: 'Jorge Luis Borges' },
    { isbn: '9788432216060', title: 'Crónica de una muerte anunciada', author: 'Gabriel García Márquez' },
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

  const heroInner = h('div', { className: 'hero-inner' });
  const heroText = h('div', { className: 'hero-text' });

  heroText.appendChild(h('p', { className: 'hero-eyebrow' }, 'Biblioteca'));
  heroText.appendChild(h('h1', { htmlContent: 'Libro<span class="hero-highlight">Mágico</span>' }));
  heroText.appendChild(h('p', { className: 'hero-subtitle' },
    'Gestioná los préstamos de tu biblioteca de forma simple. ' +
    'Registrá libros, llevá el control de quién se lleva qué, y olvidate de las planillas.'
  ));

  const heroActions = h('div', { className: 'hero-actions' });
  if (!Store.isAuthenticated) {
    heroActions.appendChild(h('a', {
      href: '#/registro', className: 'btn-hero-primary',
      onClick: (e) => { e.preventDefault(); Router.navigate('/registro'); },
    }, 'Crear cuenta'));
    heroActions.appendChild(h('a', {
      href: '#/login', className: 'btn-hero-ghost',
      onClick: (e) => { e.preventDefault(); Router.navigate('/login'); },
    }, 'Iniciar sesión'));
  } else {
    heroActions.appendChild(h('a', {
      href: '#/catalogo', className: 'btn-hero-primary',
      onClick: (e) => { e.preventDefault(); Router.navigate('/catalogo'); },
    }, 'Ver catálogo'));
  }
  heroText.appendChild(heroActions);
  heroInner.appendChild(heroText);

  const heroShelves = h('div', { className: 'hero-shelves' });
  const shelf = h('div', { className: 'hero-shelf' });
  featuredBooks.slice(0, 5).forEach(b => shelf.appendChild(bookCover(b.isbn, b.title)));
  heroShelves.appendChild(shelf);
  heroShelves.appendChild(h('div', { className: 'hero-shelf-shadow' }));
  heroInner.appendChild(heroShelves);

  hero.appendChild(heroInner);
  wrapper.appendChild(hero);

  // ══════ FEATURED BOOKS ══════
  const featured = h('div', { className: 'landing-section section-cream' });
  const fInner = h('div', { className: 'landing-inner' });
  fInner.appendChild(h('p', { className: 'section-eyebrow' }, 'Nuestro catálogo'));
  fInner.appendChild(h('h2', {}, 'Algunos de nuestros libros'));
  fInner.appendChild(h('p', { className: 'section-subtitle' },
    'Explorá la colección. Tenemos clásicos, contemporáneos y más.'
  ));

  const coversGrid = h('div', { className: 'covers-grid' });
  featuredBooks.forEach(b => {
    const item = h('div', { className: 'cover-item' });
    item.appendChild(bookCover(b.isbn, b.title));
    item.appendChild(h('p', { className: 'cover-title' }, b.title));
    item.appendChild(h('p', { className: 'cover-author' }, b.author));
    coversGrid.appendChild(item);
  });

  fInner.appendChild(coversGrid);
  featured.appendChild(fInner);
  wrapper.appendChild(featured);

  // ══════ SERVICES ══════
  const services = h('div', { className: 'landing-section' });
  const sInner = h('div', { className: 'landing-inner' });
  sInner.appendChild(h('p', { className: 'section-eyebrow' }, 'Servicios'));
  sInner.appendChild(h('h2', {}, 'Lo que ofrecemos a nuestros lectores'));
  sInner.appendChild(h('p', { className: 'section-subtitle' },
    'Todo lo necesario para que la biblioteca funcione, sin complicaciones.'
  ));

  const sGrid = h('div', { className: 'feature-grid' });
  const servicesData = [
    {
      icon: '&#128214;',
      title: 'Catálogo en línea',
      desc: 'Consultá si un libro está disponible antes de venir. Buscá por título, autor o categoría.',
    },
    {
      icon: '&#128218;',
      title: 'Préstamos',
      desc: 'Registramos cada préstamo con fecha de devolución. Simple y ordenado.',
    },
    {
      icon: '&#128221;',
      title: 'Devoluciones y multas',
      desc: 'Cuando un libro vuelve tarde, la multa se calcula automáticamente. Sin errores.',
    },
  ];

  servicesData.forEach(s => {
    const card = h('div', { className: 'feature-card' });
    card.appendChild(h('div', { className: 'feature-icon', htmlContent: s.icon }));
    card.appendChild(h('h3', {}, s.title));
    card.appendChild(h('p', { className: 'feature-desc' }, s.desc));
    sGrid.appendChild(card);
  });

  sInner.appendChild(sGrid);
  services.appendChild(sInner);
  wrapper.appendChild(services);

  // ══════ CTA ══════
  const cta = h('div', { className: 'landing-section section-cta' });
  const ctaInner = h('div', { className: 'landing-inner' });
  const ctaBox = h('div', { className: 'cta-box' });
  ctaBox.appendChild(h('h2', {}, '¿Tenés un carnet de la biblioteca?'));
  ctaBox.appendChild(h('p', {}, 'Entrá y empezá a explorar el catálogo.'));
  if (!Store.isAuthenticated) {
    ctaBox.appendChild(h('div', { className: 'cta-actions' },
      h('a', {
        href: '#/registro', className: 'btn-hero-primary',
        onClick: (e) => { e.preventDefault(); Router.navigate('/registro'); },
      }, 'Crear cuenta'),
      h('a', {
        href: '#/login', className: 'btn-hero-ghost btn-hero-ghost-dark',
        onClick: (e) => { e.preventDefault(); Router.navigate('/login'); },
      }, 'Iniciar sesión'),
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
  footerBrand.appendChild(h('p', {}, 'Sistema de gestión para bibliotecas.'));
  footerInner.appendChild(footerBrand);

  const footerLinks = h('div', { className: 'footer-links' });
  const col = h('div', { className: 'footer-col' });
  col.appendChild(h('strong', {}, 'Navegación'));
  col.appendChild(h('a', { href: '#/catalogo', onClick: (e) => { e.preventDefault(); Router.navigate('/catalogo'); } }, 'Catálogo'));
  col.appendChild(h('a', { href: '#/login', onClick: (e) => { e.preventDefault(); Router.navigate('/login'); } }, 'Iniciar sesión'));
  col.appendChild(h('a', { href: '#/registro', onClick: (e) => { e.preventDefault(); Router.navigate('/registro'); } }, 'Crear cuenta'));
  footerLinks.appendChild(col);
  footerInner.appendChild(footerLinks);

  footer.appendChild(footerInner);
  footer.appendChild(h('div', { className: 'footer-bottom' },
    h('p', {}, 'LibroMágico — Porque cada libro merece ser leído.')
  ));

  wrapper.appendChild(footer);

  return wrapper;
}
