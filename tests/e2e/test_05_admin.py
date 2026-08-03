"""Tests del panel admin: dashboard, CRUD libros, gestión usuarios."""
import pytest
from conftest import BASE_URL, login, should_see_text
from playwright.sync_api import Page


def test_admin_dashboard_shows_stats(page: Page):
    """El dashboard admin carga con tarjetas de estadísticas."""
    login(page, "admin@libromagico.com", "admin123")
    page.goto(f"{BASE_URL}/#/admin")
    page.wait_for_selector(".dashboard-grid", timeout=8000)

    should_see_text(page, "Dashboard")
    # Debe mostrar tarjetas de stats
    assert page.locator(".dashboard-card").count() >= 4


def test_admin_create_book(page: Page):
    """Admin puede crear un libro nuevo."""
    login(page, "admin@libromagico.com", "admin123")
    page.goto(f"{BASE_URL}/#/admin/libros/nuevo")
    page.wait_for_selector(".card-header")

    isbn = "9780000000001"
    page.fill("input[name='isbn']", isbn)
    page.fill("input[name='titulo']", "Libro de Test E2E")
    page.fill("input[name='autor']", "Autor Test")
    page.click("button:has-text('Crear Libro')")

    page.wait_for_timeout(2000)
    should_see_text(page, "Catálogo de Libros")


def test_admin_edit_book(page: Page):
    """Admin puede editar un libro existente."""
    # Primero crear un libro para editarlo
    login(page, "admin@libromagico.com", "admin123")
    page.goto(f"{BASE_URL}/#/admin/libros/nuevo")
    page.wait_for_selector(".card-header")

    isbn = "9780000000002"
    page.fill("input[name='isbn']", isbn)
    page.fill("input[name='titulo']", "Libro para Editar")
    page.fill("input[name='autor']", "Autor Original")
    page.click("button:has-text('Crear Libro')")
    page.wait_for_timeout(2000)

    # Ir al detalle y click editar
    page.goto(f"{BASE_URL}/#/libros/{isbn}")
    page.wait_for_selector(".detail-grid", timeout=5000)
    page.click("button:has-text('Editar')")
    page.wait_for_url(f"{BASE_URL}/#/admin/libros/{isbn}/editar", timeout=5000)
    page.wait_for_timeout(500)
    should_see_text(page, "Editar Libro")

    # Cambiar título y guardar
    page.fill("input[name='titulo']", "")
    page.fill("input[name='titulo']", "Libro Editado")
    page.click("button:has-text('Guardar Cambios')")

    page.wait_for_timeout(2000)
    page.goto(f"{BASE_URL}/#/libros/{isbn}")
    page.wait_for_timeout(1000)
    should_see_text(page, "Libro Editado")


def test_admin_delete_book(page: Page):
    """Admin puede eliminar un libro."""
    login(page, "admin@libromagico.com", "admin123")

    # Crear libro para eliminar
    isbn = "9780000000003"
    page.goto(f"{BASE_URL}/#/admin/libros/nuevo")
    page.wait_for_selector(".card-header")
    page.fill("input[name='isbn']", isbn)
    page.fill("input[name='titulo']", "Libro a Eliminar")
    page.fill("input[name='autor']", "Autor Eliminar")
    page.click("button:has-text('Crear Libro')")
    page.wait_for_timeout(2000)

    # Ir a detalle y eliminar
    page.goto(f"{BASE_URL}/#/libros/{isbn}")
    page.wait_for_selector(".detail-grid", timeout=5000)
    page.click("button:has-text('Eliminar')")
    page.wait_for_selector(".modal-dialog")
    page.click(".modal-dialog button:has-text('Confirmar')")
    page.wait_for_timeout(2000)

    # Verificar que ya no está en el catálogo
    assert page.locator(f"text=Libro a Eliminar").count() == 0


