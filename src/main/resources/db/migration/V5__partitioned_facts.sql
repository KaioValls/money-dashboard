-- =============================================================================
-- V5 — Tabelas Fato Particionadas: sale_entries e product_purchase_entries
-- Particionamento: PARTITION BY LIST (iso_year)
-- PK composta (id, iso_year) — obrigatória para tabelas particionadas
-- =============================================================================

-- ---------------------------------------------------------------------------
-- sale_entries — tabela pai
-- Captura snapshot de preço, custo, tributação e taxa no momento do lançamento.
-- Armazena todos os campos calculados (gross_profit, net_profit, roi...).
-- FK para dim_date via date_key.
-- ---------------------------------------------------------------------------
CREATE TABLE sale_entries (
    id                         UUID          NOT NULL DEFAULT gen_random_uuid(),
    company_id                 UUID          NOT NULL,
    store_id                   UUID          NOT NULL,
    listing_id                 UUID          NOT NULL,
    marketplace_id             UUID          NOT NULL,   -- denorm
    tax_config_id              UUID,                     -- snapshot FK (nullable: config pode ter expirado)
    fee_rule_id                UUID,                     -- snapshot FK (nullable)
    date_key                   INT           NOT NULL,   -- FK para dim_date
    iso_year                   INT           NOT NULL,   -- partition key
    iso_week_number            INT           NOT NULL,
    units_sold                 INT           NOT NULL,
    -- snapshots imutáveis capturados no momento do lançamento
    sale_price_snapshot        NUMERIC(12,2) NOT NULL,
    unit_cost_snapshot         NUMERIC(12,4) NOT NULL,
    tax_rate_snapshot          NUMERIC(6,4)  NOT NULL,
    tax_charge_moment_snapshot VARCHAR(20)   NOT NULL,
    fee_percentage_snapshot    NUMERIC(6,4)  NOT NULL,
    fixed_fee_snapshot         NUMERIC(12,2) NOT NULL,
    -- custos operacionais (atualizáveis)
    ads_cost                   NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    coupon_discount            NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    freight_cost               NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    return_amount              NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    other_costs                NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    other_costs_description    TEXT,
    -- campos calculados (recalculados pelo use case)
    gross_revenue              NUMERIC(12,2) NOT NULL,
    total_cogs                 NUMERIC(12,2) NOT NULL,
    marketplace_fee_total      NUMERIC(12,2) NOT NULL,
    tax_amount                 NUMERIC(12,2) NOT NULL,
    gross_profit               NUMERIC(12,2) NOT NULL,
    net_profit                 NUMERIC(12,2) NOT NULL,
    net_margin_percentage      NUMERIC(8,4)  NOT NULL,
    roi_percentage             NUMERIC(8,4)  NOT NULL,
    created_at                 TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_sale_entries
        PRIMARY KEY (id, iso_year),
    CONSTRAINT fk_sale_entries_company
        FOREIGN KEY (company_id)     REFERENCES companies (id),
    CONSTRAINT fk_sale_entries_store
        FOREIGN KEY (store_id)       REFERENCES stores (id),
    CONSTRAINT fk_sale_entries_listing
        FOREIGN KEY (listing_id)     REFERENCES listings (id),
    CONSTRAINT fk_sale_entries_marketplace
        FOREIGN KEY (marketplace_id) REFERENCES marketplaces (id),
    CONSTRAINT fk_sale_entries_tax_config
        FOREIGN KEY (tax_config_id)  REFERENCES tax_configs (id),
    CONSTRAINT fk_sale_entries_fee_rule
        FOREIGN KEY (fee_rule_id)    REFERENCES marketplace_fee_rules (id),
    CONSTRAINT fk_sale_entries_dim_date
        FOREIGN KEY (date_key)       REFERENCES dim_date (date_key)
) PARTITION BY LIST (iso_year);

-- ---------------------------------------------------------------------------
-- Partições de sale_entries por ano
-- ---------------------------------------------------------------------------
CREATE TABLE sale_entries_2024 PARTITION OF sale_entries FOR VALUES IN (2024);
CREATE TABLE sale_entries_2025 PARTITION OF sale_entries FOR VALUES IN (2025);
CREATE TABLE sale_entries_2026 PARTITION OF sale_entries FOR VALUES IN (2026);

