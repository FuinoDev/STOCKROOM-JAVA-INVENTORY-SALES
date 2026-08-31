CREATE TABLE IF NOT EXISTS users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(300) NOT NULL,
    role VARCHAR(10) NOT NULL CHECK (role IN ('ADMIN','STAFF')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    session_version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS users_username_unique ON users (LOWER(username));
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(80) NOT NULL CHECK (LENGTH(TRIM(name)) > 0)
);
CREATE UNIQUE INDEX IF NOT EXISTS categories_name_unique ON categories (LOWER(name));
CREATE TABLE IF NOT EXISTS products (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(120) NOT NULL CHECK (LENGTH(TRIM(name)) > 0),
    category_id BIGINT NOT NULL REFERENCES categories(id),
    price NUMERIC(14,2) NOT NULL CHECK (price > 0 AND price <= 999999999.99),
    quantity INTEGER NOT NULL CHECK (quantity BETWEEN 0 AND 1000000),
    minimum_stock INTEGER NOT NULL CHECK (minimum_stock BETWEEN 0 AND 1000000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS products_name_unique ON products (LOWER(name));
CREATE INDEX IF NOT EXISTS products_category_idx ON products(category_id);
CREATE TABLE IF NOT EXISTS sales (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    request_id UUID NOT NULL UNIQUE,
    total NUMERIC(14,2) NOT NULL CHECK (total > 0),
    payment NUMERIC(14,2) NOT NULL CHECK (payment >= total),
    change_amount NUMERIC(14,2) NOT NULL CHECK (change_amount = payment - total),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS sales_date_idx ON sales(created_at);
CREATE INDEX IF NOT EXISTS sales_user_date_idx ON sales(user_id,created_at);
CREATE TABLE IF NOT EXISTS sale_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    product_name VARCHAR(120) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price NUMERIC(14,2) NOT NULL CHECK (price > 0),
    subtotal NUMERIC(14,2) NOT NULL CHECK (subtotal = price * quantity),
    UNIQUE(sale_id,product_id)
);
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('STOCK_IN','SALE','ADJUSTMENT')),
    quantity INTEGER NOT NULL CHECK (quantity <> 0),
    previous_stock INTEGER NOT NULL CHECK (previous_stock >= 0),
    new_stock INTEGER NOT NULL CHECK (new_stock >= 0 AND new_stock = previous_stock + quantity),
    user_id BIGINT NOT NULL REFERENCES users(id),
    sale_id BIGINT REFERENCES sales(id),
    note VARCHAR(300) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK ((type = 'SALE' AND quantity < 0 AND sale_id IS NOT NULL)
        OR (type = 'STOCK_IN' AND quantity > 0 AND sale_id IS NULL)
        OR (type = 'ADJUSTMENT' AND sale_id IS NULL))
);
CREATE INDEX IF NOT EXISTS movements_product_date_idx ON inventory_transactions(product_id,created_at);
CREATE INDEX IF NOT EXISTS sale_items_sale_idx ON sale_items(sale_id);
INSERT INTO categories(name) VALUES ('Beverages'),('Food & pantry'),('School supplies'),('Electronics'),('Household'),('Others') ON CONFLICT DO NOTHING;
