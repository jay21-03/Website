CREATE TABLE support_settings (
    id SMALLINT PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    zalo_phone VARCHAR(32) NOT NULL,
    secondary_phone VARCHAR(32),
    facebook_url VARCHAR(1024),
    address VARCHAR(500) NOT NULL,
    map_url VARCHAR(1024),
    opening_hours VARCHAR(255),
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_support_settings_singleton CHECK (id = 1)
);

INSERT INTO support_settings (
    id,
    email,
    zalo_phone,
    secondary_phone,
    facebook_url,
    address,
    map_url,
    opening_hours,
    updated_at
) VALUES (
    1,
    'Cosogombautrucdangxem@gmail.com',
    '0343478155',
    '0966477160',
    'https://www.facebook.com/share/18jwSfSPD7/?mibextid=wwXIfr',
    '35 Bàu Trúc, thôn Vĩnh Thuận, xã Ninh Phước, tỉnh Khánh Hòa',
    'https://www.google.com/maps/search/?api=1&query=35%20B%C3%A0u%20Tr%C3%BAc%2C%20th%C3%B4n%20V%C4%A9nh%20Thu%E1%BA%ADn%2C%20x%C3%A3%20Ninh%20Ph%C6%B0%E1%BB%9Bc%2C%20t%E1%BB%89nh%20Kh%C3%A1nh%20H%C3%B2a',
    '7:00 - 17:00 hằng ngày',
    now()
);
