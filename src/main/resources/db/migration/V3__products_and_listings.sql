-- =============================================================================
-- V3 — Produtos, Anúncios e Histórico de Preço
-- =============================================================================

-- ---------------------------------------------------------------------------
-- products
-- Catálogo de produtos por empresa.
-- UNIQUE(company_id, sku) garante unicidade de SKU dentro de cada empresa.
-- ---------------------------------------------------------------------------
CREATE TABLE products (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    company_id    UUID         NOT NULL,
    sku           VARCHAR(100) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    category      VARCHAR(100),
    ean_gtin      VARCHAR(20),
    weight_grams  INT,
    in_stock      BOOLEAN      NOT NULL DEFAULT TRUE,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_products
        PRIMARY KEY (id),
    CONSTRAINT fk_products_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT uq_products_company_sku
        UNIQUE (company_id, sku)
);

-- ---------------------------------------------------------------------------
-- listings
-- Anúncio de um produto em uma loja de marketplace.
-- marketplace_id e company_id são denormalizados para facilitar consultas.
-- status: ACTIVE | PAUSED | CLOSED
-- ---------------------------------------------------------------------------
CREATE TABLE listings (
    id                   UUID          NOT NULL DEFAULT gen_random_uuid(),
    product_id           UUID          NOT NULL,
    store_id             UUID          NOT NULL,
    marketplace_id       UUID          NOT NULL,   -- denorm de stores.marketplace_id
    company_id           UUID          NOT NULL,   -- denorm de stores.company_id
    external_listing_id  VARCHAR(100),
    title                VARCHAR(255)  NOT NULL,
    sale_price           NUMERIC(12,2) NOT NULL,
    listing_type         VARCHAR(30),
    status               VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    published_at         TIMESTAMPTZ,
    closed_at            TIMESTAMPTZ,
    active               BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_listings
        PRIMARY KEY (id),
    CONSTRAINT fk_listings_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_listings_store
        FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_listings_marketplace
        FOREIGN KEY (marketplace_id) REFERENCES marketplaces (id),
    CONSTRAINT fk_listings_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT chk_listings_status
        CHECK (status IN ('ACTIVE', 'PAUSED', 'CLOSED'))
);

-- ---------------------------------------------------------------------------
-- listing_price_history
-- Histórico de variações de preço de um anúncio.
-- valid_until NULL → preço atualmente vigente
-- ---------------------------------------------------------------------------
CREATE TABLE listing_price_history (
    id             UUID          NOT NULL DEFAULT gen_random_uuid(),
    listing_id     UUID          NOT NULL,
    sale_price     NUMERIC(12,2) NOT NULL,
    valid_from     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    valid_until    TIMESTAMPTZ,              -- NULL = preço atualmente vigente
    changed_by     UUID,
    change_reason  TEXT,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_listing_price_history
        PRIMARY KEY (id),
    CONSTRAINT fk_listing_price_history_listing
        FOREIGN KEY (listing_id) REFERENCES listings (id)
);

-- ---------------------------------------------------------------------------
-- Índices (ST-15)
-- idx_products_company: listagem de produtos por empresa
-- idx_products_sku: busca de produto por empresa + SKU
-- idx_listings_store: anúncios por loja
-- idx_listings_product: anúncios por produto
-- ---------------------------------------------------------------------------
CREATE INDEX idx_products_company
    ON products (company_id);

CREATE INDEX idx_products_sku
    ON products (company_id, sku);

CREATE INDEX idx_listings_store
    ON listings (store_id);

CREATE INDEX idx_listings_product
    ON listings (product_id);
