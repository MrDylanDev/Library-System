"""Configuración compartida para tests E2E con Playwright."""
import json
import re
import time
import quopri
from urllib.request import urlopen, Request

import pytest
from playwright.sync_api import sync_playwright, Page, Browser

BASE_URL = "http://localhost:8080"
MAILHOG_URL = "http://localhost:8025"

# Usuarios de prueba (creados por UserDataInitializer en dev)
ADMIN_USER = {"email": "admin@libromagico.com", "contrasena": "admin123"}
LIBRARIAN_USER = {"email": "librarian@libromagico.com", "contrasena": "librarian123"}
BASIC_USER = {"email": "usuario@libromagico.com", "contrasena": "usuario123"}
MOROSO_USER = {"email": "moroso@libromagico.com", "contrasena": "moroso123"}


def mailhog_clean():
    """Elimina todos los mensajes de MailHog."""
    req = Request(f"{MAILHOG_URL}/api/v1/messages", method="DELETE")
    urlopen(req)


def mailhog_get_reset_token(email: str, timeout: int = 10) -> str:
    """Espera un email de reset-password y extrae el token.

    Args:
        email: Destinatario del email.
        timeout: Tiempo máximo de espera en segundos.

    Returns:
        El token de reset extraído del link.

    Raises:
        TimeoutError: Si no se encuentra el email en el tiempo dado.
    """
    deadline = time.time() + timeout
    while time.time() < deadline:
        resp = urlopen(f"{MAILHOG_URL}/api/v2/messages")
        data = json.loads(resp.read())
        for item in data.get("items", []):
            headers = item.get("Content", {}).get("Headers", {})
            to = headers.get("To", [""])[0]
            if email not in to:
                continue

            # Extraer el cuerpo HTML en quoted-printable
            full_body = item.get("MIME", {}).get("Parts", [{}])[0].get("Body", "")
            m = re.search(
                r"Content-Transfer-Encoding: quoted-printable\r?\n\r?\n(.*?)(?:\r?\n------)",
                full_body,
                re.DOTALL,
            )
            if not m:
                continue

            # Decodificar quoted-printable
            qp_joined = re.sub(r"=\r?\n", "", m.group(1))
            decoded = quopri.decodestring(qp_joined.encode("ascii"))
            html = decoded.decode("utf-8", errors="replace")

            # Extraer link de reset (el QP decode ya convirtió =3D a =)
            link_m = re.search(r'href="(http[^"]+reset-password/[^"]+)"', html)
            if link_m:
                return link_m.group(1).split("/reset-password/")[-1]

        time.sleep(1)

    raise TimeoutError(f"No se encontró email de reset para {email} luego de {timeout}s")


@pytest.fixture(scope="session")
def browser():
    """Lanza Chromium una vez por sesión de test."""
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        yield browser
        browser.close()


@pytest.fixture
def page(browser: Browser):
    """Nueva página (pestaña limpia) por cada test."""
    context = browser.new_context(viewport={"width": 1280, "height": 720})
    page = context.new_page()
    page.set_default_timeout(10000)
    yield page
    context.close()


def wait_for_hash(page: Page, expected_hash: str, timeout: int = 15000):
    """Espera a que la URL tenga el hash esperado (para SPA con # routing)."""
    page.wait_for_function(
        f"window.location.hash === '#{expected_hash}' || window.location.hash.startsWith('#{expected_hash}')",
        timeout=timeout
    )


def login(page: Page, email: str, password: str):
    """Helper: loguearse con email y contraseña."""
    page.goto(f"{BASE_URL}/#/login")
    page.wait_for_selector("input[type=email]")
    page.fill("input[type=email]", email)
    page.fill("input[type=password]", password)
    page.click("button:has-text('Ingresar')")
    # Esperar navegación al catálogo
    page.wait_for_selector("input[placeholder*='Buscar'], .book-card", timeout=10000)


def should_see_text(page: Page, text: str):
    """Verifica que un texto sea visible en la página."""
    assert page.locator(f"text={text}").first.is_visible(), f"Texto '{text}' no visible"


def should_not_see_text(page: Page, text: str):
    """Verifica que un texto NO esté visible."""
    assert page.locator(f"text={text}").count() == 0, f"Texto '{text}' está presente pero no debería"
