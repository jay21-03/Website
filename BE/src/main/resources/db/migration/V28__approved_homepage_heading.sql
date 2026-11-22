-- Replace only the two legacy headings; leave any merchant-edited heading intact.
UPDATE homepage_settings
SET slogan_vi = 'Gốm Bàu Trúc – Tinh hoa gốm Chăm từ đôi tay nghệ nhân', updated_at = now()
WHERE id = 1 AND slogan_vi IN (
    'Tinh hoa gốm Chăm – Gìn giữ hồn di sản',
    'Tinh hoa gốm Chăm – Gìn giữ hồn di sản Việt Nam'
);
