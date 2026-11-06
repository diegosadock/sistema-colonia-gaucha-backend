from decimal import Decimal

from sqlalchemy import case, func, select
from sqlalchemy.orm import Session

from app.modules.products.models import Product
from app.modules.stock.models import MovementType, StockMovement

# Saldo = entradas - saídas + ajustes (ajuste já vem com sinal)
SIGNED_QTY = case(
    (StockMovement.type == MovementType.ENTRY, StockMovement.quantity),
    (StockMovement.type == MovementType.EXIT, -StockMovement.quantity),
    else_=StockMovement.quantity,
)


class StockRepository:
    def __init__(self, db: Session):
        self.db = db

    def lock_product(self, product_id: int) -> Product | None:
        """Trava a linha do produto até o fim da transação (evita corrida)."""
        stmt = select(Product).where(Product.id == product_id).with_for_update()
        return self.db.scalar(stmt)

    def get_balance(self, product_id: int) -> Decimal:
        stmt = select(func.coalesce(func.sum(SIGNED_QTY), 0)).where(
            StockMovement.product_id == product_id
        )
        return Decimal(self.db.scalar(stmt))

    def add(self, movement: StockMovement) -> StockMovement:
        self.db.add(movement)
        self.db.flush()
        return movement

    def list_movements(self, product_id: int | None, skip: int, limit: int):
        stmt = (
            select(StockMovement)
            .order_by(StockMovement.created_at.desc(), StockMovement.id.desc())
            .offset(skip)
            .limit(limit)
        )
        if product_id is not None:
            stmt = stmt.where(StockMovement.product_id == product_id)
        return list(self.db.scalars(stmt))

    def list_balances(self, product_id: int | None):
        total = func.coalesce(func.sum(SIGNED_QTY), 0)
        stmt = (
            select(
                Product.id.label("product_id"),
                Product.name.label("product_name"),
                Product.base_unit.label("base_unit"),
                total.label("balance"),
            )
            .outerjoin(StockMovement, StockMovement.product_id == Product.id)
            .group_by(Product.id, Product.name, Product.base_unit)
            .order_by(Product.name)
        )
        if product_id is not None:
            stmt = stmt.where(Product.id == product_id)
        return self.db.execute(stmt).mappings().all()