"""Tests de paginación del catálogo."""
from conftest import BASE_URL, should_see_text
from playwright.sync_api import Page


def test_catalog_shows_pagination_controls(page: Page):
    """El catálogo muestra los controles de paginación con 2 páginas."""
    page.goto(f"{BASE_URL}/#/catalogo")
    page.wait_for_timeout(2000)
    page.wait_for_selector(".pagination", timeout=5000)
    should_see_text(page, "Página 1 de 2")


def test_catalog_page_2_shows_different_books(page: Page):
    """La página 2 del catálogo muestra títulos distintos a los de la página 1."""
    page.goto(f"{BASE_URL}/#/catalogo")
    page.wait_for_timeout(2000)
    page.wait_for_selector(".pagination", timeout=5000)

    page_1_titles = page.locator(".book-card h3").all_inner_texts()
    page.click(".pagination-next")
    page.wait_for_timeout(1500)
    page_2_titles = page.locator(".book-card h3").all_inner_texts()

    assert page_2_titles, "La página 2 no muestra libros"
    assert set(page_2_titles) != set(page_1_titles), \
        "Los títulos de la página 2 deben ser distintos de los de la página 1"


def test_catalog_search_paginated_keeps_filter(page: Page):
    """Buscar 'Prueba' mantiene el filtro al navegar entre páginas."""
    page.goto(f"{BASE_URL}/#/catalogo")
    page.wait_for_timeout(1000)
    page.fill(".search-bar input", "Prueba")
    page.click(".search-bar button")
    page.wait_for_timeout(1500)
    page.wait_for_selector(".pagination", timeout=5000)

    page_1_titles = page.locator(".book-card h3").all_inner_texts()
    assert page_1_titles, "La página 1 de la búsqueda no muestra libros"
    assert all("Prueba" in t for t in page_1_titles), \
        "Todos los resultados de la página 1 deben contener 'Prueba'"

    page.click(".pagination-next")
    page.wait_for_timeout(1500)
    page_2_titles = page.locator(".book-card h3").all_inner_texts()
    assert page_2_titles, "La página 2 de la búsqueda no muestra libros"
    assert all("Prueba" in t for t in page_2_titles), \
        "El filtro debe mantenerse al navegar a la página 2"
    assert set(page_2_titles) != set(page_1_titles), \
        "La página 2 debe mostrar títulos distintos de los de la página 1"