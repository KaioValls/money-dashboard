-- =============================================================================
-- V6 — Custos Operacionais e Tabelas Complementares
-- =============================================================================

-- ---------------------------------------------------------------------------
-- recurring_cost_definitions
-- Define um custo recorrente por empresa (ex: assinatura de software mensal).
-- cost_category: SOFTWARE | LOGISTICS | MARKETING | INFRASTRUCTURE | OTHER
-- recurrence: MONTHLY | QUARTERLY | ANNUAL
-- ---------------------------------------------------------------------------
CREATE TABLE recurring_cost_definitions (
    id            UUID          NOT NULL DEFAULT gen_random_uuid(),
    company_id    UUID          NOT NULL,
    name          VARCHAR(255)  NOT NULL,
    amount        NUMERIC(12,2) NOT NULL,
    cost_category VARCHAR(30)   NOT NULL,
    recurrence    VARCHAR(20)   NOT NULL,
    start_date    DATE          NOT NULL,
    end_date      DATE,                    -- NULL = sem data de encerramento
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_recurring_cost_definitions
        PRIMARY KEY (id),
    CONSTRAINT fk_recurring_cost_def_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT chk_recurring_cost_category
        CHECK (cost_category IN ('SOFTWARE','LOGISTICS','MARKETING','INFRASTRUCTURE','OTHER')),
    CONSTRAINT chk_recurring_cost_recurrence
        CHECK (recurrence IN ('MONTHLY','QUARTERLY','ANNUAL'))
);

-- ---------------------------------------------------------------------------
-- recurring_cost_occurrences
-- Materializa cada ocorrência mensal/trimestral/anual de uma definição.
-- amount pode diferir de definition.amount (ajuste manual permitido).
-- UNIQUE(definition_id, reference_year, reference_month) impede duplicata.
-- company_id denormalizado para facilitar consultas por empresa.
-- ---------------------------------------------------------------------------
CREATE TABLE recurring_cost_occurrences (
    id               UUID          NOT NULL DEFAULT gen_random_uuid(),
    definition_id    UUID          NOT NULL,
    company_id       UUID          NOT NULL,   -- denorm para consultas por empresa
    reference_year   INT           NOT NULL,
    reference_month  INT           NOT NULL,
    amount           NUMERIC(12,2) NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    paid             BOOLEAN       NOT NULL DEFAULT FALSE,
    paid_at          TIMESTAMPTZ,              -- NULL = não pago
    skip_reason      TEXT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_recurring_cost_occurrences
        PRIMARY KEY (id),
    CONSTRAINT fk_recurring_cost_occ_definition
        FOREIGN KEY (definition_id) REFERENCES recurring_cost_definitions (id),
    CONSTRAINT fk_recurring_cost_occ_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT uq_recurring_cost_occ_period
        UNIQUE (definition_id, reference_year, reference_month),
    CONSTRAINT chk_recurring_cost_occ_status
        CHECK (status IN ('ACTIVE','SKIPPED','CANCELLED'))
);

-- ---------------------------------------------------------------------------
-- nonrecurring_costs
-- Custos avulsos (não recorrentes) por empresa.
-- date_key FK para dim_date.
-- ---------------------------------------------------------------------------
CREATE TABLE nonrecurring_costs (
    id              UUID          NOT NULL DEFAULT gen_random_uuid(),
    company_id      UUID          NOT NULL,
    description     TEXT          NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    cost_category   VARCHAR(30)   NOT NULL DEFAULT 'OTHER',
    cost_date       DATE          NOT NULL,
    date_key        INT           NOT NULL,
    iso_year        INT           NOT NULL,
    iso_week_number INT           NOT NULL,
    invoice_number  VARCHAR(100),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_nonrecurring_costs
        PRIMARY KEY (id),
    CONSTRAINT fk_nonrecurring_costs_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_nonrecurring_costs_dim_date
        FOREIGN KEY (date_key) REFERENCES dim_date (date_key),
    CONSTRAINT chk_nonrecurring_cost_category
        CHECK (cost_category IN ('SOFTWARE','LOGISTICS','MARKETING','INFRASTRUCTURE','OTHER'))
);

-- ---------------------------------------------------------------------------
-- weekly_ads_campaigns
-- Orçamento total de ADS por loja por semana.
-- UNIQUE(store_id, iso_year, iso_week_number) impede campanha duplicada.
-- ratio_method: BY_REVENUE | BY_UNITS (define como ratear entre sale_entries)
-- ---------------------------------------------------------------------------
CREATE TABLE weekly_ads_campaigns (
    id               UUID          NOT NULL DEFAULT gen_random_uuid(),
    store_id         UUID          NOT NULL,
    iso_year         INT           NOT NULL,
    iso_week_number  INT           NOT NULL,
    total_ads_budget NUMERIC(12,2) NOT NULL,
    ratio_method     VARCHAR(20)   NOT NULL DEFAULT 'BY_REVENUE',
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_weekly_ads_campaigns
        PRIMARY KEY (id),
    CONSTRAINT fk_weekly_ads_campaigns_store
        FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT uq_weekly_ads_campaigns_store_week
        UNIQUE (store_id, iso_year, iso_week_number),
    CONSTRAINT chk_weekly_ads_ratio_method
        CHECK (ratio_method IN ('BY_REVENUE','BY_UNITS'))
);

