ALTER TABLE notifications DROP CONSTRAINT notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check
    CHECK (type IN ('NEW_ORDER','PAYMENT_SUCCESS','PAYMENT_FAILED','LOW_STOCK','OUT_OF_STOCK','CHAT_MESSAGE'));
