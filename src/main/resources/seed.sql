BEGIN;

TRUNCATE TABLE reservation_items, reservations, products
RESTART IDENTITY CASCADE;

INSERT INTO products (sku, name, total_stock, reserved_stock)
VALUES
    ('NOTE-001', 'Notebook', 10, 0),
    ('PEN-001', 'Pen', 5, 0),
    ('BAG-001', 'Backpack', 2, 0);

COMMIT;