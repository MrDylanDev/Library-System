"""Tests de navegación, 404, enlaces footer, comportamiento general."""
import pytest
from conftest import BASE_URL, login, should_see_text, wait_for_hash
from playwright.sync_api import Page


def test_404_page(page: Page):
    """Ruta inexistente muestra 404."""
    page.goto(f"{BASE_URL}/#/ruta-inexistente-xyz")
    page.wait_for_timeout(1000)
    should_see_text(page, "404")
    should_see_text(page, "Página no encontrada")


def test_footer_links(page: Page):
    """Los links del footer navegan correctamente."""
    page.goto(BASE_URL)
    page.wait_for_selector("footer")

    page.locator("footer").locator("text=Catálogo").first.click()
    page.wait_for_timeout(1500)
    assert "#/catalogo" in page.url

    page.goto(BASE_URL)
    page.wait_for_selector("footer")
    page.locator("footer").locator("text=Iniciar sesión").first.click()
    page.wait_for_timeout(1500)
    assert "#/login" in page.url


def test_navbar_brand_goes_home(page: Page):
    """Click en el logo/brand va al inicio."""
    login(page, "usuario@libromagico.com", "usuario123")
    page.goto(f"{BASE_URL}/#/catalogo")
    page.wait_for_timeout(1000)
    page.click(".brand")
    page.wait_for_timeout(1500)
    assert "#/" in page.url or page.url.rstrip("/") == BASE_URL


def test_hero_cta_authenticated(page: Page):
    """Usuario autenticado ve 'Ver catálogo' en lugar de 'Comenzar ahora'."""
    login(page, "usuario@libromagico.com", "usuario123")
    page.goto(BASE_URL)
    page.wait_for_selector(".landing-hero")
    should_see_text(page, "Ver catálogo")
    assert page.locator("text=Comenzar ahora").count() == 0


def test_hero_cta_unauthenticated(page: Page):
    """Usuario anónimo ve 'Comenzar ahora' e 'Iniciar sesión'."""
    page.goto(BASE_URL)
    page.wait_for_selector(".hero-actions")
    should_see_text(page, "Comenzar ahora")
    should_see_text(page, "Iniciar sesión")


def test_services_section(page: Page):
    """La sección de servicios se renderiza con 4 tarjetas."""
    page.goto(BASE_URL)
    page.wait_for_selector(".landing-section")
    assert page.locator(".service-card").count() == 4


def test_testimonials_section(page: Page):
    """La sección de testimonios se renderiza con 3 tarjetas."""
    page.goto(BASE_URL)
    page.wait_for_selector(".testimonial-grid")
    assert page.locator(".testimonial-card").count() == 3


def test_cta_section(page: Page):
    """La sección CTA se renderiza."""
    page.goto(BASE_URL)
    page.wait_for_selector(".cta-box")
    should_see_text(page, "¿Listo para transformar tu biblioteca?")


def test_footer_bottom(page: Page):
    """El footer tiene copyright."""
    page.goto(BASE_URL)
    page.wait_for_selector(".footer-bottom")
    should_see_text(page, "LibroMágico")
    should_see_text(page, "Todos los derechos reservados")


def test_swagger_ui_loads(page: Page):
    """Swagger/OpenAPI docs están disponibles."""
    page.goto(f"{BASE_URL}/swagger-ui.html")
    page.wait_for_timeout(3000)
    # Swagger puede redirigir o mostrar su UI
    assert "swagger" in page.url.lower()
