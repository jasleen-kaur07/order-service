CREATE TABLE order_items (
    id                    UUID          NOT NULL,
    order_id              UUID          NOT NULL,
    product_id            UUID          NOT NULL,
    merchant_id           UUID          NOT NULL,
    product_name_snapshot VARCHAR(255)  NOT NULL,
    quantity              INTEGER       NOT NULL,
    unit_price_paid       NUMERIC(12, 2) NOT NULL,
    created_at            TIMESTAMPTZ   NOT NULL,
    updated_at            TIMESTAMPTZ   NOT NULL,

    CONSTRAINT pk_order_items PRIMARY KEY (id),

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,

    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

COMMENT ON TABLE  order_items                       IS 'Line items of an order. Immutable checkout snapshots; deleted with their order (cascade).';
COMMENT ON COLUMN order_items.product_name_snapshot IS 'Product name as sold; not a live lookup into Product Service.';
COMMENT ON COLUMN order_items.unit_price_paid       IS 'Unit price actually charged; not a live lookup into Merchant Service.';
COMMENT ON COLUMN order_items.merchant_id           IS 'Selling merchant (UUID shared over REST); resolved to "sold by ..." via Merchant/User Service if needed.';
