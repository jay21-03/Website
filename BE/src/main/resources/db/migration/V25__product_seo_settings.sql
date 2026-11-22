ALTER TABLE products
    ADD COLUMN seo_title varchar(180),
    ADD COLUMN seo_description varchar(500);

CREATE TABLE product_slug_redirects (
    old_slug varchar(300) PRIMARY KEY,
    product_id bigint NOT NULL REFERENCES products(id),
    created_at timestamptz NOT NULL DEFAULT now()
);
