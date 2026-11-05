"""Unit tests for environment-backed settings."""

from app.core.config import Settings


def test_settings_read_environment_variables(monkeypatch) -> None:
    monkeypatch.setenv("APP_NAME", "Test Restaurant API")
    monkeypatch.setenv("CORS_ORIGINS", '["http://localhost:3000"]')

    settings = Settings(_env_file=None)

    assert settings.app_name == "Test Restaurant API"
    assert settings.cors_origins == ["http://localhost:3000"]
