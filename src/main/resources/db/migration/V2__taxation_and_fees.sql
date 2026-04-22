-- =============================================================================
-- V2 — Tributação e Taxas de Marketplace
-- =============================================================================

-- ---------------------------------------------------------------------------
-- tax_configs
-- Configuração tributária vigente por empresa.
-- charge_moment: ON_PURCHASE | ON_SALE | SEPARATE
-- calculation_base: GROSS_REVENUE | NET_REVENUE | PROFIT
-- valid_until NULL → vigência em aberto (config ainda ativa)
-- ---------------------------------------------------------------------------
CREATE TABLE tax_configs (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    company_id       UUID         NOT NULL,
    charge_moment    VARCHAR(20)  NOT NULL,
    calculation_base VARCHAR(20)  NOT NULL,
    rate_percentage  NUMERIC(6,4) NOT NULL,
    valid_from       DATE         NOT NULL,
    valid_until      DATE,                   -- NULL = vigência em aberto
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_tax_configs
        PRIMARY KEY (id),
    CONSTRAINT fk_tax_configs_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT chk_tax_configs_charge_moment
        CHECK (charge_moment IN ('ON_PURCHASE', 'ON_SALE', 'SEPARATE')),
    CONSTRAINT chk_tax_configs_calculation_base
        CHECK (calculation_base IN ('GROSS_REVENUE', 'NET_REVENUE', 'PROFIT'))
);

-- ---------------------------------------------------------------------------
-- marketplace_fee_rules
-- Tabela de taxas do marketplace por faixa de preço.
-- max_price NULL → sem limite de faixa superior (cobre qualquer preço >= min_price)
-- valid_until NULL → vigência em aberto
-- ---------------------------------------------------------------------------
CREATE TABLE marketplace_fee_rules (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    marketplace_id UUID         NOT NULL,
    min_price      NUMERIC(12,2) NOT NULL,
    max_price      NUMERIC(12,2),            -- NULL = sem limite superior
    fee_percentage NUMERIC(6,4) NOT NULL,
    fixed_fee      NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    valid_from     DATE         NOT NULL,
    valid_until    DATE,                      -- NULL = vigência em aberto
    priority       INT          NOT NULL DEFAULT 0,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_marketplace_fee_rules
        PRIMARY KEY (id),
    CONSTRAINT fk_marketplace_fee_rules_marketplace
        FOREIGN KEY (marketplace_id) REFERENCES marketplaces (id)
);

-- ---------------------------------------------------------------------------
-- Índices (ST-13)
-- idx_tax_configs_company: consulta de config ativa por empresa + período
-- idx_fee_rules_marketplace: busca de regra aplicável por marketplace + preço
-- ---------------------------------------------------------------------------
CREATE INDEX idx_tax_configs_company
    ON tax_configs (company_id, valid_from, valid_until);

CREATE INDEX idx_fee_rules_marketplace
    ON marketplace_fee_rules (marketplace_id, min_price);
