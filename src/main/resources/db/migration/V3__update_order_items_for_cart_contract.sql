ALTER TABLE order_items
ALTER COLUMN product_id TYPE VARCHAR(255)
        USING product_id::TEXT,
    ALTER COLUMN merchant_id TYPE VARCHAR(255)
        USING merchant_id::TEXT;

ALTER TABLE order_items
    ADD COLUMN cart_item_id UUID,
    ADD COLUMN variant_id VARCHAR(255),
    ADD COLUMN product_image_snapshot VARCHAR(1000),
    ADD COLUMN line_total_paid NUMERIC(12, 2);