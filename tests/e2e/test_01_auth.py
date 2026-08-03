"""Tests de autenticación: registro, login, logout, roles."""
import pytest
from conftest import BASE_URL, login, should_see_text, wait_for_hash, mailhog_clean, mailhog_get_reset_token
from playwright.sync_api import Page


def test_landing_page_loads(page: Page):
    """La landing debe cargar con hero y secciones principales."""
    page.goto(BASE_URL)
    page.wait_for_selector(".landing-hero")
    should_see_text(page, "LibroMágico")
    should_see_text(page, "Sistema de gestión bibliotecaria")
    should_see_text(page, "Iniciar Sesión")
    should_see_text(page, "Registrarse")
    should_see_text(page, "Catálogo")
    # No debe mostrar enlaces de usuario autenticado
    assert page.locator("text=Mis Préstamos").count() == 0


def test_navigate_to_login(page: Page):
    """Click en 'Iniciar Sesión' lleva al formulario de login."""
    page.goto(BASE_URL)
    page.click("text=Iniciar Sesión")
    page.wait_for_selector(".auth-form")
    should_see_text(page, "Iniciar Sesión")
    should_see_text(page, "Email")
    assert page.locator("input[type=email]").is_visible()
    assert page.locator("input[type=password]").is_visible()


def test_login_as_admin(page: Page):
    """Login como ADMIN redirige al catálogo y muestra todos los links admin."""
    login(page, "admin@libromagico.com", "admin123")
    # Navbar completo
    should_see_text(page, "Catálogo")
    should_see_text(page, "Mis Préstamos")
    should_see_text(page, "Mis Multas")
    should_see_text(page, "Dashboard")
    should_see_text(page, "Préstamos")
    should_see_text(page, "Usuarios")
    should_see_text(page, "Multas")
    should_see_text(page, "Nuevo Libro")
    should_see_text(page, "admin@libromagico.com")
    should_see_text(page, "Salir")


def test_login_as_librarian(page: Page):
    """Login como LIBRARIAN también ve los links admin."""
    login(page, "librarian@libromagico.com", "librarian123")
    page.wait_for_selector("text=Dashboard", timeout=10000)
    should_see_text(page, "Préstamos")
    should_see_text(page, "Usuarios")


def test_login_as_user(page: Page):
    """Login como USER solo ve links básicos, sin admin."""
    login(page, "usuario@libromagico.com", "usuario123")
    should_see_text(page, "Catálogo")
    should_see_text(page, "Mis Préstamos")
    should_see_text(page, "Mis Multas")
    # Sin links admin
    assert page.locator("text=Dashboard").count() == 0
    assert page.locator("text=Nuevo Libro").count() == 0


def test_logout(page: Page):
    """Logout limpia sesión y redirige a login."""
    login(page, "usuario@libromagico.com", "usuario123")
    page.click("text=Salir")
    wait_for_hash(page, "/login")
    should_see_text(page, "Iniciar Sesión")
    assert page.locator("text=Mis Préstamos").count() == 0


def test_register_new_user(page: Page):
    """Registro de nuevo usuario crea cuenta y redirige al catálogo."""
    page.goto(f"{BASE_URL}/#/registro")
    page.wait_for_selector(".auth-form")

    import time
    ts = str(int(time.time()))
    email = "nuevo" + ts + "@test.com"
    dni = ts[-8:]  # últimos 8 dígitos del timestamp
    page.fill("input[type=text][placeholder*='Nombre']", "Nuevo Usuario")
    page.fill("input[type=email]", email)
    page.fill("input[type=password]", "Pass123456!")
    page.fill("input[placeholder*='DNI']", dni)
    page.fill("input[type=tel]", "+5491133557799")
    page.click("button:has-text('Registrarse')")

    wait_for_hash(page, "/catalogo")
    should_see_text(page, "Catálogo")
    # Verificar que el email aparece en el navbar
    should_see_text(page, email)


