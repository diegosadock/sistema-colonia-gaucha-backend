"""HTTP integration tests for the health endpoint."""

import httpx
import pytest
from app.core.config import Settings
from app.factory import create_app
from fastapi import FastAPI


@pytest.fixture
def application() -> FastAPI:
    """Build an isolated app instance without requiring PostgreSQL for health checks."""
    return create_app(Settings(database_url="postgresql+psycopg://localhost/test"))


@pytest.mark.asyncio
async def test_health_returns_operational_status(application: FastAPI) -> None:
    transport = httpx.ASGITransport(app=application)
    async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


@pytest.mark.asyncio
async def test_http_errors_use_the_standard_envelope(application: FastAPI) -> None:
    transport = httpx.ASGITransport(app=application)
    async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/missing")

    assert response.status_code == 404
    assert response.json() == {
        "error": {
            "code": "http_error",
            "message": "Not Found",
            "details": None,
        }
    }
