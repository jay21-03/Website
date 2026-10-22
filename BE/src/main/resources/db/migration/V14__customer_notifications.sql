CREATE TABLE customer_notification_recipients (
    notification_id BIGINT NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    PRIMARY KEY (notification_id, user_id),
    CONSTRAINT chk_customer_notification_read CHECK (
        (is_read = FALSE AND read_at IS NULL) OR (is_read = TRUE AND read_at IS NOT NULL)
    )
);

CREATE INDEX idx_customer_notifications_user_read
    ON customer_notification_recipients (user_id, is_read, notification_id DESC);
