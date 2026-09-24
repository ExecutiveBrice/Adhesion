ALTER TABLE shop_orders ADD COLUMN checkout_key varchar(100);

CREATE UNIQUE INDEX uk_shop_order_customer_checkout_key
    ON shop_orders (customer_user_id, checkout_key);
