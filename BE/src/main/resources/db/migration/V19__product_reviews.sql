CREATE TABLE product_reviews (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    product_id BIGINT NOT NULL REFERENCES products(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    body TEXT NOT NULL,
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_product_review_user_product UNIQUE (user_id, product_id),
    CONSTRAINT chk_product_review_body CHECK (char_length(btrim(body)) BETWEEN 10 AND 1000)
);
CREATE INDEX idx_product_reviews_public ON product_reviews(product_id, created_at DESC, id DESC) WHERE hidden = FALSE;
CREATE INDEX idx_product_reviews_admin ON product_reviews(created_at DESC, id DESC);
