-- V32/V36 allowed these slots but did not create the rows required by SiteMediaMetadataService.
INSERT INTO site_media (slot, updated_at) VALUES
    ('SEO_SHARE', now()),
    ('BLOG_HERO', now()),
    ('BLOG_HERO_DARK', now())
ON CONFLICT (slot) DO NOTHING;
