from decimal import Decimal

from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.modules.stock.models import MovementType, StockMovement
from app.modules.stock.repository import StockRepository
from app.modules.stock.schemas import MovementCreate


class StockService:
    def __init__(self, db: Session):
        self.db = db
        self.repo = StockRepository(db)

    def register_movement(self, data: MovementCreate) -> StockMovement:
        try:
            # 1) trava o produto: dois pedidos simultâneos esperam na fila
            product = self.repo.lock_product(data.product_id)
            if not product:
                raise HTTPException(status.HTTP_404_NOT_FOUND, "Produto não encontrado")
            if not product.is_active:
                raise HTTPException(status.HTTP_409_CONFLICT, "Produto está desativado")

            # 2) calcula o efeito do movimento no saldo
            if data.type == MovementType.EXIT:
                delta = -data.quantity
            else:  # ENTRY (+) ou ADJUSTMENT (já com sinal)
                delta = data.quantity

            # 3) recusa saldo negativo
            balance = self.repo.get_balance(product.id)
            if balance + delta < Decimal("0"):
                raise HTTPException(
                    status.HTTP_409_CONFLICT,
                    f"Saldo insuficiente. Disponível: {balance}",
                )

            # 4) grava e confirma tudo de uma vez
            movement = self.repo.add(
                StockMovement(
                    product_id=product.id,
                    type=data.type,
                    quantity=data.quantity,
                    reason=data.reason,
                )
            )
            self.db.commit()
            self.db.refresh(movement)
            return movement
        except Exception:
            self.db.rollback()
            raise

    def list_movements(self, product_id: int | None, skip: int, limit: int):
        return self.repo.list_movements(product_id, skip, limit)

    def list_balances(self, product_id: int | None):
        return self.repo.list_balances(product_id)