from pydantic import BaseModel, ConfigDict, Field


class SupplierCreate(BaseModel):
    name: str = Field(min_length=2, max_length=120)
    document: str | None = Field(default=None, max_length=18)
    phone: str | None = Field(default=None, max_length=20)
    email: str | None = Field(default=None, max_length=120)


class SupplierUpdate(BaseModel):
    # tudo opcional: no PATCH você manda só o que quer mudar
    name: str | None = Field(default=None, min_length=2, max_length=120)
    document: str | None = Field(default=None, max_length=18)
    phone: str | None = Field(default=None, max_length=20)
    email: str | None = Field(default=None, max_length=120)
    is_active: bool | None = None


class SupplierRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    document: str | None
    phone: str | None
    email: str | None
    is_active: bool