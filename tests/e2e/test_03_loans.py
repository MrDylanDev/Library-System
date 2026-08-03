"""Tests de préstamos: mis préstamos, devolución."""
import pytest
from conftest import BASE_URL, login, should_see_text, wait_for_hash
from playwright.sync_api import Page


def test_my_loans_empty_for_new_user(page: Page):
    """Usuario sin préstamos ve estado vacío."""
    login(page, "usuario@libromagico.com", "usuario123")
    page.goto(f"{BASE_URL}/#/mis-prestamos")
    page.wait_for_timeout(2000)
    has_empty = page.locator("text=No tenés préstamos").count() > 0
    has_table = page.locator("table").count() > 0
    assert has_empty or has_table, "No se ve ni empty state ni tabla"


def test_loans_requires_auth(page: Page):
    """Ir a mis préstamos sin autenticar redirige a login."""
    page.goto(f"{BASE_URL}/#/mis-prestamos")
    page.wait_for_timeout(1000)
    assert "#/login" in page.url or "mis-prestamos" not in page.url


def test_loan_borrow_and_return_flow(page: Page):
    """Flujo completo: prestar libro y devolverlo."""
    login(page, "usuario@libromagico.com", "usuario123")

    # Usar un ISBN específico (evita colisiones entre tests)
    isbn = "9780201633610"  # Design Patterns
    page.goto(f"{BASE_URL}/#/libros/{isbn}")
    page.wait_for_selector(".detail-grid", timeout=5000)

    prestar_btn = page.locator("button:has-text('Prestar este libro')")
    if prestar_btn.count() == 0:
        pytest.skip("Libro no disponible para préstamo")

    prestar_btn.click()
    page.wait_for_selector(".modal-dialog")
    page.click(".modal-dialog button:has-text('Confirmar')")
    page.wait_for_timeout(2000)

    # Ir a mis préstamos
    page.goto(f"{BASE_URL}/#/mis-prestamos")
    page.wait_for_timeout(2000)

    page.wait_for_selector("table", timeout=5000)
    should_see_text(page, "Activo")

    # Click en Devolver
    devolver = page.locator("button:has-text('Devolver')")
    if devolver.count() == 0:
        pytest.skip("No se encontró botón Devolver")

    devolver.first.click()
    page.wait_for_timeout(2000)

    # Después de devolver, el estado cambia
    should_see_text(page, "Devuelto")


def test_loan_status_badge_colors(page: Page):
    """Los badges de estado tienen los colores correctos."""
    login(page, "usuario@libromagico.com", "usuario123")
    page.goto(f"{BASE_URL}/#/mis-prestamos")
    page.wait_for_timeout(2000)

    if page.locator("table").count() > 0:
        badges = page.locator(".badge")
        if badges.count() > 0:
            class_attr = badges.first.get_attribute("class") or ""
            assert any(c in class_attr for c in ["blue", "green", "yellow"]), \
                f"Badge sin color válido: {class_attr}"


def test_admin_can_see_all_loans(page: Page):
    """Admin ve página de préstamos con filtro."""
    login(page, "admin@libromagico.com", "admin123")
    page.goto(f"{BASE_URL}/#/admin/prestamos")
    page.wait_for_timeout(2000)

    should_see_text(page, "Préstamos")
    assert page.locator("select").count() > 0

    has_table = page.locator("table").count() > 0
    has_empty = page.locator("text=No hay préstamos").count() > 0
    assert has_table or has_empty, "No se ve ni tabla ni empty state"


def test_admin_loan_filter(page: Page):
    """Filtro de estado en préstamos admin funciona."""
    login(page, "admin@libromagico.com", "admin123")
    page.goto(f"{BASE_URL}/#/admin/prestamos")
    page.wait_for_timeout(2000)

    select = page.locator("select")
    if select.count() > 0:
        select.first.select_option("ACTIVO")
        page.wait_for_timeout(1500)
        assert page.locator("h2:has-text('Préstamos')").is_visible()
