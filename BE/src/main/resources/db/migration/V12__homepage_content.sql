CREATE TABLE site_media (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    slot VARCHAR(64) NOT NULL,
    object_key VARCHAR(512),
    content_type VARCHAR(100),
    file_size_bytes BIGINT,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_site_media_slot UNIQUE (slot),
    CONSTRAINT uk_site_media_object_key UNIQUE (object_key),
    CONSTRAINT chk_site_media_slot CHECK (
        slot IN (
            'HOME_HERO',
            'HOME_STORY',
            'HOME_SOCIAL_1',
            'HOME_SOCIAL_2',
            'HOME_SOCIAL_3',
            'HOME_SOCIAL_4',
            'HOME_SOCIAL_5',
            'HOME_SOCIAL_6'
        )
    ),
    CONSTRAINT chk_site_media_content_type CHECK (
        content_type IS NULL
        OR content_type IN ('image/jpeg', 'image/png', 'image/webp')
    ),
    CONSTRAINT chk_site_media_file_size CHECK (
        file_size_bytes IS NULL OR file_size_bytes > 0
    ),
    CONSTRAINT chk_site_media_metadata CHECK (
        (
            object_key IS NULL
            AND content_type IS NULL
            AND file_size_bytes IS NULL
        )
        OR
        (
            object_key IS NOT NULL
            AND content_type IS NOT NULL
            AND file_size_bytes IS NOT NULL
        )
    )
);

INSERT INTO site_media(slot, updated_at) VALUES
    ('HOME_HERO', now()),
    ('HOME_STORY', now()),
    ('HOME_SOCIAL_1', now()),
    ('HOME_SOCIAL_2', now()),
    ('HOME_SOCIAL_3', now()),
    ('HOME_SOCIAL_4', now()),
    ('HOME_SOCIAL_5', now()),
    ('HOME_SOCIAL_6', now());

CREATE TABLE homepage_featured_products (
    slot SMALLINT PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_homepage_featured_slot CHECK (slot BETWEEN 1 AND 3),
    CONSTRAINT uk_homepage_featured_product UNIQUE (product_id)
);
