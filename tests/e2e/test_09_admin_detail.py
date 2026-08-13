"""Tests de acciones de admin desde el detalle del libro."""
from conftest import BASE_URL, login, should_see_text
from playwright.sync_api import Page


def test_admin_detail_shows_admin_actions(page: Page):
    """Un admin ve las acciones Editar, Eliminar y Perdido en el detalle de un libro disponible."""
    login(page, "admin@libromagico.com", "admin123")
    page.goto(f"{BASE_URL}/#/libros/9780132350884")  # Clean Code, DISPONIBLE
    page.wait_for_selector(".detail-grid", timeout=5000)

    assert page.locator("button:has-text('Editar')").count() > 0, "Debe verse el botón Editar"
    assert page.locator("button:has-text('Eliminar')").count() > 0, "Debe verse el botón Eliminar"
    assert page.locator("button:has-text('Perdido')").count() > 0, "Debe verse el botón Perdido"


def test_user_detail_hides_admin_actions(page: Page):
    """Un usuario común NO ve las acciones de admin en el detalle de un libro."""
    login(page, "usuario@libromagico.com", "usuario123")
    page.goto(f"{BASE_URL}/#/libros/9780132350884")
    page.wait_for_selector(".detail-grid", timeout=5000)

    assert page.locator("button:has-text('Prestar este libro')").count() > 0, \
        "Debe verse el botón Prestar este libro"
    assert page.locator("button:has-text('Editar')").count() == 0, \
        "El botón Editar no debe verse para un usuario común"
    assert page.locator("button:has-text('Eliminar')").count() == 0, \
        "El botón Eliminar no debe verse para un usuario común"
    assert page.locator("button:has-text('Perdido')").count() == 0, \
        "El botón Perdido no debe verse para un usuario común"


def test_admin_lost_book_hides_perdido_button(page: Page):
    """Un libro ya PERDIDO no ofrece el botón para marcarlo como perdido."""
    login(page, "admin@libromagico.com", "admin123")
    page.goto(f"{BASE_URL}/#/libros/9780201616224")  # The Pragmatic Programmer, PERDIDO
    page.wait_for_selector(".detail-grid", timeout=5000)

    should_see_text(page, "PERDIDO")
    assert page.locator("button:has-text('Editar')").count() > 0, \
        "Debe verse el botón Editar para un libro perdido"
    assert page.locator("button:has-text('Eliminar')").count() > 0, \
        "Debe verse el botón Eliminar para un libro perdido"
    assert page.locator("button:has-text('Perdido')").count() == 0, \
        "No debe ofrecerse marcar como perdido un libro que ya está perdido"