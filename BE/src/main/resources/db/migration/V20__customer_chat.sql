CREATE TABLE chat_conversations (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    guest_token_hash CHAR(64),
    guest_name VARCHAR(100),
    guest_email VARCHAR(320),
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    CONSTRAINT chk_chat_owner CHECK ((user_id IS NOT NULL AND guest_token_hash IS NULL) OR (user_id IS NULL AND guest_token_hash IS NOT NULL)),
    CONSTRAINT chk_chat_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT chk_chat_closed_at CHECK ((status = 'OPEN' AND closed_at IS NULL) OR (status = 'CLOSED' AND closed_at IS NOT NULL))
);

CREATE UNIQUE INDEX uk_chat_open_user ON chat_conversations(user_id) WHERE status = 'OPEN' AND user_id IS NOT NULL;
CREATE UNIQUE INDEX uk_chat_open_guest ON chat_conversations(guest_token_hash) WHERE status = 'OPEN' AND guest_token_hash IS NOT NULL;
CREATE INDEX idx_chat_admin_list ON chat_conversations(status, updated_at DESC);
CREATE INDEX idx_chat_retention ON chat_conversations(closed_at) WHERE status = 'CLOSED';

CREATE TABLE chat_messages (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    conversation_id BIGINT NOT NULL REFERENCES chat_conversations(id) ON DELETE CASCADE,
    sender VARCHAR(10) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_chat_sender CHECK (sender IN ('CUSTOMER', 'ADMIN')),
    CONSTRAINT chk_chat_body CHECK (char_length(btrim(body)) BETWEEN 1 AND 2000)
);

CREATE INDEX idx_chat_messages_conversation ON chat_messages(conversation_id, id DESC);
