-- H2-compatible schema for integration tests (subset of Flyway V1–V9; idempotent for shared in-memory DB).

CREATE TABLE IF NOT EXISTS profiles (
    id          UUID NOT NULL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    id       UUID NOT NULL PRIMARY KEY,
    user_id  UUID NOT NULL REFERENCES profiles (id),
    role     VARCHAR(32) NOT NULL,
    CONSTRAINT user_roles_user_role_unique UNIQUE (user_id, role)
);

CREATE TABLE IF NOT EXISTS warehouses (
    id          UUID NOT NULL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    location    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    id                    UUID NOT NULL PRIMARY KEY,
    name                  VARCHAR(255)   NOT NULL,
    sku                   VARCHAR(255)   NOT NULL UNIQUE,
    category              VARCHAR(255)   NOT NULL,
    price                 DECIMAL(12, 2) NOT NULL,
    default_warehouse_id  UUID           NOT NULL REFERENCES warehouses (id),
    low_stock_threshold   INT            NOT NULL DEFAULT 0,
    images                VARCHAR(255) ARRAY NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stock_movements (
    id                  UUID NOT NULL PRIMARY KEY,
    type                VARCHAR(32) NOT NULL,
    product_id          UUID        NOT NULL REFERENCES products (id),
    qty                 INT         NOT NULL,
    from_warehouse_id   UUID        REFERENCES warehouses (id),
    to_warehouse_id     UUID        REFERENCES warehouses (id),
    user_id             UUID        NOT NULL REFERENCES profiles (id),
    provider            VARCHAR(255),
    recipient           VARCHAR(255),
    note                VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_entries (
    id          UUID NOT NULL PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES profiles (id),
    action      VARCHAR(255) NOT NULL,
    entity      VARCHAR(255) NOT NULL,
    entity_id   UUID         NOT NULL,
    details     VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transactions (
    id          UUID NOT NULL PRIMARY KEY,
    items       JSON         NOT NULL,
    subtotal    DECIMAL(12, 2) NOT NULL,
    tax         DECIMAL(12, 2) NOT NULL,
    total       DECIMAL(12, 2) NOT NULL,
    cashier_id  UUID         NOT NULL REFERENCES profiles (id),
    status      VARCHAR(32)  NOT NULL DEFAULT 'completed',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP VIEW IF EXISTS product_stock_summary;
DROP VIEW IF EXISTS inventory_balances;

CREATE VIEW inventory_balances AS
SELECT
    product_id,
    warehouse_id,
    CAST(SUM(delta) AS INT) AS quantity
FROM (
    SELECT product_id, to_warehouse_id AS warehouse_id, qty AS delta
    FROM stock_movements WHERE type = 'IN'
    UNION ALL
    SELECT product_id, from_warehouse_id, -qty FROM stock_movements WHERE type = 'OUT'
    UNION ALL
    SELECT product_id, from_warehouse_id, -qty FROM stock_movements WHERE type = 'TRANSFER'
    UNION ALL
    SELECT product_id, to_warehouse_id, qty FROM stock_movements WHERE type = 'TRANSFER'
    UNION ALL
    SELECT product_id, from_warehouse_id, -qty
    FROM stock_movements WHERE type = 'ADJUSTMENT' AND from_warehouse_id IS NOT NULL
    UNION ALL
    SELECT product_id, to_warehouse_id, qty
    FROM stock_movements WHERE type = 'ADJUSTMENT' AND to_warehouse_id IS NOT NULL
) AS deltas
GROUP BY product_id, warehouse_id;

CREATE VIEW product_stock_summary AS
SELECT
    p.id AS product_id,
    p.default_warehouse_id AS warehouse_id,
    CAST(COALESCE(ib.quantity, 0) AS INT) AS quantity
FROM products p
LEFT JOIN inventory_balances ib
    ON ib.product_id = p.id AND ib.warehouse_id = p.default_warehouse_id;
