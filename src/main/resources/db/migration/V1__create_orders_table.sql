CREATE TABLE orders (
    id             UUID          NOT NULL,
    cart_id        UUID          NOT NULL,
    user_id        UUID          NOT NULL,
    email_snapshot VARCHAR(255)  NOT NULL,
    ship_line1     VARCHAR(255)  NOT NULL,
    ship_line2     VARCHAR(255),
    ship_city      VARCHAR(100)  NOT NULL,
    ship_state     VARCHAR(100)  NOT NULL,
    ship_country   VARCHAR(100)  NOT NULL,
    ship_pincode   VARCHAR(20)   NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    total_price    NUMERIC(12, 2) NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL,

    CONSTRAINT pk_orders PRIMARY KEY (id),

    CONSTRAINT uq_orders_cart_id UNIQUE (cart_id),

    CONSTRAINT ck_orders_status CHECK (status IN ('CREATED', 'CANCELLED'))
);

CREATE INDEX idx_orders_user_id ON orders (user_id);

COMMENT ON TABLE  orders                IS 'Placed orders. Every column except status is an immutable checkout snapshot.';
COMMENT ON COLUMN orders.cart_id        IS 'The checkout that produced this order; unique, so a retry cannot duplicate it.';
COMMENT ON COLUMN orders.email_snapshot IS 'Customer email at checkout, snapshotted from User Service for the confirmation mail.';
COMMENT ON COLUMN orders.status         IS 'CREATED or CANCELLED (ck_orders_status). The only mutable column.';
COMMENT ON COLUMN orders.total_price    IS 'Amount actually charged at checkout.';
