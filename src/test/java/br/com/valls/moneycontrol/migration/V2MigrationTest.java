package br.com.valls.moneycontrol.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Valida a migration V2 (tributação e taxas) contra PostgreSQL 16 real.
 * Salta automaticamente se Docker não estiver disponível.
 */
@Testcontainers
@EnabledIfDockerAvailable
class V2MigrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    private static Connection conn;

    @BeforeAll
    static void runMigrationsAndConnect() throws Exception {
        // V1 + V2 devem migrar sem erros
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

    // ── critério: V2 migra sem erros após V1 ─────────────────────────────────
    @Test
    void v2_migrates_after_v1_without_errors() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true");
        rs.next();
        assertTrue(rs.getInt(1) >= 2, "Devem existir ao menos 2 migrations executadas (V1 e V2)");
    }

    // ── critério: tax_configs aceita valid_until NULL (vigência em aberto) ───
    @Test
    void tax_configs_accepts_null_valid_until() throws Exception {
        UUID companyId = insertCompany("10203040000150", "Empresa Config", "SIMPLES_NACIONAL");

        UUID configId = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO tax_configs
                    (id, company_id, charge_moment, calculation_base, rate_percentage, valid_from, valid_until)
                VALUES (?, ?, 'ON_SALE', 'GROSS_REVENUE', 0.0600, ?, NULL)
                """)) {
            ps.setObject(1, configId);
            ps.setObject(2, companyId);
            ps.setDate(3, Date.valueOf(LocalDate.of(2025, 1, 1)));
            ps.execute();
        }

        var rs = conn.createStatement().executeQuery(
                "SELECT valid_until FROM tax_configs WHERE id = '" + configId + "'");
        assertTrue(rs.next());
        assertNull(rs.getDate("valid_until"), "valid_until deve ser NULL para vigência em aberto");
    }

    // ── critério: marketplace_fee_rules aceita max_price NULL ────────────────
    @Test
    void fee_rules_accepts_null_max_price() throws Exception {
        UUID marketplaceId = insertMarketplace("Shopee V2", "shopee-v2");

        UUID ruleId = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO marketplace_fee_rules
                    (id, marketplace_id, min_price, max_price, fee_percentage, fixed_fee, valid_from)
                VALUES (?, ?, 0.00, NULL, 0.1600, 0.00, ?)
                """)) {
            ps.setObject(1, ruleId);
            ps.setObject(2, marketplaceId);
            ps.setDate(3, Date.valueOf(LocalDate.of(2025, 1, 1)));
            ps.execute();
        }

        var rs = conn.createStatement().executeQuery(
                "SELECT max_price FROM marketplace_fee_rules WHERE id = '" + ruleId + "'");
        assertTrue(rs.next());
        assertNull(rs.getBigDecimal("max_price"), "max_price deve ser NULL para faixa sem limite superior");
    }

    // ── critério: índice composto em tax_configs(company_id, valid_from, valid_until) ─
    @Test
    void index_tax_configs_company_exists() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT indexname FROM pg_indexes WHERE indexname = 'idx_tax_configs_company'");
        assertTrue(rs.next(), "Índice idx_tax_configs_company deve existir");
        assertEquals("idx_tax_configs_company", rs.getString("indexname"));
    }

    // ── critério: índice em marketplace_fee_rules(marketplace_id, min_price) ─
    @Test
    void index_fee_rules_marketplace_exists() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT indexname FROM pg_indexes WHERE indexname = 'idx_fee_rules_marketplace'");
        assertTrue(rs.next(), "Índice idx_fee_rules_marketplace deve existir");
    }

    // ── critério: CHECK charge_moment rejeita valor inválido ─────────────────
    @Test
    void check_constraint_rejects_invalid_charge_moment() throws Exception {
        UUID companyId = insertCompany("20304050000160", "Empresa Check", "LUCRO_REAL");

        var ex = assertThrows(SQLException.class, () -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO tax_configs
                        (company_id, charge_moment, calculation_base, rate_percentage, valid_from)
                    VALUES (?, 'MOMENTO_INVALIDO', 'GROSS_REVENUE', 0.06, ?)
                    """)) {
                ps.setObject(1, companyId);
                ps.setDate(2, Date.valueOf(LocalDate.of(2025, 1, 1)));
                ps.execute();
            }
        });
        assertTrue(ex.getSQLState().startsWith("23"),
                "Esperado SQLState 23xxx (check violation), foi: " + ex.getSQLState());
    }

    // ── critério: CHECK calculation_base rejeita valor inválido ─────────────
    @Test
    void check_constraint_rejects_invalid_calculation_base() throws Exception {
        UUID companyId = insertCompany("30405060000170", "Empresa CalcBase", "LUCRO_PRESUMIDO");

        var ex = assertThrows(SQLException.class, () -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO tax_configs
                        (company_id, charge_moment, calculation_base, rate_percentage, valid_from)
                    VALUES (?, 'ON_SALE', 'BASE_INVALIDA', 0.06, ?)
                    """)) {
                ps.setObject(1, companyId);
                ps.setDate(2, Date.valueOf(LocalDate.of(2025, 1, 1)));
                ps.execute();
            }
        });
        assertTrue(ex.getSQLState().startsWith("23"),
                "Esperado SQLState 23xxx (check violation), foi: " + ex.getSQLState());
    }

    // ── critério: rate_percentage usa escala NUMERIC(6,4) ────────────────────
    @Test
    void rate_percentage_preserves_scale_4() throws Exception {
        UUID companyId = insertCompany("40506070000180", "Empresa Scale", "SIMPLES_NACIONAL");
        UUID configId = UUID.randomUUID();

        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO tax_configs
                    (id, company_id, charge_moment, calculation_base, rate_percentage, valid_from)
                VALUES (?, ?, 'ON_SALE', 'GROSS_REVENUE', 0.0625, ?)
                """)) {
            ps.setObject(1, configId);
            ps.setObject(2, companyId);
            ps.setDate(3, Date.valueOf(LocalDate.of(2025, 1, 1)));
            ps.execute();
        }

        var rs = conn.createStatement().executeQuery(
                "SELECT rate_percentage FROM tax_configs WHERE id = '" + configId + "'");
        assertTrue(rs.next());
        assertEquals(0, new BigDecimal("0.0625").compareTo(rs.getBigDecimal("rate_percentage")),
                "rate_percentage deve preservar precisão NUMERIC(6,4)");
    }

    // ── helpers ───────────────────────────────────────────────────────────────
    private static UUID insertCompany(String cnpj, String name, String taxRegime) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO companies (id, cnpj_digits, legal_name, tax_regime) VALUES (?, ?, ?, ?)")) {
            ps.setObject(1, id);
            ps.setString(2, cnpj);
            ps.setString(3, name);
            ps.setString(4, taxRegime);
            ps.execute();
        }
        return id;
    }

    private static UUID insertMarketplace(String name, String slug) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO marketplaces (id, name, slug) VALUES (?, ?, ?)")) {
            ps.setObject(1, id);
            ps.setString(2, name);
            ps.setString(3, slug);
            ps.execute();
        }
        return id;
    }
}
