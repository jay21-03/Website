CREATE INDEX idx_orders_reporting_completed ON orders (status, completed_at) WHERE completed_at IS NOT NULL;
CREATE INDEX idx_payments_reporting_order ON payments (status, order_id);
CREATE INDEX idx_order_items_reporting_product ON order_items (product_id, order_id);
