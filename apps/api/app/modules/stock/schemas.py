from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel, ConfigDict, Field, model_validator

from app.modules.stock.models import MovementType


class MovementCreate(BaseModel):
    product_id: int
    type: MovementType
    quantity: Decimal = Field(max_digits=14, decimal_places=3)
    reason: str | None = Field(default=None, max_length=255)

    @model_validator(mode="after")
    def check_rules(self):
        if self.type == MovementType.ADJUSTMENT:
            if self.quantity == 0:
                raise ValueError("Ajuste não pode ter quantidade zero")
            if not self.reason or not self.reason.strip():
                raise ValueError("Ajuste exige uma justificativa (reason)")
        elif self.quantity <= 0:
            raise ValueError("Entrada e saída exigem quantidade maior que zero")
        return self


class MovementRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    product_id: int
    type: MovementType
    quantity: Decimal
    reason: str | None
    created_at: datetime


class BalanceRead(BaseModel):
    product_id: int
    product_name: str
    base_unit: str
    balance: Decimal