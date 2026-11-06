from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.modules.suppliers.models import Supplier
from app.modules.suppliers.repository import SupplierRepository
from app.modules.suppliers.schemas import SupplierCreate, SupplierUpdate


class SupplierService:
    def __init__(self, db: Session):
        self.db = db
        self.repo = SupplierRepository(db)

    def create(self, data: SupplierCreate) -> Supplier:
        if data.document and self.repo.get_by_document(data.document):
            raise HTTPException(status.HTTP_409_CONFLICT, "Já existe fornecedor com este documento")
        supplier = self.repo.create(data.model_dump())
        self.db.commit()
        self.db.refresh(supplier)
        return supplier

    def get(self, supplier_id: int) -> Supplier:
        supplier = self.repo.get(supplier_id)
        if not supplier:
            raise HTTPException(status.HTTP_404_NOT_FOUND, "Fornecedor não encontrado")
        return supplier

    def list(self, skip: int, limit: int, only_active: bool) -> list[Supplier]:
        return self.repo.list(skip, limit, only_active)

    def update(self, supplier_id: int, data: SupplierUpdate) -> Supplier:
        supplier = self.get(supplier_id)
        changes = data.model_dump(exclude_unset=True)  # só o que foi enviado

        new_doc = changes.get("document")
        if new_doc and new_doc != supplier.document and self.repo.get_by_document(new_doc):
            raise HTTPException(status.HTTP_409_CONFLICT, "Já existe fornecedor com este documento")

        supplier = self.repo.update(supplier, changes)
        self.db.commit()
        self.db.refresh(supplier)
        return supplier