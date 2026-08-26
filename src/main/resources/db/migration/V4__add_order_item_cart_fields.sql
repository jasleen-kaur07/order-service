ALTER TABLE order_items
    ADD COLUMN cart_item_id UUID,
    ADD COLUMN variant_id VARCHAR(255),
    ADD COLUMN product_image_snapshot VARCHAR(1000),
    ADD COLUMN line_total_paid NUMERIC(12, 2);