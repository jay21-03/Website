CREATE TABLE blog_posts (
    id bigint PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    slug varchar(160) NOT NULL UNIQUE,
    title varchar(180) NOT NULL,
    body_markdown text NOT NULL,
    seo_title varchar(180),
    seo_description varchar(500),
    og_title varchar(180),
    og_description varchar(500),
    image_slot varchar(64),
    status varchar(16) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','SCHEDULED','PUBLISHED')),
    publish_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_blog_posts_public ON blog_posts(status, publish_at);
