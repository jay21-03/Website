CREATE TABLE about_page_content (
    id SMALLINT PRIMARY KEY,
    content_json JSONB NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_about_page_singleton CHECK (id = 1),
    CONSTRAINT chk_about_page_content_object
        CHECK (jsonb_typeof(content_json) = 'object')
);

ALTER TABLE site_media
    DROP CONSTRAINT chk_site_media_slot;

ALTER TABLE site_media
    ADD CONSTRAINT chk_site_media_slot CHECK (
        slot IN (
            'HOME_HERO',
            'HOME_STORY',
            'HOME_SOCIAL_1',
            'HOME_SOCIAL_2',
            'HOME_SOCIAL_3',
            'HOME_SOCIAL_4',
            'HOME_SOCIAL_5',
            'HOME_SOCIAL_6',
            'ABOUT_HERO',
            'ABOUT_PORTRAIT',
            'ABOUT_CERTIFICATE'
        )
    );

INSERT INTO site_media (slot, updated_at)
VALUES
    ('ABOUT_HERO', now()),
    ('ABOUT_PORTRAIT', now()),
    ('ABOUT_CERTIFICATE', now())
ON CONFLICT (slot) DO NOTHING;
