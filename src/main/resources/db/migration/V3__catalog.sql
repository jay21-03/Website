CREATE TABLE collections (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    name_vi VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    description_vi TEXT,
    description_en TEXT,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_collections_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE products (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    name_vi VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    description_vi TEXT,
    description_en TEXT,
    base_price BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    collection_id BIGINT NOT NULL REFERENCES collections(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_products_base_price CHECK (base_price > 0),
    CONSTRAINT chk_products_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE discounts (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    product_id BIGINT NOT NULL REFERENCES products(id),
    discount_type VARCHAR(20) NOT NULL,
    discount_value NUMERIC(19,4) NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_discounts_product UNIQUE (product_id),
    CONSTRAINT chk_discounts_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_PRICE')),
    CONSTRAINT chk_discounts_value CHECK (discount_value > 0),
    CONSTRAINT chk_discounts_time CHECK (start_at < end_at)
);

CREATE INDEX idx_products_public ON products (status, deleted_at, created_at);
CREATE INDEX idx_products_collection ON products (collection_id, status, deleted_at);
CREATE INDEX idx_discounts_effective ON discounts (product_id, is_active, start_at, end_at);
