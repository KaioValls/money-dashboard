-- =============================================================================
-- V1 — Extensões PostgreSQL e Tabelas Mestras
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Extensões
-- ---------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "unaccent";   -- busca sem acentuação

-- ---------------------------------------------------------------------------
-- companies
-- Representa o CNPJ/empresa dona das lojas e configurações tributárias.
-- ---------------------------------------------------------------------------
CREATE TABLE companies (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    cnpj_digits   VARCHAR(14)  NOT NULL,
    legal_name    VARCHAR(255) NOT NULL,
    trade_name    VARCHAR(255),
    tax_regime    VARCHAR(30)  NOT NULL,
    simples_annex VARCHAR(5),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_companies PRIMARY KEY (id),
    CONSTRAINT uq_companies_cnpj UNIQUE (cnpj_digits),
    CONSTRAINT chk_companies_tax_regime
        CHECK (tax_regime IN ('SIMPLES_NACIONAL', 'LUCRO_PRESUMIDO', 'LUCRO_REAL'))
);

-- ---------------------------------------------------------------------------
-- marketplaces
-- Cadastro dos marketplaces integrados (Mercado Livre, Shopee, Amazon…).
-- ---------------------------------------------------------------------------
CREATE TABLE marketplaces (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    name                VARCHAR(100) NOT NULL,
    slug                VARCHAR(100) NOT NULL,
    logo_url            VARCHAR(500),
    website_url         VARCHAR(500),
    payment_cycle_days  INT          NOT NULL DEFAULT 30,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_marketplaces PRIMARY KEY (id),
    CONSTRAINT uq_marketplaces_slug UNIQUE (slug)
);

-- ---------------------------------------------------------------------------
-- marketplace_accounts
-- Vínculo entre company e marketplace (conta de vendedor).
-- UNIQUE(company_id, marketplace_id) impede duplicidade de conta.
-- ---------------------------------------------------------------------------
CREATE TABLE marketplace_accounts (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    company_id       UUID        NOT NULL,
    marketplace_id   UUID        NOT NULL,
    seller_id_external VARCHAR(100),
    active           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_marketplace_accounts PRIMARY KEY (id),
    CONSTRAINT fk_marketplace_accounts_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_marketplace_accounts_marketplace
        FOREIGN KEY (marketplace_id) REFERENCES marketplaces (id),
    CONSTRAINT uq_marketplace_accounts_company_marketplace
        UNIQUE (company_id, marketplace_id)
);

-- ---------------------------------------------------------------------------
-- stores
-- Loja física dentro de uma conta de marketplace.
-- status: ACTIVE | PAUSED | CLOSED
-- ads_ratio_mode: DIRECT (valor direto) | CAMPAIGN (rateio por campanha)
-- ---------------------------------------------------------------------------
CREATE TABLE stores (
    id                     UUID         NOT NULL DEFAULT gen_random_uuid(),
    marketplace_account_id UUID         NOT NULL,
    company_id             UUID         NOT NULL,
    marketplace_id         UUID         NOT NULL,
    name                   VARCHAR(255) NOT NULL,
    status                 VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    ads_ratio_mode         VARCHAR(20)  NOT NULL DEFAULT 'DIRECT',
    active                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_stores PRIMARY KEY (id),
    CONSTRAINT fk_stores_marketplace_account
        FOREIGN KEY (marketplace_account_id) REFERENCES marketplace_accounts (id),
    CONSTRAINT fk_stores_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_stores_marketplace
        FOREIGN KEY (marketplace_id) REFERENCES marketplaces (id),
    CONSTRAINT chk_stores_status
        CHECK (status IN ('ACTIVE', 'PAUSED', 'CLOSED')),
    CONSTRAINT chk_stores_ads_ratio_mode
        CHECK (ads_ratio_mode IN ('DIRECT', 'CAMPAIGN'))
);

-- ---------------------------------------------------------------------------
-- Índices das tabelas mestras (ST-12)
-- ---------------------------------------------------------------------------
CREATE INDEX idx_marketplace_accounts_company  ON marketplace_accounts (company_id);
CREATE INDEX idx_stores_company                ON stores (company_id);
CREATE INDEX idx_stores_marketplace            ON stores (marketplace_id);
