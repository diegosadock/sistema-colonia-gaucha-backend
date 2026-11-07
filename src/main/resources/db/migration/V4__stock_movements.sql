CREATE TABLE stock_movements (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    supplier_id UUID REFERENCES suppliers(id) ON DELETE RESTRICT,
    movement_type VARCHAR(20) NOT NULL,
    quantity NUMERIC(18,3) NOT NULL,
    reason VARCHAR(255),
    unit_of_measure VARCHAR(2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    CONSTRAINT ck_stock_movement_type CHECK (movement_type IN ('ENTRY', 'EXIT', 'ADJUSTMENT')),
    CONSTRAINT ck_stock_quantity_nonzero CHECK (quantity <> 0),
    CONSTRAINT ck_stock_movement_quantity_sign CHECK (
        (movement_type IN ('ENTRY', 'EXIT') AND quantity > 0)
        OR movement_type = 'ADJUSTMENT'
    ),
    CONSTRAINT ck_stock_adjustment_reason CHECK (
        movement_type <> 'ADJUSTMENT' OR (reason IS NOT NULL AND length(btrim(reason)) > 0)
    ),
    CONSTRAINT ck_stock_supplier_entry_only CHECK (supplier_id IS NULL OR movement_type = 'ENTRY'),
    CONSTRAINT ck_stock_unit CHECK (unit_of_measure IN ('g', 'ml', 'un'))
);

CREATE INDEX ix_stock_product_created ON stock_movements(product_id, created_at DESC, id);
CREATE INDEX ix_stock_type_created ON stock_movements(movement_type, created_at DESC);
CREATE INDEX ix_stock_supplier_created ON stock_movements(supplier_id, created_at DESC) WHERE supplier_id IS NOT NULL;

CREATE FUNCTION prevent_stock_movement_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'stock movement history is immutable' USING ERRCODE = '55000';
END;
$$;

CREATE TRIGGER stock_movements_immutable
BEFORE UPDATE OR DELETE ON stock_movements
FOR EACH ROW EXECUTE FUNCTION prevent_stock_movement_mutation();
