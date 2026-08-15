CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    order_code VARCHAR(40) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    receiver_name VARCHAR(255) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    email VARCHAR(320) NOT NULL,
    address VARCHAR(500) NOT NULL,
    note TEXT,
    total_amount BIGINT NOT NULL CHECK (total_amount > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('NEW','CONFIRMED','COMPLETED','CANCELLED')),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE order_items (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL,
    product_name_vi VARCHAR(255) NOT NULL,
    product_name_en VARCHAR(255) NOT NULL,
    base_price BIGINT NOT NULL CHECK (base_price > 0),
    selling_price BIGINT NOT NULL CHECK (selling_price > 0),
    quantity INTEGER NOT NULL CHECK (quantity >= 1),
    total_price BIGINT NOT NULL CHECK (total_price > 0),
    CONSTRAINT uk_order_items_order_product UNIQUE (order_id, product_id)
);

CREATE TABLE payments (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    order_id BIGINT NOT NULL REFERENCES orders(id),
    provider VARCHAR(16) NOT NULL CHECK (provider = 'PAYOS'),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','PAID','FAILED','CANCELLED','EXPIRED','REFUNDED')),
    amount BIGINT NOT NULL CHECK (amount > 0),
    provider_payment_link_id VARCHAR(128),
    external_transaction_identifier VARCHAR(128),
    checkout_url VARCHAR(2048),
    qr_code TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    paid_at TIMESTAMPTZ,
    refunded_at TIMESTAMPTZ,
    refunded_by_user_id BIGINT REFERENCES users(id),
    manual_refund_note VARCHAR(500),
    manual_resolution_required BOOLEAN NOT NULL DEFAULT FALSE,
    manual_resolution_note TEXT,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_payments_order UNIQUE (order_id),
    CONSTRAINT uk_payments_provider_link UNIQUE (provider_payment_link_id),
    CONSTRAINT uk_payments_external_tx UNIQUE (external_transaction_identifier),
    CONSTRAINT chk_payments_expiry CHECK (expires_at > created_at)
);

CREATE TABLE checkout_operations (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    user_id BIGINT NOT NULL REFERENCES users(id),
    idempotency_key VARCHAR(64) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    state VARCHAR(32) NOT NULL CHECK (state IN ('STARTED','LOCAL_PREPARED','PAYOS_CREATING','PAYOS_CREATED','COMPLETED','FAILED')),
    order_id BIGINT REFERENCES orders(id),
    payment_id BIGINT REFERENCES payments(id),
    processing_started_at TIMESTAMPTZ,
    retry_count INTEGER NOT NULL DEFAULT 0 CHECK (retry_count >= 0),
    last_error_code VARCHAR(64),
    state_changed_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_checkout_user_key UNIQUE (user_id, idempotency_key),
    CONSTRAINT uk_checkout_order UNIQUE (order_id),
    CONSTRAINT uk_checkout_payment UNIQUE (payment_id),
    CONSTRAINT chk_checkout_request_hash CHECK (request_hash ~ '^[0-9a-f]{64}$')
);

CREATE TABLE checkout_operation_items (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    checkout_operation_id BIGINT NOT NULL REFERENCES checkout_operations(id) ON DELETE CASCADE,
    cart_item_id BIGINT NOT NULL,
    cart_item_version BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity >= 1),
    CONSTRAINT uk_checkout_operation_item UNIQUE (checkout_operation_id, cart_item_id)
);

CREATE INDEX idx_orders_user_created ON orders (user_id, created_at DESC);
CREATE INDEX idx_orders_status_created ON orders (status, created_at DESC);
CREATE INDEX idx_orders_completed_at ON orders (completed_at);
CREATE INDEX idx_payments_status_expiry ON payments (status, expires_at);
CREATE INDEX idx_payments_status_updated ON payments (status, updated_at);
CREATE INDEX idx_checkout_state_changed ON checkout_operations (state, state_changed_at);
