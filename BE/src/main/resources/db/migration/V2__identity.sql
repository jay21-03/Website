CREATE TABLE users (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    google_id VARCHAR(128) NOT NULL,
    email VARCHAR(320) NOT NULL,
    full_name VARCHAR(255),
    avatar_url VARCHAR(1024),
    role VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_users_google_id UNIQUE (google_id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'BLOCKED'))
);

CREATE INDEX idx_users_role_status ON users (role, status);
