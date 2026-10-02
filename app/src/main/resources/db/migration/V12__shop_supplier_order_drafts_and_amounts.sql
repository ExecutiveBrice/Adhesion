ALTER TABLE shop_supplier_orders DROP CONSTRAINT ck_shop_supplier_order_status;
ALTER TABLE shop_supplier_orders ADD CONSTRAINT ck_shop_supplier_order_status
    CHECK (status IN ('DRAFT', 'ORDERED', 'RECEIVED'));

ALTER TABLE shop_supplier_order_lines ADD COLUMN unit_cost_amount_cents bigint;
ALTER TABLE shop_supplier_order_lines ADD COLUMN unit_cost_currency varchar(3);
ALTER TABLE shop_supplier_order_lines ADD COLUMN expected_need integer;
ALTER TABLE shop_supplier_order_lines ADD COLUMN extra_approved boolean NOT NULL DEFAULT false;
ALTER TABLE shop_supplier_order_lines ADD CONSTRAINT ck_shop_supplier_line_cost
    CHECK ((unit_cost_amount_cents IS NULL AND unit_cost_currency IS NULL)
        OR (unit_cost_amount_cents >= 0 AND unit_cost_currency IS NOT NULL));
