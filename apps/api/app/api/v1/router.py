"""Root router for versioned endpoints."""

from fastapi import APIRouter

from app.modules.suppliers.router import router as suppliers_router

api_router = APIRouter()
api_router.include_router(suppliers_router)