"""Tests del catálogo: listado, búsqueda, detalle, préstamo."""
import pytest
from conftest import BASE_URL, login, should_see_text, should_not_see_text, wait_for_hash
from playwright.sync_api import Page


def test_catalog_shows_books(page: Page):
    """El catálogo público muestra libros."""
    page.goto(f"{BASE_URL}/#/catalogo")
    page.wait_for_timeout(2000)
    # Debe mostrar al menos los libros del seed
    should_see_text(page, "Clean Code")
    should_see_text(page, "Effective Java")


def test_catalog_search_by_title(page: Page):
    """Buscar por título filtra resultados."""
    page.goto(f"{BASE_URL}/#/catalogo")
    page.wait_for_timeout(1000)
    page.fill(".search-bar input", "Kubernetes")
    page.click(".search-bar button")
    page.wait_for_timeout(1500)
    should_see_text(page, "Kubernetes: Up and Running")


def test_catalog_search_enter_key(page: Page):
    """Presionar Enter en el campo de búsqueda también busca."""
    page.goto(f"{BASE_URL}/#/catalogo")
    page.wait_for_timeout(1000)
    page.fill(".search-bar input", "Python")
    page.press(".search-bar input", "Enter")
    page.wait_for_timeout(1500)
    should_see_text(page, "Fluent Python")
    should_see_text(page, "Learning Python")


def test_book_detail_from_catalog(page: Page):
    """Click en 'Detalle' de un libro abre la página de detalle."""
    page.goto(f"{BASE_URL}/#/catalogo")
    page.wait_for_timeout(2000)
    page.click("text=Detalle >> nth=0")
    page.wait_for_selector(".detail-grid")
    should_see_text(page, "ISBN")


def test_borrow_button_not_shown_anonymous(page: Page):
    """Sin autenticar, el botón 'Prestar' NO aparece en el catálogo."""
    page.goto(f"{BASE_URL}/#/catalogo")
    page.wait_for_timeout(2000)
    assert page.locator("button:has-text('Prestar')").count() == 0


def test_borrow_book_as_user(page: Page):
    """Usuario autenticado puede prestar un libro disponible."""
    login(page, "usuario@libromagico.com", "usuario123")
    page.goto(f"{BASE_URL}/#/catalogo")
    page.wait_for_timeout(2000)

    # Ir al detalle de un libro específico (evita race condition con otros tests)
    page.goto(f"{BASE_URL}/#/libros/9781617296208")  # Spring in Action
    page.wait_for_selector(".detail-grid", timeout=5000)

    prestar_btn = page.locator("button:has-text('Prestar este libro')")
    if prestar_btn.count() == 0:
        pytest.skip("Libro no disponible para préstamo")

    prestar_btn.click()
    page.wait_for_selector(".modal-dialog")
    page.click(".modal-dialog button:has-text('Confirmar')")
    page.wait_for_timeout(2000)
    # Debe redirigir a Mis Préstamos
    should_see_text(page, "Mis Préstamos")


def test_book_detail_shows_author_and_meta(page: Page):
    """El detalle del libro muestra todos los campos."""
    page.goto(f"{BASE_URL}/#/catalogo")
    page.wait_for_timeout(2000)

    # Click en Detalle del primer libro
    page.click("text=Detalle >> nth=0")
    page.wait_for_selector(".detail-grid")

    should_see_text(page, "ISBN")
    should_see_text(page, "Autor")
    should_see_text(page, "Categoria")
    should_see_text(page, "Estado")
    should_see_text(page, "Volver")


def test_catalog_search_by_author(page: Page):
    """Buscar por autor filtra resultados."""
    page.goto(f"{BASE_URL}/#/catalogo")
    page.wait_for_timeout(1000)
    page.fill(".search-bar input", "Kelsey Hightower")
    page.click(".search-bar button")
    page.wait_for_timeout(1500)
    should_see_text(page, "Kubernetes: Up and Running")
    should_not_see_text(page, "Clean Code")


def test_catalog_search_by_isbn(page: Page):
    """Buscar por ISBN encuentra el libro exacto."""
    page.goto(f"{BASE_URL}/#/catalogo")
    page.wait_for_timeout(1000)
    page.fill(".search-bar input", "9780134685991")
    page.click(".search-bar button")
    page.wait_for_timeout(1500)
    should_see_text(page, "Effective Java")


def test_catalog_search_no_results(page: Page):
    """Buscar sin resultados muestra mensaje."""
    page.goto(f"{BASE_URL}/#/catalogo")
    page.wait_for_timeout(1000)
    page.fill(".search-bar input", "xyzzy_nonexistent_99999")
    page.click(".search-bar button")
    page.wait_for_timeout(1500)
    should_see_text(page, "No se encontraron libros")


@pytest.mark.skip(reason="Requiere libro con estado PERDIDO en seed")
def test_borrow_lost_book_shows_error(page: Page):
    """Prestar un libro perdido debe mostrar error."""
    pass


def test_book_marked_as_borrowed_after_loan(page: Page):
    """Después de prestar un libro, aparece en Mis Préstamos."""
    login(page, "usuario@libromagico.com", "usuario123")

    # Usar un ISBN específico (evita colisiones entre tests)
    isbn = "9781492052203"  # Kubernetes: Up and Running
    page.goto(f"{BASE_URL}/#/libros/{isbn}")
    page.wait_for_selector(".detail-grid", timeout=5000)

    prestar_btn = page.locator("button:has-text('Prestar este libro')")
    if prestar_btn.count() == 0:
        pytest.skip("No hay libros disponibles")

    book_title = page.locator(".detail-grid").locator("text=Kubernetes: Up and Running")
    prestar_btn.click()
    page.wait_for_selector(".modal-dialog")
    page.click(".modal-dialog button:has-text('Confirmar')")
    page.wait_for_timeout(1500)

    # El libro ahora está en Mis Préstamos
    page.goto(f"{BASE_URL}/#/mis-prestamos")
    page.wait_for_timeout(2000)
    should_see_text(page, "Kubernetes: Up and Running")
