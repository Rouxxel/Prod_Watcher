-- H2-compatible schema for integration tests (mirrors squashed Flyway V1–V14; idempotent for shared in-memory DB).
-- Demo ecosystem id matches V21 backfill (tests use DB defaults when entities omit ecosystem_id).

CREATE TABLE IF NOT EXISTS ecosystems (
    id          UUID NOT NULL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL DEFAULT '',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO ecosystems (id, name)
SELECT '33333333-3333-4333-8333-333333333301', 'Acme Demo Test'
WHERE NOT EXISTS (
    SELECT 1 FROM ecosystems WHERE id = '33333333-3333-4333-8333-333333333301'
);

CREATE TABLE IF NOT EXISTS profiles (
    id            UUID NOT NULL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    ecosystem_id  UUID         REFERENCES ecosystems (id),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    id       UUID NOT NULL PRIMARY KEY,
    user_id  UUID NOT NULL REFERENCES profiles (id) ON DELETE CASCADE,
    role     VARCHAR(32) NOT NULL,
    CONSTRAINT user_roles_user_role_unique UNIQUE (user_id, role)
);

CREATE TABLE IF NOT EXISTS warehouses (
    id            UUID NOT NULL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    location      VARCHAR(255) NOT NULL,
    ecosystem_id  UUID         NOT NULL DEFAULT '33333333-3333-4333-8333-333333333301' REFERENCES ecosystems (id),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    id                    UUID NOT NULL PRIMARY KEY,
    name                  VARCHAR(255)   NOT NULL,
    sku                   VARCHAR(255)   NOT NULL,
    category              VARCHAR(255)   NOT NULL,
    price                 DECIMAL(12, 2) NOT NULL CHECK (price >= 0),
    default_warehouse_id  UUID           NOT NULL REFERENCES warehouses (id),
    low_stock_threshold   INT            NOT NULL DEFAULT 0 CHECK (low_stock_threshold >= 0),
    images                VARCHAR(255) ARRAY NOT NULL,
    ecosystem_id          UUID           NOT NULL DEFAULT '33333333-3333-4333-8333-333333333301' REFERENCES ecosystems (id),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT products_ecosystem_sku_unique UNIQUE (ecosystem_id, sku)
);

CREATE TABLE IF NOT EXISTS stock_movements (
    id                  UUID NOT NULL PRIMARY KEY,
    type                VARCHAR(32) NOT NULL,
    product_id          UUID        NOT NULL REFERENCES products (id),
    qty                 INT         NOT NULL CHECK (qty > 0),
    from_warehouse_id   UUID        REFERENCES warehouses (id),
    to_warehouse_id     UUID        REFERENCES warehouses (id),
    user_id             UUID        REFERENCES profiles (id),
    provider            VARCHAR(255),
    recipient           VARCHAR(255),
    note                VARCHAR(255),
    ecosystem_id        UUID        NOT NULL DEFAULT '33333333-3333-4333-8333-333333333301' REFERENCES ecosystems (id),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT stock_movements_type_warehouses_check CHECK (
        (type = 'IN' AND to_warehouse_id IS NOT NULL AND from_warehouse_id IS NULL AND provider IS NOT NULL AND TRIM(provider) <> '' AND recipient IS NULL)
        OR (type = 'OUT' AND from_warehouse_id IS NOT NULL AND to_warehouse_id IS NULL AND provider IS NULL AND recipient IS NOT NULL AND TRIM(recipient) <> '')
        OR (type = 'TRANSFER' AND from_warehouse_id IS NOT NULL AND to_warehouse_id IS NOT NULL AND from_warehouse_id <> to_warehouse_id AND provider IS NULL AND recipient IS NULL)
        OR (type = 'ADJUSTMENT' AND provider IS NULL AND recipient IS NULL AND ((from_warehouse_id IS NOT NULL AND to_warehouse_id IS NULL) OR (to_warehouse_id IS NOT NULL AND from_warehouse_id IS NULL)))
    )
);

CREATE TABLE IF NOT EXISTS audit_entries (
    id            UUID NOT NULL PRIMARY KEY,
    user_id       UUID         REFERENCES profiles (id),
    action        VARCHAR(255) NOT NULL,
    entity        VARCHAR(255) NOT NULL,
    entity_id     UUID         NOT NULL,
    details       VARCHAR(255),
    entity_label  VARCHAR(255),
    ecosystem_id  UUID         NOT NULL DEFAULT '33333333-3333-4333-8333-333333333301' REFERENCES ecosystems (id),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transactions (
    id            UUID NOT NULL PRIMARY KEY,
    items         JSON         NOT NULL,
    subtotal      DECIMAL(12, 2) NOT NULL CHECK (subtotal >= 0),
    tax           DECIMAL(12, 2) NOT NULL CHECK (tax >= 0),
    total         DECIMAL(12, 2) NOT NULL CHECK (total >= 0),
    cashier_id    UUID         NOT NULL REFERENCES profiles (id),
    status        VARCHAR(32)  NOT NULL DEFAULT 'completed' CHECK (status IN ('completed', 'refunded', 'void_')),
    ecosystem_id  UUID         NOT NULL DEFAULT '33333333-3333-4333-8333-333333333301' REFERENCES ecosystems (id),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS workspace_settings (
    id                  UUID DEFAULT '00000000-0000-4000-8000-000000000001' NOT NULL PRIMARY KEY,
    business_name       VARCHAR(255) NOT NULL DEFAULT '',
    contact_email       VARCHAR(255) NOT NULL DEFAULT '',
    tax_rate            DECIMAL(6, 4) NOT NULL DEFAULT 0.16,
    tax_label           VARCHAR(255) NOT NULL DEFAULT 'Tax',
    receipt_footer      VARCHAR(255),
    receipt_logo_url    VARCHAR(255),
    business_mode       VARCHAR(32) NOT NULL DEFAULT 'auto',
    ecosystem_id        UUID NOT NULL DEFAULT '33333333-3333-4333-8333-333333333301' REFERENCES ecosystems (id),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          UUID REFERENCES profiles (id) ON DELETE SET NULL,
    CONSTRAINT workspace_settings_ecosystem_unique UNIQUE (ecosystem_id),
    CONSTRAINT workspace_settings_tax_rate_check CHECK (tax_rate >= 0 AND tax_rate <= 1),
    CONSTRAINT workspace_settings_business_mode_check CHECK (business_mode IN ('auto', 'single', 'multi'))
);

CREATE INDEX IF NOT EXISTS idx_ecosystems_name ON ecosystems (name);
CREATE INDEX IF NOT EXISTS idx_profiles_ecosystem_id ON profiles (ecosystem_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles (user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles (role);
CREATE INDEX IF NOT EXISTS idx_warehouses_name ON warehouses (name);
CREATE INDEX IF NOT EXISTS idx_warehouses_ecosystem_id ON warehouses (ecosystem_id);
CREATE INDEX IF NOT EXISTS idx_products_sku ON products (sku);
CREATE INDEX IF NOT EXISTS idx_products_category ON products (category);
CREATE INDEX IF NOT EXISTS idx_products_default_warehouse_id ON products (default_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_products_ecosystem_id ON products (ecosystem_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_product_id ON stock_movements (product_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_from_warehouse_id ON stock_movements (from_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_to_warehouse_id ON stock_movements (to_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_created_at ON stock_movements (created_at);
CREATE INDEX IF NOT EXISTS idx_stock_movements_user_id ON stock_movements (user_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_ecosystem_id ON stock_movements (ecosystem_id);
CREATE INDEX IF NOT EXISTS idx_audit_entries_entity ON audit_entries (entity);
CREATE INDEX IF NOT EXISTS idx_audit_entries_entity_id ON audit_entries (entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_entries_created_at ON audit_entries (created_at);
CREATE INDEX IF NOT EXISTS idx_audit_entries_user_id ON audit_entries (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_entries_ecosystem_id ON audit_entries (ecosystem_id);
CREATE INDEX IF NOT EXISTS idx_transactions_cashier_id ON transactions (cashier_id);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions (created_at);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions (status);
CREATE INDEX IF NOT EXISTS idx_transactions_ecosystem_id ON transactions (ecosystem_id);
CREATE INDEX IF NOT EXISTS idx_workspace_settings_ecosystem_id ON workspace_settings (ecosystem_id);

DROP VIEW IF EXISTS product_stock_summary;
DROP VIEW IF EXISTS inventory_balances;

CREATE VIEW inventory_balances AS
SELECT
    sm.ecosystem_id,
    sm.product_id,
    sm.warehouse_id,
    CAST(SUM(sm.delta) AS INT) AS quantity
FROM (
    SELECT ecosystem_id, product_id, to_warehouse_id AS warehouse_id, qty AS delta
    FROM stock_movements WHERE type = 'IN'
    UNION ALL
    SELECT ecosystem_id, product_id, from_warehouse_id, -qty FROM stock_movements WHERE type = 'OUT'
    UNION ALL
    SELECT ecosystem_id, product_id, from_warehouse_id, -qty FROM stock_movements WHERE type = 'TRANSFER'
    UNION ALL
    SELECT ecosystem_id, product_id, to_warehouse_id, qty FROM stock_movements WHERE type = 'TRANSFER'
    UNION ALL
    SELECT ecosystem_id, product_id, from_warehouse_id, -qty
    FROM stock_movements WHERE type = 'ADJUSTMENT' AND from_warehouse_id IS NOT NULL
    UNION ALL
    SELECT ecosystem_id, product_id, to_warehouse_id, qty
    FROM stock_movements WHERE type = 'ADJUSTMENT' AND to_warehouse_id IS NOT NULL
) AS sm
GROUP BY sm.ecosystem_id, sm.product_id, sm.warehouse_id;

CREATE VIEW product_stock_summary AS
SELECT
    p.id AS product_id,
    p.default_warehouse_id AS warehouse_id,
    CAST(COALESCE(ib.quantity, 0) AS INT) AS quantity
FROM products p
LEFT JOIN inventory_balances ib
    ON ib.product_id = p.id
   AND ib.warehouse_id = p.default_warehouse_id
   AND ib.ecosystem_id = p.ecosystem_id;

-- Demo workspace settings (mirrors Flyway V18 seed + V21 ecosystem backfill).
INSERT INTO workspace_settings (
    id, business_name, contact_email, tax_rate, tax_label, receipt_footer, business_mode, ecosystem_id
)
SELECT
    '00000000-0000-4000-8000-000000000001',
    'ProdWatch Demo Co.',
    'ops@prodwatch.app',
    0.16,
    'VAT',
    'Thank you for your purchase!',
    'auto',
    '33333333-3333-4333-8333-333333333301'
WHERE NOT EXISTS (
    SELECT 1 FROM workspace_settings WHERE id = '00000000-0000-4000-8000-000000000001'
);
