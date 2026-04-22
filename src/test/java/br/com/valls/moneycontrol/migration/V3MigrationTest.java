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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Valida a migration V3 (produtos, anúncios e histórico de preço) contra PostgreSQL 16 real.
 * Salta automaticamente se Docker não estiver disponível.
 */
@Testcontainers
@EnabledIfDockerAvailable
class V3MigrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    private static Connection conn;

    @BeforeAll
    static void runMigrationsAndConnect() throws Exception {
        // V1 + V2 + V3 devem migrar sem erros
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

    // ── critério: V3 migra sem erros após V1 e V2 ────────────────────────────
    @Test
    void v3_migrates_after_v1_v2_without_errors() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true");
        rs.next();
        assertTrue(rs.getInt(1) >= 3, "Devem existir ao menos 3 migrations executadas (V1, V2 e V3)");
    }

    // ── critério: tabelas criadas pela V3 existem ─────────────────────────────
    @Test
    void all_v3_tables_exist() throws Exception {
        for (String table : new String[]{"products", "listings", "listing_price_history"}) {
            var rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables " +
                    "WHERE table_schema = 'public' AND table_name = '" + table + "'");
            rs.next();
            assertEquals(1, rs.getInt(1), "Tabela '" + table + "' deve existir");
        }
    }

    // ── critério: UNIQUE(company_id, sku) rejeita SKU duplicado por empresa ──
    @Test
    void unique_constraint_rejects_duplicate_sku_per_company() throws Exception {
        UUID companyId = insertCompany("11223344000155", "Empresa SKU", "SIMPLES_NACIONAL");

        insertProduct(companyId, "SKU-001", "Produto A");  // 1ª inserção — ok

        var ex = assertThrows(SQLException.class,
                () -> insertProduct(companyId, "SKU-001", "Produto A Duplicado"));  // 2ª — deve falhar
        assertTrue(ex.getSQLState().startsWith("23"),
                "Esperado SQLState 23xxx (unique violation), foi: " + ex.getSQLState());
    }

    // ── critério: mesmo SKU aceito em empresas distintas ─────────────────────
    @Test
    void same_sku_allowed_for_different_companies() throws Exception {
        UUID company1 = insertCompany("22334455000166", "Empresa SKU1", "LUCRO_REAL");
        UUID company2 = insertCompany("33445566000177", "Empresa SKU2", "LUCRO_PRESUMIDO");

        insertProduct(company1, "SKU-COMUM", "Produto Empresa 1");
        insertProduct(company2, "SKU-COMUM", "Produto Empresa 2");  // deve funcionar

        var rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM products WHERE sku = 'SKU-COMUM'");
        rs.next();
        assertEquals(2, rs.getInt(1), "Mesmo SKU deve ser aceito em empresas distintas");
    }

    // ── critério: listings.marketplace_id denormalizado é aceito ─────────────
    @Test
    void listings_accepts_denormalized_marketplace_id() throws Exception {
        UUID companyId = insertCompany("44556677000188", "Empresa Listing", "SIMPLES_NACIONAL");
        UUID marketplaceId = insertMarketplace("Amazon V3", "amazon-v3");
        UUID accountId = insertMarketplaceAccount(companyId, marketplaceId);
        UUID storeId = insertStore(accountId, companyId, marketplaceId, "Loja Amazon V3");
        UUID productId = insertProduct(companyId, "SKU-LIST-001", "Produto Listing");

        UUID listingId = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO listings
                    (id, product_id, store_id, marketplace_id, company_id, title, sale_price, status)
                VALUES (?, ?, ?, ?, ?, 'Anúncio Teste', 99.90, 'ACTIVE')
                """)) {
            ps.setObject(1, listingId);
            ps.setObject(2, productId);
            ps.setObject(3, storeId);
            ps.setObject(4, marketplaceId);  // denorm
            ps.setObject(5, companyId);       // denorm
            ps.execute();
        }

        var rs = conn.createStatement().executeQuery(
                "SELECT marketplace_id FROM listings WHERE id = '" + listingId + "'");
        assertTrue(rs.next());
        assertEquals(marketplaceId, UUID.fromString(rs.getString("marketplace_id")),
                "marketplace_id denorm deve ser igual ao marketplace da store");
    }

    // ── critério: listing_price_history aceita valid_until NULL ──────────────
    @Test
    void price_history_accepts_null_valid_until() throws Exception {
        UUID companyId = insertCompany("55667788000199", "Empresa Price", "SIMPLES_NACIONAL");
        UUID marketplaceId = insertMarketplace("Shopee V3", "shopee-v3");
        UUID accountId = insertMarketplaceAccount(companyId, marketplaceId);
        UUID storeId = insertStore(accountId, companyId, marketplaceId, "Loja Shopee V3");
        UUID productId = insertProduct(companyId, "SKU-PRICE-001", "Produto Price");
        UUID listingId = insertListing(productId, storeId, marketplaceId, companyId, "Anúncio Price", new BigDecimal("79.90"));

        UUID historyId = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO listing_price_history
                    (id, listing_id, sale_price, valid_from, valid_until)
                VALUES (?, ?, 79.90, ?, NULL)
                """)) {
            ps.setObject(1, historyId);
            ps.setObject(2, listingId);
            ps.setTimestamp(3, Timestamp.from(Instant.now()));
            ps.execute();
        }

        var rs = conn.createStatement().executeQuery(
                "SELECT valid_until FROM listing_price_history WHERE id = '" + historyId + "'");
        assertTrue(rs.next());
        assertNull(rs.getTimestamp("valid_until"), "valid_until deve ser NULL para preço atualmente vigente");
    }

    // ── critério: CHECK status em listings rejeita valor inválido ────────────
    @Test
    void check_constraint_rejects_invalid_listing_status() throws Exception {
        UUID companyId = insertCompany("66778899000100", "Empresa Status Listing", "LUCRO_REAL");
        UUID marketplaceId = insertMarketplace("ML V3", "ml-v3");
        UUID accountId = insertMarketplaceAccount(companyId, marketplaceId);
        UUID storeId = insertStore(accountId, companyId, marketplaceId, "Loja ML V3");
        UUID productId = insertProduct(companyId, "SKU-STATUS-001", "Produto Status");

        var ex = assertThrows(SQLException.class, () -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO listings
                        (product_id, store_id, marketplace_id, company_id, title, sale_price, status)
                    VALUES (?, ?, ?, ?, 'Anúncio Inválido', 10.00, 'STATUS_INVALIDO')
                    """)) {
                ps.setObject(1, productId);
                ps.setObject(2, storeId);
                ps.setObject(3, marketplaceId);
                ps.setObject(4, companyId);
                ps.execute();
            }
        });
        assertTrue(ex.getSQLState().startsWith("23"),
                "Esperado SQLState 23xxx (check violation), foi: " + ex.getSQLState());
    }

    // ── critério: índice idx_products_sku(company_id, sku) existe ────────────
    @Test
    void index_products_sku_exists() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT indexname FROM pg_indexes WHERE indexname = 'idx_products_sku'");
        assertTrue(rs.next(), "Índice idx_products_sku deve existir");
        assertEquals("idx_products_sku", rs.getString("indexname"));
    }

    // ── critério: índice idx_products_company existe ──────────────────────────
    @Test
    void index_products_company_exists() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT indexname FROM pg_indexes WHERE indexname = 'idx_products_company'");
        assertTrue(rs.next(), "Índice idx_products_company deve existir");
    }

    // ── critério: índice idx_listings_store existe ────────────────────────────
    @Test
    void index_listings_store_exists() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT indexname FROM pg_indexes WHERE indexname = 'idx_listings_store'");
        assertTrue(rs.next(), "Índice idx_listings_store deve existir");
    }

    // ── critério: índice idx_listings_product existe ──────────────────────────
    @Test
    void index_listings_product_exists() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT indexname FROM pg_indexes WHERE indexname = 'idx_listings_product'");
        assertTrue(rs.next(), "Índice idx_listings_product deve existir");
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

    private static UUID insertStore(UUID accountId, UUID companyId, UUID marketplaceId, String name) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO stores (id, marketplace_account_id, company_id, marketplace_id, name)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, accountId);
            ps.setObject(3, companyId);
            ps.setObject(4, marketplaceId);
            ps.setString(5, name);
            ps.execute();
        }
        return id;
    }

    private static UUID insertProduct(UUID companyId, String sku, String name) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO products (id, company_id, sku, name) VALUES (?, ?, ?, ?)")) {
            ps.setObject(1, id);
            ps.setObject(2, companyId);
            ps.setString(3, sku);
            ps.setString(4, name);
            ps.execute();
        }
        return id;
    }

    private static UUID insertListing(UUID productId, UUID storeId, UUID marketplaceId,
                                       UUID companyId, String title, BigDecimal price) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO listings (id, product_id, store_id, marketplace_id, company_id, title, sale_price)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, productId);
            ps.setObject(3, storeId);
            ps.setObject(4, marketplaceId);
            ps.setObject(5, companyId);
            ps.setString(6, title);
            ps.setBigDecimal(7, price);
            ps.execute();
        }
        return id;
    }
}