-- ---------------------------------------------------------------------------
-- monthly_revenue_snapshots
-- Snapshot mensal de faturamento e faixa Simples Nacional por empresa.
-- locked=true impede alteração (validar no use case).
-- UNIQUE(company_id, reference_year, reference_month).
-- ---------------------------------------------------------------------------
CREATE TABLE monthly_revenue_snapshots (
    id                   UUID          NOT NULL DEFAULT gen_random_uuid(),
    company_id           UUID          NOT NULL,
    reference_year       INT           NOT NULL,
    reference_month      INT           NOT NULL,
    total_revenue        NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    trailing_12m_revenue NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    simples_bracket      VARCHAR(10),              -- faixa Simples (ex: "1", "2A")
    effective_rate       NUMERIC(6,4),             -- alíquota efetiva calculada
    source               VARCHAR(20)   NOT NULL DEFAULT 'CALCULATED',
    locked               BOOLEAN       NOT NULL DEFAULT FALSE,
    locked_at            TIMESTAMPTZ,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_monthly_revenue_snapshots
        PRIMARY KEY (id),
    CONSTRAINT fk_monthly_revenue_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT uq_monthly_revenue_company_period
        UNIQUE (company_id, reference_year, reference_month),
    CONSTRAINT chk_monthly_revenue_source
        CHECK (source IN ('CALCULATED','MANUAL'))
);

-- ---------------------------------------------------------------------------
-- audit_logs
-- Registro de criação, alteração e exclusão de entidades críticas.
-- old_values / new_values: JSONB arbitrário.
-- changed_fields: array de nomes de campos alterados.
-- ---------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    company_id     UUID         NOT NULL,
    user_id        UUID,                   -- NULL = operação de sistema/job
    entity_type    VARCHAR(60)  NOT NULL,
    entity_id      UUID         NOT NULL,
    action         VARCHAR(20)  NOT NULL,
    old_values     JSONB,                  -- NULL para CREATE
    new_values     JSONB,                  -- NULL para DELETE
    changed_fields TEXT[],
    occurred_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_audit_logs
        PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT chk_audit_logs_action
        CHECK (action IN ('CREATE','UPDATE','DELETE'))
);

-- ---------------------------------------------------------------------------
-- marketplace_payout_entries
-- Registro de repasses esperados e recebidos do marketplace.
-- status: EXPECTED | RECEIVED | DISPUTED
-- ---------------------------------------------------------------------------
CREATE TABLE marketplace_payout_entries (
    id               UUID          NOT NULL DEFAULT gen_random_uuid(),
    store_id         UUID          NOT NULL,
    company_id       UUID          NOT NULL,   -- denorm
    reference_date   DATE          NOT NULL,
    date_key         INT           NOT NULL,
    expected_amount  NUMERIC(12,2) NOT NULL,
    received_amount  NUMERIC(12,2),            -- NULL até o repasse ser conciliado
    status           VARCHAR(20)   NOT NULL DEFAULT 'EXPECTED',
    payment_date     DATE,
    notes            TEXT,
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_marketplace_payout_entries
        PRIMARY KEY (id),
    CONSTRAINT fk_payout_entries_store
        FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_payout_entries_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_payout_entries_dim_date
        FOREIGN KEY (date_key) REFERENCES dim_date (date_key),
    CONSTRAINT chk_payout_entries_status
        CHECK (status IN ('EXPECTED','RECEIVED','DISPUTED'))
);

-- ---------------------------------------------------------------------------
-- Índices (ST-23)
-- ---------------------------------------------------------------------------
CREATE INDEX idx_recurring_occ_company_period
    ON recurring_cost_occurrences (company_id, reference_year, reference_month);

CREATE INDEX idx_nonrecurring_company_period
    ON nonrecurring_costs (company_id, iso_year, iso_week_number);

CREATE INDEX idx_ads_campaigns_store_week
    ON weekly_ads_campaigns (store_id, iso_year, iso_week_number);

CREATE INDEX idx_revenue_snapshots_company
    ON monthly_revenue_snapshots (company_id, reference_year, reference_month);

CREATE INDEX idx_audit_logs_entity
    ON audit_logs (entity_type, entity_id);

CREATE INDEX idx_audit_logs_occurred_at
    ON audit_logs (occurred_at DESC);

CREATE INDEX idx_payout_store_date
    ON marketplace_payout_entries (store_id, reference_date);
