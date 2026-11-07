CREATE TABLE suppliers (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(254),
    phone VARCHAR(32),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_suppliers_name_not_blank CHECK (length(btrim(name)) > 0)
);

CREATE INDEX ix_suppliers_active_created ON suppliers(active, created_at DESC);
CREATE INDEX ix_suppliers_name_lower ON suppliers(lower(name));
