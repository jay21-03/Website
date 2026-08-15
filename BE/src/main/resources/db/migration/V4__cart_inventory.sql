CREATE TABLE carts (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    user_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_carts_user UNIQUE (user_id)
);

CREATE TABLE cart_items (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    cart_id BIGINT NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT chk_cart_items_quantity CHECK (quantity >= 1)
);

CREATE TABLE inventories (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity BIGINT NOT NULL DEFAULT 0,
    reserved_quantity BIGINT NOT NULL DEFAULT 0,
    low_stock_threshold BIGINT NOT NULL DEFAULT 5,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_inventories_product UNIQUE (product_id),
    CONSTRAINT chk_inventories_values CHECK (
        quantity >= 0 AND reserved_quantity >= 0
        AND quantity - reserved_quantity >= 0
        AND low_stock_threshold >= 0
    )
);

CREATE TABLE inventory_transactions (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    product_id BIGINT NOT NULL REFERENCES products(id),
    type VARCHAR(24) NOT NULL,
    quantity_delta BIGINT NOT NULL,
    reserved_quantity_delta BIGINT NOT NULL,
    before_quantity BIGINT NOT NULL,
    after_quantity BIGINT NOT NULL,
    before_reserved_quantity BIGINT NOT NULL,
    after_reserved_quantity BIGINT NOT NULL,
    reference_type VARCHAR(32),
    reference_id BIGINT,
    business_key VARCHAR(160),
    reason VARCHAR(500),
    created_by_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_inventory_tx_type CHECK (type IN ('IMPORT','ADJUSTMENT','RESERVE','RELEASE','SALE','CANCEL_ORDER'))
);

CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);
CREATE INDEX idx_inventory_product ON inventories (product_id);
CREATE INDEX idx_inv_tx_product_created ON inventory_transactions (product_id, created_at DESC);
CREATE UNIQUE INDEX uk_inv_tx_business_key ON inventory_transactions (business_key) WHERE business_key IS NOT NULL;
