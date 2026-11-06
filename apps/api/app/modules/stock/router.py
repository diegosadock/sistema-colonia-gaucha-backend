from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.modules.stock.schemas import BalanceRead, MovementCreate, MovementRead
from app.modules.stock.service import StockService

router = APIRouter(prefix="/stock", tags=["Stock"])


@router.post("/movements", response_model=MovementRead, status_code=status.HTTP_201_CREATED)
def create_movement(data: MovementCreate, db: Session = Depends(get_db)):
    return StockService(db).register_movement(data)


@router.get("/movements", response_model=list[MovementRead])
def list_movements(
    product_id: int | None = None,
    skip: int = Query(0, ge=0),
    limit: int = Query(50, ge=1, le=200),
    db: Session = Depends(get_db),
):
    return StockService(db).list_movements(product_id, skip, limit)


@router.get("/balances", response_model=list[BalanceRead])
def list_balances(product_id: int | None = None, db: Session = Depends(get_db)):
    return StockService(db).list_balances(product_id)