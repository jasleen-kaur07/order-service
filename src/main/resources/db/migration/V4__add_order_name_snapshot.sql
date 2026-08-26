ALTER TABLE orders
    ADD COLUMN first_name_snapshot VARCHAR(100),
    ADD COLUMN last_name_snapshot  VARCHAR(100);

COMMENT ON COLUMN orders.first_name_snapshot IS 'Customer first name at checkout, snapshotted from Cart Service (which resolves it from User Service). Nullable - a profile may not have a name set.';
COMMENT ON COLUMN orders.last_name_snapshot  IS 'Customer last name at checkout, snapshotted the same way.';