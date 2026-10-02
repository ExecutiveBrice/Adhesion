ALTER TABLE shop_product_variants DROP CONSTRAINT IF EXISTS ck_shop_variant_stock;

-- Existing variants without a count start at zero and remain manageable without a stock switch.
UPDATE shop_product_variants
SET stock_tracked = true, stock_on_hand = 0
WHERE stock_tracked = false;

ALTER TABLE shop_product_variants ADD CONSTRAINT ck_shop_variant_stock CHECK (
    (stock_tracked = false AND stock_on_hand IS NULL AND stock_reserved = 0)
    OR (stock_tracked = true AND stock_on_hand IS NOT NULL AND stock_reserved >= 0)
);
