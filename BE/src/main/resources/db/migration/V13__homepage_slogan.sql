CREATE TABLE homepage_settings (
    id SMALLINT PRIMARY KEY,
    slogan_vi VARCHAR(200) NOT NULL,
    slogan_en VARCHAR(200) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_homepage_settings_singleton CHECK (id = 1),
    CONSTRAINT chk_homepage_slogan_vi_not_blank CHECK (length(btrim(slogan_vi)) > 0),
    CONSTRAINT chk_homepage_slogan_en_not_blank CHECK (length(btrim(slogan_en)) > 0)
);

INSERT INTO homepage_settings(id, slogan_vi, slogan_en, updated_at)
VALUES (
    1,
    'Tinh hoa gốm Chăm – Gìn giữ hồn di sản',
    'The essence of Cham pottery – preserving the soul of heritage',
    now()
);
