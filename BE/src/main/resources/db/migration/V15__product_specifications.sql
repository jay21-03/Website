ALTER TABLE products
    ADD COLUMN size_cm VARCHAR(120),
    ADD COLUMN weight_grams BIGINT;

ALTER TABLE products
    ADD CONSTRAINT chk_products_weight_grams
    CHECK (weight_grams IS NULL OR weight_grams > 0);
