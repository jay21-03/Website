-- Remove import metadata only from the two original seeded posts.
-- Preserve the article body and later author edits.

UPDATE blog_posts
SET body_markdown = regexp_replace(
    body_markdown,
    '^Title:[^\r\n]*\r?\nMeta:[^\r\n]*\r?\nURL:[^\r\n]*\r?\n(Đăng:[^\r\n]*\r?\n)?\r?\n',
    ''
)
WHERE slug IN (
    'lang-gom-bau-truc-o-dau',
    'mua-gom-bau-truc-lam-qua-tet'
)
AND body_markdown LIKE 'Title: %';