-- ---------------------------------------------------------------------------
-- Índices por partição de sale_entries (ST-20)
-- ---------------------------------------------------------------------------
CREATE INDEX idx_sale_entries_2024_store_week
    ON sale_entries_2024 (store_id, iso_year, iso_week_number);
CREATE INDEX idx_sale_entries_2024_listing
    ON sale_entries_2024 (listing_id);
CREATE INDEX idx_sale_entries_2024_company
    ON sale_entries_2024 (company_id, iso_year, iso_week_number);

CREATE INDEX idx_sale_entries_2025_store_week
    ON sale_entries_2025 (store_id, iso_year, iso_week_number);
CREATE INDEX idx_sale_entries_2025_listing
    ON sale_entries_2025 (listing_id);
CREATE INDEX idx_sale_entries_2025_company
    ON sale_entries_2025 (company_id, iso_year, iso_week_number);

CREATE INDEX idx_sale_entries_2026_store_week
    ON sale_entries_2026 (store_id, iso_year, iso_week_number);
CREATE INDEX idx_sale_entries_2026_listing
    ON sale_entries_2026 (listing_id);
CREATE INDEX idx_sale_entries_2026_company
    ON sale_entries_2026 (company_id, iso_year, iso_week_number);

-- ---------------------------------------------------------------------------
-- product_purchase_entries — tabela pai
-- Registra compras de produtos (CMV). Armazena custo unitário e frete.
-- ---------------------------------------------------------------------------
CREATE TABLE product_purchase_entries (
    id                  UUID          NOT NULL DEFAULT gen_random_uuid(),
    company_id          UUID          NOT NULL,
    product_id          UUID          NOT NULL,
    iso_year            INT           NOT NULL,   -- partition key
    iso_week_number     INT           NOT NULL,
    quantity            INT           NOT NULL,
    unit_cost           NUMERIC(12,4) NOT NULL,
    total_cost          NUMERIC(12,2) NOT NULL,
    has_freight         BOOLEAN       NOT NULL DEFAULT FALSE,
    freight_cost        NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    freight_per_unit    NUMERIC(12,4) NOT NULL DEFAULT 0.0000,
    total_landed_cost   NUMERIC(12,2) NOT NULL,
    supplier_name       VARCHAR(255),             -- snapshot (não FK)
    invoice_number      VARCHAR(100),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_product_purchase_entries
        PRIMARY KEY (id, iso_year),
    CONSTRAINT fk_purchase_entries_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_purchase_entries_product
        FOREIGN KEY (product_id) REFERENCES products (id)
) PARTITION BY LIST (iso_year);

-- ---------------------------------------------------------------------------
-- Partições de product_purchase_entries por ano
-- ---------------------------------------------------------------------------
CREATE TABLE product_purchase_entries_2024
    PARTITION OF product_purchase_entries FOR VALUES IN (2024);
CREATE TABLE product_purchase_entries_2025
    PARTITION OF product_purchase_entries FOR VALUES IN (2025);
CREATE TABLE product_purchase_entries_2026
    PARTITION OF product_purchase_entries FOR VALUES IN (2026);

-- ---------------------------------------------------------------------------
-- Índices por partição de product_purchase_entries (ST-20)
-- ---------------------------------------------------------------------------
CREATE INDEX idx_purchase_entries_2024_company
    ON product_purchase_entries_2024 (company_id, iso_year, iso_week_number);
CREATE INDEX idx_purchase_entries_2024_product
    ON product_purchase_entries_2024 (product_id);

CREATE INDEX idx_purchase_entries_2025_company
    ON product_purchase_entries_2025 (company_id, iso_year, iso_week_number);
CREATE INDEX idx_purchase_entries_2025_product
    ON product_purchase_entries_2025 (product_id);

CREATE INDEX idx_purchase_entries_2026_company
    ON product_purchase_entries_2026 (company_id, iso_year, iso_week_number);
CREATE INDEX idx_purchase_entries_2026_product
    ON product_purchase_entries_2026 (product_id);
