"""SQLAlchemy 2.x declarative base for future persistence models."""

from sqlalchemy.orm import DeclarativeBase


class Base(DeclarativeBase):
    """Base class whose metadata is used by Alembic."""
