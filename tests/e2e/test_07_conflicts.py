"""Tests de conflictos y errores: préstamos duplicados, ISBN duplicado, etc."""
import pytest
from conftest import BASE_URL, login, should_see_text
from playwright.sync_api import Page


def test_borrow_same_book_twice_shows_error(page: Page):
    """Prestar el mismo libro dos veces muestra error (sin redirigir)."""
    login(page, "usuario@libromagico.com", "usuario123")
    page.goto(f"{BASE_URL}/#/catalogo")
    page.wait_for_selector(".book-card", timeout=5000)

    prestar_btns = page.locator("button:has-text('Prestar')")
    if prestar_btns.count() == 0:
        pytest.skip("No hay libros disponibles para prestar")

    # Agarrar título del primer libro disponible
    first_card = page.locator(".book-card").first
    book_title = first_card.locator("h3").text_content() or ""

    # Click en Detalle para ir a la página de detalle
    first_card.locator("button:has-text('Detalle')").click()
    page.wait_for_selector(".detail-grid", timeout=5000)

    # Capturar ISBN desde la URL del detalle
    isbn = page.url.split("/")[-1]

    # Prestar desde la página de detalle
    prestar_btn = page.locator("button:has-text('Prestar este libro')")
    if prestar_btn.count() == 0:
        pytest.skip("Libro no disponible para préstamo")

    prestar_btn.click()
    page.wait_for_selector(".modal-dialog")
    page.click(".modal-dialog button:has-text('Confirmar')")
    # Debe redirigir a Mis Préstamos
    page.wait_for_timeout(2000)

    # Volver al detalle del mismo libro (ISBN ya capturado arriba)
    page.goto(f"{BASE_URL}/#/libros/{isbn}")
    page.wait_for_selector(".detail-grid", timeout=5000)

    # Intentar prestar de nuevo
    prestar_btn2 = page.locator("button:has-text('Prestar este libro')")
    if prestar_btn2.count() == 0:
        pytest.skip("Botón Prestar no visible (quizás ya sin copias)")

    prestar_btn2.click()
    page.wait_for_selector(".modal-dialog")
    page.click(".modal-dialog button:has-text('Confirmar')")

    # Esperar a que se procese (native alert se auto-descarta)
    page.wait_for_timeout(2000)

    # NO debe redirigir a Mis Préstamos — debe quedarse en el detalle
    assert "/mis-prestamos" not in page.url, (
        "No debería redirigir a Mis Préstamos tras prestar el mismo libro"
    )
    # Debe seguir mostrando el detalle del libro
    assert page.locator(".detail-grid").is_visible()


def test_admin_future_year_publication_shows_error(page: Page):
    """Crear libro con año futuro muestra error de validación del backend."""
    login(page, "admin@libromagico.com", "admin123")

    page.goto(f"{BASE_URL}/#/admin/libros/nuevo")
    page.wait_for_timeout(1000)
    page.fill("input[name='isbn']", "9781111111111")
    page.fill("input[name='titulo']", "Libro Futuro")
    page.fill("input[name='autor']", "Autor Futuro")
    page.fill("input[name='añoPub']", "2099")
    page.click("button:has-text('Crear Libro')")
    page.wait_for_timeout(2000)

    # Debe mostrar error de validación (año futuro)
    assert page.locator(".alert-error").is_visible(), "Debe mostrar error por año de publicación futuro"
    # Debe quedarse en el formulario
    assert "nuevo" in page.url, "Debe quedarse en el formulario"


def test_register_duplicate_email_shows_error(page: Page):
    """Registrarse con un email ya existente muestra error."""
    page.goto(f"{BASE_URL}/#/registro")
    page.wait_for_selector(".auth-form")

    # Usar email que ya existe en seed
    page.fill("input[placeholder='Nombre completo']", "Usuario Duplicado")
    page.fill("input[placeholder='correo@ejemplo.com']", "admin@libromagico.com")
    page.fill("input[placeholder='Contraseña']", "pass123456")
    page.fill("input[placeholder='DNI (8 dígitos)']", "99999999")
    page.fill("input[placeholder='+5491123456789']", "+5491160000000")
    page.click("button:has-text('Registrarse')")
    page.wait_for_timeout(2000)

    # Debe mostrar error
    assert page.locator(".alert-error").is_visible(), "Debe mostrar error por email duplicado"
    # Debe quedarse en la página de registro
    assert "registro" in page.url
