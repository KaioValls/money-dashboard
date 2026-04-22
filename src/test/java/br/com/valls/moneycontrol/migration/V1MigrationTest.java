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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Valida a migration V1 contra PostgreSQL 16 real via Testcontainers.
 * Requer Docker em execução — salta automaticamente se Docker estiver ausente
 * (@EnabledIfDockerAvailable), garantindo que mvn verify não falha em CI sem Docker.
 */
@Testcontainers
@EnabledIfDockerAvailable
class V1MigrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    private static Connection conn;

    // ── setup: Flyway roda V1 e abre conexão JDBC ────────────────────────────
    @BeforeAll
    static void runMigrationAndConnect() throws Exception {
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

    // ── critério: flyway migrate executa V1 sem erros ────────────────────────
    @Test
    void v1_migrates_without_errors() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank DESC LIMIT 1");
        assertTrue(rs.next(), "Deve haver ao menos uma migration executada com sucesso");
        assertTrue(rs.getString("version").startsWith("1"), "Versão da migration deve ser 1");
    }

    // ── critério: gen_random_uuid() funciona (extensão pgcrypto ativa) ────────
    @Test
    void pgcrypto_gen_random_uuid_works() throws Exception {
        var rs = conn.createStatement().executeQuery("SELECT gen_random_uuid()::text");
        assertTrue(rs.next());
        var uuid = rs.getString(1);
        assertNotNull(uuid, "gen_random_uuid() não deve retornar null");
        assertTrue(uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
                "UUID gerado deve ter formato válido: " + uuid);
    }

    // ── critério: CHECK tax_regime rejeita valores fora do enum ──────────────
    @Test
    void check_constraint_rejects_invalid_tax_regime() {
        var ex = assertThrows(SQLException.class, () ->
                conn.createStatement().execute("""
                        INSERT INTO companies (cnpj_digits, legal_name, tax_regime)
                        VALUES ('99999999000199', 'Empresa Inválida', 'REGIME_INVALIDO')
                        """));
        assertTrue(ex.getSQLState().startsWith("23"),
                "Esperado SQLState 23xxx (constraint violation), foi: " + ex.getSQLState());
    }

    // ── critério: UNIQUE(company_id, marketplace_id) rejeita duplicata ────────
    @Test
    void unique_constraint_rejects_duplicate_marketplace_account() throws Exception {
        UUID companyId = insertCompany("12345678000195", "Empresa Única", "SIMPLES_NACIONAL");
        UUID marketplaceId = insertMarketplace("Shopee Test", "shopee-test");

        insertMarketplaceAccount(companyId, marketplaceId);  // 1ª inserção — ok

        var ex = assertThrows(SQLException.class,            // 2ª inserção — deve falhar
                () -> insertMarketplaceAccount(companyId, marketplaceId));
        assertTrue(ex.getSQLState().startsWith("23"),
                "Esperado SQLState 23xxx (unique violation), foi: " + ex.getSQLState());
    }

    // ── critério: CHECK status em stores rejeita valor inválido ──────────────
    @Test
    void check_constraint_rejects_invalid_store_status() throws Exception {
        UUID companyId = insertCompany("11222333000181", "Empresa Status", "LUCRO_PRESUMIDO");
        UUID marketplaceId = insertMarketplace("Mercado Livre Test", "ml-test");
        UUID accountId = insertMarketplaceAccount(companyId, marketplaceId);

        var ex = assertThrows(SQLException.class, () -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO stores
                        (marketplace_account_id, company_id, marketplace_id, name, status, ads_ratio_mode)
                    VALUES (?, ?, ?, 'Loja Inválida', 'STATUS_INVALIDO', 'DIRECT')
                    """)) {
                ps.setObject(1, accountId);
                ps.setObject(2, companyId);
                ps.setObject(3, marketplaceId);
                ps.execute();
            }
        });
        assertTrue(ex.getSQLState().startsWith("23"),
                "Esperado SQLState 23xxx (check violation), foi: " + ex.getSQLState());
    }

    // ── critério: tabelas e índices criados ───────────────────────────────────
    @Test
    void all_tables_and_indexes_exist() throws Exception {
        // Tabelas
        for (String table : new String[]{"companies", "marketplaces", "marketplace_accounts", "stores"}) {
            var rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables " +
                    "WHERE table_schema = 'public' AND table_name = '" + table + "'");
            rs.next();
            assertTrue(rs.getInt(1) == 1, "Tabela '" + table + "' deve existir");
        }
        // Índices
        for (String idx : new String[]{
                "idx_marketplace_accounts_company",
                "idx_stores_company",
                "idx_stores_marketplace"}) {
            var rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM pg_indexes WHERE indexname = '" + idx + "'");
            rs.next();
            assertTrue(rs.getInt(1) == 1, "Índice '" + idx + "' deve existir");
        }
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

    private static UUID insertMarketplaceAccount(UUID companyId, UUID marketplaceId) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO marketplace_accounts (id, company_id, marketplace_id) VALUES (?, ?, ?)")) {
            ps.setObject(1, id);
            ps.setObject(2, companyId);
            ps.setObject(3, marketplaceId);
            ps.execute();
        }
        return id;
    }
}
