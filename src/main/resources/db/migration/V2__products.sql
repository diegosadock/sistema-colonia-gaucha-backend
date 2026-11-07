CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    name_key VARCHAR(360) NOT NULL,
    description VARCHAR(255),
    category VARCHAR(80),
    unit_of_measure VARCHAR(2) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_products_name_key UNIQUE (name_key),
    CONSTRAINT ck_products_name_not_blank CHECK (length(btrim(name)) > 0),
    CONSTRAINT ck_products_name_key_not_blank CHECK (length(btrim(name_key)) > 0),
    CONSTRAINT ck_products_unit CHECK (unit_of_measure IN ('g', 'ml', 'un')),
    CONSTRAINT ck_products_price_positive CHECK (price > 0)
);

CREATE INDEX ix_products_active_created ON products(active, created_at DESC);
CREATE INDEX ix_products_category ON products(lower(category));
