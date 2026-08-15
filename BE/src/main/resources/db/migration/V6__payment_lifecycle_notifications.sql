CREATE TABLE notifications (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    type VARCHAR(32) NOT NULL CHECK (type IN ('NEW_ORDER','PAYMENT_SUCCESS','PAYMENT_FAILED','LOW_STOCK','OUT_OF_STOCK')),
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    reference_type VARCHAR(32),
    reference_id BIGINT,
    metadata JSONB,
    dedup_key VARCHAR(180) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE notification_recipients (
    id BIGINT PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    notification_id BIGINT NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    admin_id BIGINT NOT NULL REFERENCES users(id),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    CONSTRAINT uk_notification_recipient UNIQUE (notification_id, admin_id),
    CONSTRAINT chk_notification_read CHECK (
        (is_read = FALSE AND read_at IS NULL) OR
        (is_read = TRUE AND read_at IS NOT NULL)
    )
);

CREATE INDEX idx_notifications_created ON notifications (created_at DESC);
CREATE INDEX idx_nr_admin_read ON notification_recipients (admin_id, is_read, notification_id DESC);

