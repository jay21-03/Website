CREATE TABLE homepage_content (
    id SMALLINT PRIMARY KEY,
    slogan_vi VARCHAR(255) NOT NULL,
    slogan_en VARCHAR(255) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_homepage_content_singleton CHECK (id = 1)
);

INSERT INTO homepage_content (
    id,
    slogan_vi,
    slogan_en,
    updated_at
) VALUES (
    1,
    'Tinh hoa gốm Chăm – Gìn giữ hồn di sản',
    'The essence of Cham pottery - preserving heritage soul',
    now()
);
