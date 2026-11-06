from sqlalchemy import select
from sqlalchemy.orm import Session

from app.modules.suppliers.models import Supplier


class SupplierRepository:
    def __init__(self, db: Session):
        self.db = db

    def create(self, data: dict) -> Supplier:
        supplier = Supplier(**data)
        self.db.add(supplier)
        self.db.flush()  # gera o id, mas ainda não confirma
        return supplier

    def get(self, supplier_id: int) -> Supplier | None:
        return self.db.get(Supplier, supplier_id)

    def get_by_document(self, document: str) -> Supplier | None:
        return self.db.scalar(select(Supplier).where(Supplier.document == document))

    def list(self, skip: int, limit: int, only_active: bool) -> list[Supplier]:
        stmt = select(Supplier).order_by(Supplier.name).offset(skip).limit(limit)
        if only_active:
            stmt = stmt.where(Supplier.is_active.is_(True))
        return list(self.db.scalars(stmt))

    def update(self, supplier: Supplier, data: dict) -> Supplier:
        for field, value in data.items():
            setattr(supplier, field, value)
        self.db.flush()
        return supplier