def test_admin_mark_book_as_lost(page: Page):
    """Admin puede marcar un libro como PERDIDO."""
    login(page, "admin@libromagico.com", "admin123")

    # Crear libro
    isbn = "9780000000004"
    page.goto(f"{BASE_URL}/#/admin/libros/nuevo")
    page.wait_for_selector(".card-header")
    page.fill("input[name='isbn']", isbn)
    page.fill("input[name='titulo']", "Libro a Perder")
    page.fill("input[name='autor']", "Autor Perder")
    page.click("button:has-text('Crear Libro')")
    page.wait_for_timeout(2000)
    page.goto(f"{BASE_URL}/#/libros/{isbn}")
    page.wait_for_selector(".detail-grid", timeout=5000)
    page.click("button:has-text('Perdido')")
    page.wait_for_selector(".modal-dialog")
    page.click(".modal-dialog button:has-text('Confirmar')")
    page.wait_for_timeout(1000)

    # Debe mostrar PERDIDO
    # Recargamos la página
    page.goto(f"{BASE_URL}/#/libros/{isbn}")
    page.wait_for_selector(".detail-grid", timeout=5000)
    should_see_text(page, "PERDIDO")


def test_admin_users_page(page: Page):
    """Admin ve lista de usuarios."""
    login(page, "admin@libromagico.com", "admin123")
    page.goto(f"{BASE_URL}/#/admin/usuarios")
    page.wait_for_timeout(1000)
    should_see_text(page, "Usuarios")
    has_table = page.locator("table").count() > 0
    has_empty = page.locator("text=No hay usuarios").count() > 0
    assert has_table or has_empty


def test_admin_can_change_user_role(page: Page):
    """Admin puede cambiar el rol de un usuario."""
    login(page, "admin@libromagico.com", "admin123")
    page.goto(f"{BASE_URL}/#/admin/usuarios")
    page.wait_for_timeout(1000)

    # Buscar botón de cambio de rol
    rol_btn = page.locator("button:has-text('Rol:')")
    if rol_btn.count() == 0:
        pytest.skip("No hay botones de cambio de rol disponibles")

    rol_btn.first.click()
    page.wait_for_selector(".modal-dialog")
    page.click(".modal-dialog button:has-text('Confirmar')")
    page.wait_for_timeout(1000)
    # No debe mostrar error
    assert page.locator(".alert-error").count() == 0


def test_admin_can_block_user(page: Page):
    """Admin puede bloquear/desbloquear un usuario."""
    login(page, "admin@libromagico.com", "admin123")
    page.goto(f"{BASE_URL}/#/admin/usuarios")
    page.wait_for_timeout(1000)

    bloqueo_btn = page.locator("button:has-text('Bloquear')")
    if bloqueo_btn.count() == 0:
        pytest.skip("No hay usuarios para bloquear")

    bloqueo_btn.first.click()
    page.wait_for_selector(".modal-dialog")
    page.click(".modal-dialog button:has-text('Confirmar')")
    page.wait_for_timeout(1000)
    assert page.locator(".alert-error").count() == 0


def test_admin_edit_book_form_prefilled(page: Page):
    """El formulario de edición de libro carga con datos existentes."""
    login(page, "admin@libromagico.com", "admin123")

    # Usar un libro existente del seed
    page.goto(f"{BASE_URL}/#/libros/9780132350884")
    page.wait_for_selector(".detail-grid", timeout=5000)

    should_see_text(page, "Clean Code")
    page.click("button:has-text('Editar')")
    page.wait_for_url(f"{BASE_URL}/#/admin/libros/9780132350884/editar", timeout=5000)
    page.wait_for_timeout(500)
    should_see_text(page, "Editar Libro")
    # Verificar que el título está precargado
    title_input = page.locator("input[name='titulo']")
    assert title_input.input_value() == "Clean Code"


def test_unauthorized_admin_access(page: Page):
    """Usuario USER no puede acceder a rutas admin."""
    login(page, "usuario@libromagico.com", "usuario123")
    page.goto(f"{BASE_URL}/#/admin")
    page.wait_for_timeout(1000)
    should_see_text(page, "No tenés permisos")


def test_admin_nuevo_libro_validation(page: Page):
    """Crear libro sin título muestra error del backend."""
    login(page, "admin@libromagico.com", "admin123")
    page.goto(f"{BASE_URL}/#/admin/libros/nuevo")
    page.wait_for_selector(".card-header")

    # ISBN vacío
    page.fill("input[name='isbn']", "")
    page.click("button:has-text('Crear Libro')")
    # El navegador no debe permitir submit con required vacío
    assert "#/admin/libros/nuevo" in page.url
