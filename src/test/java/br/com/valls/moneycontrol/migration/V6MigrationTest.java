package br.com.valls.moneycontrol.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Valida a migration V6 (custos operacionais e tabelas complementares) contra PostgreSQL 16 real.
 * Salta automaticamente se Docker não estiver disponível.
 */
@Testcontainers
@EnabledIfDockerAvailable
class V6MigrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    private static Connection conn;

    @BeforeAll
    static void runMigrationsAndConnect() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    @AfterAll
    static void closeConnection() throws Exception {
        if (conn != null && !conn.isClosed()) conn.close();
    }

    // ── critério: V6 migra sem erros após V1-V5 ──────────────────────────────
    @Test
    void v6_migrates_after_v1_to_v5_without_errors() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true");
        rs.next();
        assertTrue(rs.getInt(1) >= 6, "Devem existir ao menos 6 migrations executadas");
    }

    // ── critério: UNIQUE(definition_id, reference_year, reference_month) ─────
    @Test
    void unique_constraint_rejects_duplicate_recurring_occurrence() throws Exception {
        UUID companyId = insertCompany("11223300000101", "Empresa Recorrente", "SIMPLES_NACIONAL");
        UUID defId     = insertRecurringCostDef(companyId, "Assinatura Software", "SOFTWARE", "MONTHLY");

        insertRecurringOccurrence(defId, companyId, 2025, 4);  // 1ª — ok

        var ex = assertThrows(SQLException.class,
                () -> insertRecurringOccurrence(defId, companyId, 2025, 4));  // 2ª — deve falhar
        assertTrue(ex.getSQLState().startsWith("23"),
                "Esperado SQLState 23xxx (unique violation), foi: " + ex.getSQLState());
    }

    // ── critério: amount em occurrence pode diferir da definition ─────────────
    @Test
    void occurrence_amount_can_differ_from_definition() throws Exception {
        UUID companyId = insertCompany("22334400000102", "Empresa Valor Ajustado", "LUCRO_REAL");
        UUID defId     = insertRecurringCostDef(companyId, "Logística", "LOGISTICS", "MONTHLY");

        UUID occId = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO recurring_cost_occurrences
                    (id, definition_id, company_id, reference_year, reference_month, amount)
                VALUES (?, ?, ?, 2025, 3, 999.99)
                """)) {
            ps.setObject(1, occId);
            ps.setObject(2, defId);
            ps.setObject(3, companyId);
            ps.execute();
        }
        var rs = conn.createStatement().executeQuery(
                "SELECT amount FROM recurring_cost_occurrences WHERE id = '" + occId + "'");
        assertTrue(rs.next());
        assertEquals(0, new java.math.BigDecimal("999.99").compareTo(rs.getBigDecimal("amount")),
                "amount da occurrence deve poder diferir da definition");
    }

    // ── critério: CHECK status em recurring_cost_occurrences ─────────────────
    @Test
    void check_constraint_rejects_invalid_occurrence_status() throws Exception {
        UUID companyId = insertCompany("33445500000103", "Empresa Status Occ", "LUCRO_PRESUMIDO");
        UUID defId     = insertRecurringCostDef(companyId, "Marketing", "MARKETING", "MONTHLY");

        var ex = assertThrows(SQLException.class, () -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO recurring_cost_occurrences
                        (definition_id, company_id, reference_year, reference_month, amount, status)
                    VALUES (?, ?, 2025, 5, 100.00, 'STATUS_INVALIDO')
                    """)) {
                ps.setObject(1, defId);
                ps.setObject(2, companyId);
                ps.execute();
            }
        });
        assertTrue(ex.getSQLState().startsWith("23"),
                "Esperado SQLState 23xxx (check violation), foi: " + ex.getSQLState());
    }

    // ── critério: weekly_ads_campaigns UNIQUE(store_id, iso_year, iso_week_number)
    @Test
    void weekly_ads_unique_per_store_and_week() throws Exception {
        UUID companyId    = insertCompany("44556600000104", "Empresa ADS", "SIMPLES_NACIONAL");
        UUID marketId     = insertMarketplace("ADS Market", "ads-market");
        UUID accountId    = insertMarketplaceAccount(companyId, marketId);
        UUID storeId      = insertStore(accountId, companyId, marketId, "Loja ADS");

        insertAdsCampaign(storeId, 2025, 10, "100.00");  // 1ª — ok

        var ex = assertThrows(SQLException.class,
                () -> insertAdsCampaign(storeId, 2025, 10, "200.00"));  // 2ª — deve falhar
        assertTrue(ex.getSQLState().startsWith("23"),
                "Esperado SQLState 23xxx (unique violation), foi: " + ex.getSQLState());
    }

    // ── critério: CHECK ratio_method em weekly_ads_campaigns ─────────────────
    @Test
    void check_constraint_rejects_invalid_ratio_method() throws Exception {
        UUID companyId = insertCompany("55667700000105", "Empresa Ratio", "LUCRO_REAL");
        UUID marketId  = insertMarketplace("Ratio Market", "ratio-market");
        UUID accountId = insertMarketplaceAccount(companyId, marketId);
        UUID storeId   = insertStore(accountId, companyId, marketId, "Loja Ratio");

        var ex = assertThrows(SQLException.class, () -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO weekly_ads_campaigns
                        (store_id, iso_year, iso_week_number, total_ads_budget, ratio_method)
                    VALUES (?, 2025, 20, 500.00, 'METODO_INVALIDO')
                    """)) {
                ps.setObject(1, storeId);
                ps.execute();
            }
        });
        assertTrue(ex.getSQLState().startsWith("23"),
                "Esperado SQLState 23xxx (check violation), foi: " + ex.getSQLState());
    }

    // ── critério: monthly_revenue_snapshots UNIQUE(company_id, year, month) ──
    @Test
    void monthly_revenue_unique_per_company_and_period() throws Exception {
        UUID companyId = insertCompany("66778800000106", "Empresa Revenue", "SIMPLES_NACIONAL");

        insertRevenueSnapshot(companyId, 2025, 1);  // 1ª — ok

        var ex = assertThrows(SQLException.class,
                () -> insertRevenueSnapshot(companyId, 2025, 1));  // 2ª — deve falhar
        assertTrue(ex.getSQLState().startsWith("23"),
                "Esperado SQLState 23xxx (unique violation), foi: " + ex.getSQLState());
    }

    // ── critério: CHECK source em monthly_revenue_snapshots ──────────────────
    @Test
    void check_constraint_rejects_invalid_revenue_source() throws Exception {
        UUID companyId = insertCompany("77889900000107", "Empresa Source", "LUCRO_PRESUMIDO");

        var ex = assertThrows(SQLException.class, () -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO monthly_revenue_snapshots
                        (company_id, reference_year, reference_month, source)
                    VALUES (?, 2025, 6, 'FONTE_INVALIDA')
                    """)) {
                ps.setObject(1, companyId);
                ps.execute();
            }
        });
        assertTrue(ex.getSQLState().startsWith("23"),
                "Esperado SQLState 23xxx (check violation), foi: " + ex.getSQLState());
    }

    // ── critério: audit_logs aceita JSONB arbitrário ──────────────────────────
    @Test
    void audit_logs_accepts_arbitrary_jsonb() throws Exception {
        UUID companyId = insertCompany("88990000000108", "Empresa Audit", "SIMPLES_NACIONAL");
        UUID entityId  = UUID.randomUUID();

        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO audit_logs
                    (company_id, entity_type, entity_id, action, old_values, new_values, changed_fields)
                VALUES (?, 'SaleEntry', ?, 'UPDATE',
                    '{"gross_revenue": 1000.00, "net_profit": 500.00}'::jsonb,
                    '{"gross_revenue": 1200.00, "net_profit": 650.00}'::jsonb,
                    ARRAY['gross_revenue','net_profit'])
                """)) {
            ps.setObject(1, companyId);
            ps.setObject(2, entityId);
            ps.execute();
        }

        var rs = conn.createStatement().executeQuery(
                "SELECT old_values->>'gross_revenue' AS old_rev, " +
                "       new_values->>'gross_revenue' AS new_rev, " +
                "       array_length(changed_fields, 1) AS num_fields " +
                "FROM audit_logs WHERE entity_id = '" + entityId + "'");
        assertTrue(rs.next());
        assertEquals("1000.00", rs.getString("old_rev"));
        assertEquals("1200.00", rs.getString("new_rev"));
        assertEquals(2, rs.getInt("num_fields"), "changed_fields deve ter 2 entradas");
    }

    // ── critério: audit_logs aceita old_values NULL (CREATE) ─────────────────
    @Test
    void audit_logs_accepts_null_old_values_for_create() throws Exception {
        UUID companyId = insertCompany("99001100000109", "Empresa Create Audit", "LUCRO_REAL");
        UUID entityId  = UUID.randomUUID();

        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO audit_logs
                    (company_id, entity_type, entity_id, action, new_values)
                VALUES (?, 'Company', ?, 'CREATE', '{"legal_name": "Empresa Nova"}'::jsonb)
                """)) {
            ps.setObject(1, companyId);
            ps.setObject(2, entityId);
            ps.execute();
        }

        var rs = conn.createStatement().executeQuery(
                "SELECT old_values FROM audit_logs WHERE entity_id = '" + entityId + "'");
        assertTrue(rs.next());
        assertTrue(rs.getObject("old_values") == null,
                "old_values deve ser NULL para operação CREATE");
    }

    // ── critério: CHECK action em audit_logs rejeita valor inválido ──────────
    @Test
    void check_constraint_rejects_invalid_audit_action() throws Exception {
        UUID companyId = insertCompany("10011100000110", "Empresa Action", "SIMPLES_NACIONAL");

        var ex = assertThrows(SQLException.class, () -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO audit_logs (company_id, entity_type, entity_id, action)
                    VALUES (?, 'Company', gen_random_uuid(), 'ACAO_INVALIDA')
                    """)) {
                ps.setObject(1, companyId);
                ps.execute();
            }
        });
        assertTrue(ex.getSQLState().startsWith("23"),
                "Esperado SQLState 23xxx (check violation), foi: " + ex.getSQLState());
    }

    // ── critério: CHECK status em marketplace_payout_entries ─────────────────
    @Test
    void check_constraint_rejects_invalid_payout_status() throws Exception {
        UUID companyId = insertCompany("20022200000111", "Empresa Payout", "LUCRO_PRESUMIDO");
        UUID marketId  = insertMarketplace("Payout Market", "payout-market");
        UUID accountId = insertMarketplaceAccount(companyId, marketId);
        UUID storeId   = insertStore(accountId, companyId, marketId, "Loja Payout");

        var ex = assertThrows(SQLException.class, () -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO marketplace_payout_entries
                        (store_id, company_id, reference_date, date_key, expected_amount, status)
                    VALUES (?, ?, '2025-01-06', 20250106, 500.00, 'STATUS_INVALIDO')
                    """)) {
                ps.setObject(1, storeId);
                ps.setObject(2, companyId);
                ps.execute();
            }
        });
        assertTrue(ex.getSQLState().startsWith("23"),
                "Esperado SQLState 23xxx (check violation), foi: " + ex.getSQLState());
    }

    // ── critério: índice idx_audit_logs_occurred_at (DESC) existe ────────────
    @Test
    void index_audit_logs_occurred_at_exists() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT indexname FROM pg_indexes WHERE indexname = 'idx_audit_logs_occurred_at'");
        assertTrue(rs.next(), "Índice idx_audit_logs_occurred_at deve existir");
    }

    // ── critério: todos os índices do ST-23 existem ───────────────────────────
    @Test
    void all_st23_indexes_exist() throws Exception {
        for (String idx : new String[]{
                "idx_recurring_occ_company_period",
                "idx_nonrecurring_company_period",
                "idx_ads_campaigns_store_week",
                "idx_revenue_snapshots_company",
                "idx_audit_logs_entity",
                "idx_audit_logs_occurred_at",
                "idx_payout_store_date"}) {
            var rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM pg_indexes WHERE indexname = '" + idx + "'");
            rs.next();
            assertEquals(1, rs.getInt(1), "Índice '" + idx + "' deve existir");
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────
    private static UUID insertCompany(String cnpj, String name, String taxRegime) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO companies (id, cnpj_digits, legal_name, tax_regime) VALUES (?, ?, ?, ?)")) {
            ps.setObject(1, id); ps.setString(2, cnpj);
            ps.setString(3, name); ps.setString(4, taxRegime);
            ps.execute();
        }
        return id;
    }

    private static UUID insertMarketplace(String name, String slug) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO marketplaces (id, name, slug) VALUES (?, ?, ?)")) {
            ps.setObject(1, id); ps.setString(2, name); ps.setString(3, slug);
            ps.execute();
        }
        return id;
    }

    private static UUID insertMarketplaceAccount(UUID companyId, UUID marketplaceId) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO marketplace_accounts (id, company_id, marketplace_id) VALUES (?, ?, ?)")) {
            ps.setObject(1, id); ps.setObject(2, companyId); ps.setObject(3, marketplaceId);
            ps.execute();
        }
        return id;
    }

    private static UUID insertStore(UUID accountId, UUID companyId, UUID marketplaceId, String name)
            throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO stores (id, marketplace_account_id, company_id, marketplace_id, name) VALUES (?, ?, ?, ?, ?)")) {
            ps.setObject(1, id); ps.setObject(2, accountId);
            ps.setObject(3, companyId); ps.setObject(4, marketplaceId); ps.setString(5, name);
            ps.execute();
        }
        return id;
    }

    private static UUID insertRecurringCostDef(UUID companyId, String name,
                                                String category, String recurrence) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO recurring_cost_definitions
                    (id, company_id, name, amount, cost_category, recurrence, start_date)
                VALUES (?, ?, ?, 100.00, ?, ?, '2025-01-01')
                """)) {
            ps.setObject(1, id); ps.setObject(2, companyId);
            ps.setString(3, name); ps.setString(4, category); ps.setString(5, recurrence);
            ps.execute();
        }
        return id;
    }

    private static void insertRecurringOccurrence(UUID defId, UUID companyId,
                                                   int year, int month) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO recurring_cost_occurrences
                    (definition_id, company_id, reference_year, reference_month, amount)
                VALUES (?, ?, ?, ?, 100.00)
                """)) {
            ps.setObject(1, defId); ps.setObject(2, companyId);
            ps.setInt(3, year); ps.setInt(4, month);
            ps.execute();
        }
    }

    private static void insertAdsCampaign(UUID storeId, int isoYear,
                                           int isoWeek, String budget) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO weekly_ads_campaigns
                    (store_id, iso_year, iso_week_number, total_ads_budget)
                VALUES (?, ?, ?, ?)
                """)) {
            ps.setObject(1, storeId); ps.setInt(2, isoYear);
            ps.setInt(3, isoWeek); ps.setBigDecimal(4, new java.math.BigDecimal(budget));
            ps.execute();
        }
    }

    private static void insertRevenueSnapshot(UUID companyId, int year, int month) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO monthly_revenue_snapshots
                    (company_id, reference_year, reference_month)
                VALUES (?, ?, ?)
                """)) {
            ps.setObject(1, companyId); ps.setInt(2, year); ps.setInt(3, month);
            ps.execute();
        }
    }
}
