CREATE TABLE workshop_offerings (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    price_amount BIGINT NOT NULL,
    duration_minutes INTEGER NOT NULL,
    max_participants INTEGER NOT NULL,
    image_url VARCHAR(1024),
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_workshop_offerings_price CHECK (price_amount >= 0),
    CONSTRAINT chk_workshop_offerings_duration CHECK (duration_minutes > 0),
    CONSTRAINT chk_workshop_offerings_capacity CHECK (max_participants BETWEEN 1 AND 100),
    CONSTRAINT chk_workshop_offerings_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_workshop_offerings_status
    ON workshop_offerings(status);

CREATE TABLE workshop_bookings (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    workshop_id BIGINT REFERENCES workshop_offerings(id),
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    preferred_at TIMESTAMPTZ NOT NULL,
    participants INTEGER NOT NULL,
    note VARCHAR(1000),
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_workshop_bookings_participants CHECK (participants BETWEEN 1 AND 30),
    CONSTRAINT chk_workshop_bookings_status CHECK (status IN ('NEW', 'CONFIRMED', 'CANCELLED', 'COMPLETED'))
);

CREATE INDEX idx_workshop_bookings_status_preferred_at
    ON workshop_bookings(status, preferred_at);

CREATE INDEX idx_workshop_bookings_email
    ON workshop_bookings(email);

CREATE INDEX idx_workshop_bookings_workshop_id
    ON workshop_bookings(workshop_id);
