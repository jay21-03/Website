CREATE TABLE product_images (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    product_id BIGINT NOT NULL REFERENCES products(id),
    object_key VARCHAR(512) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    sort_order SMALLINT NOT NULL,
    is_thumbnail BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_product_images_object_key UNIQUE (object_key),
    CONSTRAINT uk_product_images_product_order UNIQUE (product_id, sort_order),
    CONSTRAINT chk_product_images_content_type CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    CONSTRAINT chk_product_images_file_size CHECK (file_size_bytes > 0),
    CONSTRAINT chk_product_images_sort_order CHECK (sort_order >= 0)
);

CREATE UNIQUE INDEX uk_product_images_thumbnail
    ON product_images(product_id)
    WHERE is_thumbnail = true;

CREATE INDEX idx_product_images_order
    ON product_images(product_id, sort_order);
