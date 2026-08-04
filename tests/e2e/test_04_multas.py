"""Tests de multas: mis multas, pago, admin multas."""
import pytest
from conftest import BASE_URL, login, should_see_text, MOROSO_USER
from playwright.sync_api import Page


def test_mis_multas_empty(page: Page):
    """Usuario sin multas ve estado vacío."""
    login(page, "usuario@libromagico.com", "usuario123")
    page.goto(f"{BASE_URL}/#/mis-multas")
    page.wait_for_timeout(1000)
    has_empty = page.locator("text=No tenés multas").count() > 0
    has_table = page.locator("table").count() > 0
    assert has_empty or has_table, "No se ve ni empty state ni tabla"


def test_mis_multas_requires_auth(page: Page):
    """Sin autenticar, mis-multas redirige."""
    page.goto(f"{BASE_URL}/#/mis-multas")
    page.wait_for_timeout(1000)
    assert "#/login" in page.url or "#/mis-multas" not in page.url


def test_admin_multas_page(page: Page):
    """Admin ve página de multas."""
    login(page, "admin@libromagico.com", "admin123")
    page.goto(f"{BASE_URL}/#/admin/multas")
    page.wait_for_timeout(1000)
    should_see_text(page, "Multas")

    has_table = page.locator("table").count() > 0
    has_empty = page.locator("text=No hay multas").count() > 0
    assert has_table or has_empty, "No se ve ni tabla ni empty state"


def test_user_can_pay_fine(page: Page):
    """Usuario con multa pendiente puede pagarla desde Mis Multas."""
    login(page, MOROSO_USER["email"], MOROSO_USER["contrasena"])
    page.goto(f"{BASE_URL}/#/mis-multas")
    page.wait_for_timeout(1000)

    pagar_btn = page.locator("button:has-text('Pagar')")
    assert pagar_btn.count() > 0, "No se encontraron multas pendientes para pagar"

    total_before = pagar_btn.count()
    pagar_btn.first.click()
    page.wait_for_timeout(1500)

    # Mis Multas solo muestra PENDIENTES, la pagada desaparece
    remaining = page.locator("button:has-text('Pagar')").count()
    assert remaining < total_before, "La multa no desapareció tras pagarla"


def test_admin_can_pay_fine(page: Page):
    """Admin puede pagar una multa desde el panel."""
    login(page, "admin@libromagico.com", "admin123")
    page.goto(f"{BASE_URL}/#/admin/multas")
    page.wait_for_timeout(1000)

    pagar_btn = page.locator("button:has-text('Pagar')")
    if pagar_btn.count() == 0:
        pytest.skip("No hay multas pendientes para pagar")

    total_before = pagar_btn.count()
    pagar_btn.first.click()
    page.wait_for_selector(".modal-dialog")
    page.click(".modal-dialog button:has-text('Confirmar')")
    page.wait_for_timeout(1500)

    remaining = page.locator("button:has-text('Pagar')").count()
    assert remaining < total_before, "La multa no desapareció tras pagarla"


def test_multas_page_shows_table_headers(page: Page):
    """La tabla de multas tiene los encabezados correctos."""
    login(page, "admin@libromagico.com", "admin123")
    page.goto(f"{BASE_URL}/#/admin/multas")
    page.wait_for_timeout(1000)

    if page.locator("table").count() > 0:
        headers = page.locator("th").all_text_contents()
        headers_text = [h.strip() for h in headers]
        assert "ID" in headers_text
        assert "Monto" in headers_text or "Multa" in str(headers_text)