def test_login_invalid_credentials(page: Page):
    """Credenciales inválidas muestran error."""
    page.goto(f"{BASE_URL}/#/login")
    page.fill("input[type=email]", "vacio@test.com")
    page.fill("input[type=password]", "wrongpass")
    page.click("button:has-text('Ingresar')")
    page.wait_for_timeout(2000)
    # El mensaje de error debe ser visible
    assert page.locator(".alert.alert-error").is_visible()


def test_register_validation_empty_fields(page: Page):
    """Campos vacíos en registro muestran validación HTML5."""
    page.goto(f"{BASE_URL}/#/registro")
    page.click("button:has-text('Registrarse')")
    # El navegador muestra validación HTML5, la URL no cambia
    assert "#/registro" in page.url


def test_forgot_password_page(page: Page):
    """La página de recuperación de contraseña carga."""
    page.goto(f"{BASE_URL}/#/forgot-password")
    should_see_text(page, "Recuperar Contraseña")
    assert page.locator("button:has-text('Enviar enlace')").is_visible()


def test_login_redirects_when_authenticated(page: Page):
    """Si ya está autenticado, visitar /login redirige a catálogo."""
    login(page, "usuario@libromagico.com", "usuario123")
    page.goto(f"{BASE_URL}/#/login")
    wait_for_hash(page, "/catalogo")


def test_reset_password_full_flow(page: Page):
    """Flujo completo: forgot-password → MailHog token → reset → login con nueva pass → restaurar."""
    EMAIL = "usuario@libromagico.com"
    ORIGINAL_PASS = "usuario123"
    TEMP_PASS = "TempPass999!"

    mailhog_clean()

    # 1. Ir a forgot-password y solicitar reset
    page.goto(f"{BASE_URL}/#/forgot-password")
    page.wait_for_selector("input[type=email]")
    page.fill("input[type=email]", EMAIL)
    page.click("button:has-text('Enviar enlace')")
    page.wait_for_selector(".alert-success", timeout=10000)

    # 2. Extraer token de MailHog
    token = mailhog_get_reset_token(EMAIL)
    assert token, "Debe extraer un token de reset"

    # 3. Ir a la página de reset con el token
    page.goto(f"{BASE_URL}/#/reset-password/{token}")
    page.wait_for_selector("button:has-text('Actualizar contraseña')")
    should_see_text(page, "Nueva Contraseña")

    # 4. Ingresar nueva contraseña y confirmar
    page.locator("input[type=password]").first.fill(TEMP_PASS)
    page.locator("input[type=password]").nth(1).fill(TEMP_PASS)
    page.click("button:has-text('Actualizar contraseña')")

    # 5. Verificar mensaje de éxito y redirección a login
    page.wait_for_selector(".alert-success", timeout=10000)
    page.wait_for_timeout(3000)
    assert "/login" in page.url or "Iniciar" in page.text_content("body")

    # 6. Login con la nueva contraseña
    login(page, EMAIL, TEMP_PASS)
    should_see_text(page, "Catálogo")
    page.click("text=Salir")
    page.wait_for_timeout(1000)

    # 7. Restaurar contraseña original
    mailhog_clean()
    page.goto(f"{BASE_URL}/#/forgot-password")
    page.wait_for_selector("input[type=email]")
    page.fill("input[type=email]", EMAIL)
    page.click("button:has-text('Enviar enlace')")
    page.wait_for_selector(".alert-success", timeout=10000)

    token2 = mailhog_get_reset_token(EMAIL)
    assert token2, "Debe extraer token para restaurar"

    page.goto(f"{BASE_URL}/#/reset-password/{token2}")
    page.wait_for_selector("button:has-text('Actualizar contraseña')")
    page.fill("input[placeholder*='Nueva contraseña']", ORIGINAL_PASS)
    page.fill("input[placeholder*='Confirmar']", ORIGINAL_PASS)
    page.click("button:has-text('Actualizar contraseña')")
    page.wait_for_selector(".alert-success", timeout=10000)

    # 8. Verificar que la contraseña original funciona de nuevo
    page.wait_for_timeout(2000)
    page.goto(f"{BASE_URL}/#/login")
    login(page, EMAIL, ORIGINAL_PASS)
    should_see_text(page, "Catálogo")
