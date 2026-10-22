CREATE TABLE site_presentation (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    content_json JSONB NOT NULL CHECK (jsonb_typeof(content_json) = 'object'),
    updated_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE site_media DROP CONSTRAINT chk_site_media_slot;
ALTER TABLE site_media ADD CONSTRAINT chk_site_media_slot CHECK (slot IN (
    'HOME_HERO', 'HOME_STORY', 'HOME_SOCIAL_1', 'HOME_SOCIAL_2',
    'HOME_SOCIAL_3', 'HOME_SOCIAL_4', 'HOME_SOCIAL_5', 'HOME_SOCIAL_6',
    'ABOUT_HERO', 'ABOUT_PORTRAIT', 'ABOUT_CERTIFICATE',
    'CATALOG_HERO', 'WORKSHOP_HERO', 'SUPPORT_HERO', 'CHECKOUT_HERO', 'SITE_LOGO'
));

INSERT INTO site_media (slot, updated_at) VALUES
    ('CATALOG_HERO', now()), ('WORKSHOP_HERO', now()), ('SUPPORT_HERO', now()), ('CHECKOUT_HERO', now()),
    ('SITE_LOGO', now())
ON CONFLICT (slot) DO NOTHING;
