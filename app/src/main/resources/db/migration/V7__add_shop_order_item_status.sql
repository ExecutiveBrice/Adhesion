ALTER TABLE shop_order_items
    ADD COLUMN status varchar(32) NOT NULL DEFAULT 'PENDING';

UPDATE shop_order_items AS item
SET status = (
    SELECT CASE
        WHEN orders.status = 'COMPLETED' THEN 'COMPLETED'
        WHEN orders.status IN ('CANCELLED', 'EXPIRED', 'REFUNDED') THEN 'CANCELLED'
        WHEN orders.status = 'PROCESSING' THEN 'PROCESSING'
        ELSE 'PENDING'
    END
    FROM shop_orders AS orders
    WHERE orders.id = item.order_id
);

ALTER TABLE shop_order_items
    ADD CONSTRAINT ck_shop_order_item_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'CANCELLED'));
