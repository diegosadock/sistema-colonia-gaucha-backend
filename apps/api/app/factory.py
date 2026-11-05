"""Application factory, kept separate from environment configuration."""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.v1.router import api_router
from app.core.config import Settings
from app.core.errors import register_exception_handlers


def create_app(settings: Settings) -> FastAPI:
    """Build a FastAPI application using the supplied settings."""
    application = FastAPI(
        title=settings.app_name,
        version="0.1.0",
        description="Backend API for restaurant operations management.",
        docs_url="/docs",
        redoc_url="/redoc",
        openapi_url="/openapi.json",
    )

    if settings.cors_origins:
        application.add_middleware(
            CORSMiddleware,
            allow_origins=settings.cors_origins,
            allow_credentials=True,
            allow_methods=["*"],
            allow_headers=["*"],
        )

    register_exception_handlers(application)
    application.include_router(api_router, prefix="/api/v1")

    @application.get("/health", tags=["health"], summary="Check API availability")
    async def health() -> dict[str, str]:
        """Report that the API process is available; database checks are out of scope."""
        return {"status": "ok"}

    return application
