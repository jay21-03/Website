ALTER TABLE blog_posts
    ADD COLUMN title_en varchar(180),
    ADD COLUMN body_markdown_en text,
    ADD COLUMN seo_title_en varchar(180),
    ADD COLUMN seo_description_en varchar(500),
    ADD COLUMN og_title_en varchar(180),
    ADD COLUMN og_description_en varchar(500);
