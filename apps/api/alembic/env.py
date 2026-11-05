"""Alembic environment configured from application settings and SQLAlchemy metadata."""

from alembic import context
from app.core.config import get_settings
from app.db.base import Base
from sqlalchemy import engine_from_config, pool

config = context.config
target_metadata = Base.metadata


def database_url() -> str:
    """Read the connection URL from the same settings used by the application."""
    return get_settings().database_url


def run_migrations_offline() -> None:
    """Run migrations without creating an Engine, using literal SQL values."""
    context.configure(
        url=database_url(),
        target_metadata=target_metadata,
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
        compare_type=True,
    )

    with context.begin_transaction():
        context.run_migrations()


def run_migrations_online() -> None:
    """Run migrations through a short-lived SQLAlchemy connection."""
    section = config.get_section(config.config_ini_section, {})
    section["sqlalchemy.url"] = database_url()
    connectable = engine_from_config(
        section,
        prefix="sqlalchemy.",
        poolclass=pool.NullPool,
    )

    with connectable.connect() as connection:
        context.configure(
            connection=connection,
            target_metadata=target_metadata,
            compare_type=True,
        )

        with context.begin_transaction():
            context.run_migrations()


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
