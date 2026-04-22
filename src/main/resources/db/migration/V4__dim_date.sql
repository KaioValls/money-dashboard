-- =============================================================================
-- V4 — Dimensão de Data (dim_date)
-- Populada via generate_series de 2020-01-01 a 2040-12-31 (~7.671 linhas)
-- =============================================================================

CREATE TABLE dim_date (
    date_key        INT          NOT NULL,
    full_date       DATE         NOT NULL,
    day_of_week     INT          NOT NULL,   -- 1=Segunda … 7=Domingo (ISO)
    day_name        VARCHAR(15)  NOT NULL,   -- em português
    day_of_month    INT          NOT NULL,
    day_of_year     INT          NOT NULL,
    week_of_month   INT          NOT NULL,   -- semana dentro do mês (1-5)
    iso_week_number INT          NOT NULL,   -- semana ISO do ano
    iso_year        INT          NOT NULL,   -- ano ISO (pode diferir do calendário em jan/dez)
    month           INT          NOT NULL,
    year            INT          NOT NULL,
    month_name      VARCHAR(20)  NOT NULL,   -- em português
    quarter         INT          NOT NULL,
    is_weekend      BOOLEAN      NOT NULL,
    week_label      VARCHAR(30)  NOT NULL,   -- "Sem 3 / Mar 2025"

    CONSTRAINT pk_dim_date PRIMARY KEY (date_key),
    CONSTRAINT uq_dim_date_full_date UNIQUE (full_date)
);

-- ---------------------------------------------------------------------------
-- Popula dim_date com generate_series 2020-01-01 → 2040-12-31
-- week_of_month: semanas delimitadas pelo dia-da-semana do dia 1 do mês
--   = floor((day_of_month - 1) / 7) + 1
-- ---------------------------------------------------------------------------
INSERT INTO dim_date (
    date_key, full_date, day_of_week, day_name,
    day_of_month, day_of_year, week_of_month,
    iso_week_number, iso_year,
    month, year, month_name, quarter,
    is_weekend, week_label
)
WITH series AS (
    SELECT generate_series(
        '2020-01-01'::date,
        '2040-12-31'::date,
        '1 day'::interval
    )::date AS d
),
base AS (
    SELECT
        d,
        TO_CHAR(d, 'YYYYMMDD')::INT            AS date_key,
        EXTRACT(ISODOW  FROM d)::INT            AS day_of_week,
        EXTRACT(DAY     FROM d)::INT            AS day_of_month,
        EXTRACT(DOY     FROM d)::INT            AS day_of_year,
        EXTRACT(WEEK    FROM d)::INT            AS iso_week_number,
        EXTRACT(ISOYEAR FROM d)::INT            AS iso_year,
        EXTRACT(MONTH   FROM d)::INT            AS month,
        EXTRACT(YEAR    FROM d)::INT            AS year,
        CEIL(EXTRACT(MONTH FROM d) / 3.0)::INT  AS quarter,
        EXTRACT(ISODOW  FROM d) >= 6            AS is_weekend,
        ((EXTRACT(DAY   FROM d)::INT - 1) / 7 + 1) AS week_of_month
    FROM series
)
SELECT
    date_key,
    d                                           AS full_date,
    day_of_week,
    CASE day_of_week
        WHEN 1 THEN 'Segunda-feira'
        WHEN 2 THEN 'Terça-feira'
        WHEN 3 THEN 'Quarta-feira'
        WHEN 4 THEN 'Quinta-feira'
        WHEN 5 THEN 'Sexta-feira'
        WHEN 6 THEN 'Sábado'
        WHEN 7 THEN 'Domingo'
    END                                         AS day_name,
    day_of_month,
    day_of_year,
    week_of_month,
    iso_week_number,
    iso_year,
    month,
    year,
    CASE month
        WHEN  1 THEN 'Janeiro'
        WHEN  2 THEN 'Fevereiro'
        WHEN  3 THEN 'Março'
        WHEN  4 THEN 'Abril'
        WHEN  5 THEN 'Maio'
        WHEN  6 THEN 'Junho'
        WHEN  7 THEN 'Julho'
        WHEN  8 THEN 'Agosto'
        WHEN  9 THEN 'Setembro'
        WHEN 10 THEN 'Outubro'
        WHEN 11 THEN 'Novembro'
        WHEN 12 THEN 'Dezembro'
    END                                         AS month_name,
    quarter,
    is_weekend,
    'Sem ' || iso_week_number || ' / ' ||
    CASE month
        WHEN  1 THEN 'Jan'
        WHEN  2 THEN 'Fev'
        WHEN  3 THEN 'Mar'
        WHEN  4 THEN 'Abr'
        WHEN  5 THEN 'Mai'
        WHEN  6 THEN 'Jun'
        WHEN  7 THEN 'Jul'
        WHEN  8 THEN 'Ago'
        WHEN  9 THEN 'Set'
        WHEN 10 THEN 'Out'
        WHEN 11 THEN 'Nov'
        WHEN 12 THEN 'Dez'
    END || ' ' || year                          AS week_label
FROM base;

-- ---------------------------------------------------------------------------
-- Índices (ST-17)
-- ---------------------------------------------------------------------------
CREATE INDEX idx_dim_date_iso_week
    ON dim_date (iso_year, iso_week_number);

CREATE INDEX idx_dim_date_month_year
    ON dim_date (year, month);
