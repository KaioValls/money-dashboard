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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Valida a migration V5 (tabelas fato particionadas) contra PostgreSQL 16 real.
 * Salta automaticamente se Docker não estiver disponível.
 */
@Testcontainers
@EnabledIfDockerAvailable
class V5MigrationTest {

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

    // ── critério: V5 migra sem erros após V1-V4 ──────────────────────────────
    @Test
    void v5_migrates_after_v1_to_v4_without_errors() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true");
        rs.next();
        assertTrue(rs.getInt(1) >= 5, "Devem existir ao menos 5 migrations executadas");
    }

    // ── critério: SELECT COUNT(*) FROM sale_entries retorna 0 ────────────────
    @Test
    void sale_entries_is_empty_after_migration() throws Exception {
        var rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM sale_entries");
        rs.next();
        assertEquals(0, rs.getInt(1), "sale_entries deve estar vazia após migration");
    }

    // ── critério: SELECT COUNT(*) FROM product_purchase_entries retorna 0 ────
    @Test
    void purchase_entries_is_empty_after_migration() throws Exception {
        var rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM product_purchase_entries");
        rs.next();
        assertEquals(0, rs.getInt(1), "product_purchase_entries deve estar vazia após migration");
    }

    // ── critério: iso_year na PK composta ─────────────────────────────────────
    @Test
    void sale_entries_pk_includes_iso_year() throws Exception {
        var rs = conn.createStatement().executeQuery("""
                SELECT kcu.column_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                    ON tc.constraint_name = kcu.constraint_name
                WHERE tc.table_name = 'sale_entries'
                  AND tc.constraint_type = 'PRIMARY KEY'
                """);
        boolean foundIsoYear = false;
        while (rs.next()) {
            if ("iso_year".equals(rs.getString("column_name"))) foundIsoYear = true;
        }
        assertTrue(foundIsoYear, "iso_year deve fazer parte da PK composta de sale_entries");
    }

    // ── critério: FK date_key → dim_date está presente em sale_entries ────────
    @Test
    void sale_entries_has_fk_to_dim_date() throws Exception {
        var rs = conn.createStatement().executeQuery("""
                SELECT COUNT(*) FROM information_schema.referential_constraints rc
                JOIN information_schema.key_column_usage kcu
                    ON rc.constraint_name = kcu.constraint_name
                WHERE kcu.table_name IN ('sale_entries_2024','sale_entries_2025','sale_entries_2026')
                  AND kcu.column_name = 'date_key'
                """);
        rs.next();
        assertTrue(rs.getInt(1) > 0,
                "FK de date_key para dim_date deve existir em sale_entries (herdada pelas partições)");
    }

    // ── critério: INSERT iso_year=2025 roteia para sale_entries_2025 ──────────
    @Test
    void insert_2025_routes_to_sale_entries_2025() throws Exception {
        UUID companyId    = insertCompany("77889900000101", "Empresa Partition", "SIMPLES_NACIONAL");
        UUID marketId     = insertMarketplace("Partition Market", "partition-mkt");
        UUID accountId    = insertMarketplaceAccount(companyId, marketId);
        UUID storeId      = insertStore(accountId, companyId, marketId, "Loja Partition");
        UUID productId    = insertProduct(companyId, "SKU-PART-2025", "Produto Partition");
        UUID listingId    = insertListing(productId, storeId, marketId, companyId,
                                          "Anúncio Partition", new BigDecimal("100.00"));
        // date_key para 2025-01-06 (ISO week 2, iso_year 2025)
        int dateKey = 20250106;

        UUID entryId = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO sale_entries (
                    id, company_id, store_id, listing_id, marketplace_id,
                    date_key, iso_year, iso_week_number, units_sold,
                    sale_price_snapshot, unit_cost_snapshot,
                    tax_rate_snapshot, tax_charge_moment_snapshot,
                    fee_percentage_snapshot, fixed_fee_snapshot,
                    gross_revenue, total_cogs, marketplace_fee_total,
                    tax_amount, gross_profit, net_profit,
                    net_margin_percentage, roi_percentage
                ) VALUES (
                    ?, ?, ?, ?, ?,
                    ?, 2025, 2, 10,
                    100.00, 40.0000,
                    0.0600, 'ON_SALE',
                    0.1600, 0.00,
                    1000.00, 400.00, 160.00,
                    60.00, 440.00, 380.00,
                    0.3800, 0.9500
                )
                """)) {
            ps.setObject(1, entryId);
            ps.setObject(2, companyId);
            ps.setObject(3, storeId);
            ps.setObject(4, listingId);
            ps.setObject(5, marketId);
            ps.setInt(6, dateKey);
            ps.execute();
        }

        // Verifica que a linha foi para a partição correta
        var rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM sale_entries_2025 WHERE id = '" + entryId + "'");
        rs.next();
        assertEquals(1, rs.getInt(1),
                "Entrada com iso_year=2025 deve estar em sale_entries_2025");

        // Verifica que aparece na tabela pai também
        rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM sale_entries WHERE id = '" + entryId + "' AND iso_year = 2025");
        rs.next();
        assertEquals(1, rs.getInt(1),
                "sale_entries (tabela pai) deve retornar a entrada inserida em 2025");
    }

    // ── critério: INSERT iso_year=2026 roteia para sale_entries_2026 ──────────
    @Test
    void insert_2026_routes_to_sale_entries_2026() throws Exception {
        UUID companyId = insertCompany("88990011000112", "Empresa Part2026", "LUCRO_REAL");
        UUID marketId  = insertMarketplace("Market 2026", "market-2026");
        UUID accountId = insertMarketplaceAccount(companyId, marketId);
        UUID storeId   = insertStore(accountId, companyId, marketId, "Loja 2026");
        UUID productId = insertProduct(companyId, "SKU-2026", "Produto 2026");
        UUID listingId = insertListing(productId, storeId, marketId, companyId,
                                       "Anúncio 2026", new BigDecimal("50.00"));

        UUID entryId = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO sale_entries (
                    id, company_id, store_id, listing_id, marketplace_id,
                    date_key, iso_year, iso_week_number, units_sold,
                    sale_price_snapshot, unit_cost_snapshot,
                    tax_rate_snapshot, tax_charge_moment_snapshot,
                    fee_percentage_snapshot, fixed_fee_snapshot,
                    gross_revenue, total_cogs, marketplace_fee_total,
                    tax_amount, gross_profit, net_profit,
                    net_margin_percentage, roi_percentage
                ) VALUES (?, ?, ?, ?, ?, 20260105, 2026, 2, 5,
                    50.00, 20.0000, 0.0600, 'ON_SALE', 0.1600, 0.00,
                    250.00, 100.00, 40.00, 15.00, 110.00, 95.00,
                    0.3800, 0.9500)
                """)) {
            ps.setObject(1, entryId);
            ps.setObject(2, companyId);
            ps.setObject(3, storeId);
            ps.setObject(4, listingId);
            ps.setObject(5, marketId);
            ps.execute();
        }

        var rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM sale_entries_2026 WHERE id = '" + entryId + "'");
        rs.next();
        assertEquals(1, rs.getInt(1),
                "Entrada com iso_year=2026 deve estar em sale_entries_2026");
    }

    // ── critério: NUMERIC(12,2) preserva escala para valores monetários ───────
    @Test
    void numeric_precision_preserved_for_monetary_fields() throws Exception {
        var rs = conn.createStatement().executeQuery("""
                SELECT numeric_precision, numeric_scale
                FROM information_schema.columns
                WHERE table_name = 'sale_entries_2025'
                  AND column_name = 'gross_revenue'
                """);
        assertTrue(rs.next());
        assertEquals(12, rs.getInt("numeric_precision"));
        assertEquals(2,  rs.getInt("numeric_scale"),
                "gross_revenue deve ter escala 2 (NUMERIC 12,2)");
    }

    // ── critério: NUMERIC(6,4) para percentuais ───────────────────────────────
    @Test
    void numeric_precision_preserved_for_percentage_fields() throws Exception {
        var rs = conn.createStatement().executeQuery("""
                SELECT numeric_precision, numeric_scale
                FROM information_schema.columns
                WHERE table_name = 'sale_entries_2025'
                  AND column_name = 'fee_percentage_snapshot'
                """);
        assertTrue(rs.next());
        assertEquals(6, rs.getInt("numeric_precision"));
        assertEquals(4, rs.getInt("numeric_scale"),
                "fee_percentage_snapshot deve ter escala 4 (NUMERIC 6,4)");
    }

    // ── critério: NUMERIC(8,4) para margens ───────────────────────────────────
    @Test
    void numeric_precision_preserved_for_margin_fields() throws Exception {
        var rs = conn.createStatement().executeQuery("""
                SELECT numeric_precision, numeric_scale
                FROM information_schema.columns
                WHERE table_name = 'sale_entries_2025'
                  AND column_name = 'net_margin_percentage'
                """);
        assertTrue(rs.next());
        assertEquals(8, rs.getInt("numeric_precision"));
        assertEquals(4, rs.getInt("numeric_scale"),
                "net_margin_percentage deve ter escala 4 (NUMERIC 8,4)");
    }

    // ── critério: índices existem nas partições ───────────────────────────────
    @Test
    void indexes_exist_on_sale_entries_partitions() throws Exception {
        for (String idx : new String[]{
                "idx_sale_entries_2024_store_week",
                "idx_sale_entries_2024_listing",
                "idx_sale_entries_2024_company",
                "idx_sale_entries_2025_store_week",
                "idx_sale_entries_2025_listing",
                "idx_sale_entries_2025_company",
                "idx_sale_entries_2026_store_week",
                "idx_sale_entries_2026_listing",
                "idx_sale_entries_2026_company"}) {
            var rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM pg_indexes WHERE indexname = '" + idx + "'");
            rs.next();
            assertEquals(1, rs.getInt(1), "Índice '" + idx + "' deve existir");
        }
    }

    // ── critério: índices existem nas partições de purchase_entries ───────────
    @Test
    void indexes_exist_on_purchase_entries_partitions() throws Exception {
        for (String idx : new String[]{
                "idx_purchase_entries_2024_company",
                "idx_purchase_entries_2024_product",
                "idx_purchase_entries_2025_company",
                "idx_purchase_entries_2025_product",
                "idx_purchase_entries_2026_company",
                "idx_purchase_entries_2026_product"}) {
            var rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM pg_indexes WHERE indexname = '" + idx + "'");
            rs.next();
            assertEquals(1, rs.getInt(1), "Índice '" + idx + "' deve existir");
        }
    }

    // ── critério: INSERT em purchase_entries roteia para partição correta ─────
    @Test
    void purchase_entry_routes_to_correct_partition() throws Exception {
        UUID companyId = insertCompany("99001122000123", "Empresa Purchase", "LUCRO_PRESUMIDO");
        UUID productId = insertProduct(companyId, "SKU-PURCH-2025", "Produto Compra");

        UUID purchaseId = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO product_purchase_entries (
                    id, company_id, product_id, iso_year, iso_week_number,
                    quantity, unit_cost, total_cost, total_landed_cost
                ) VALUES (?, ?, ?, 2025, 5, 10, 25.0000, 250.00, 250.00)
                """)) {
            ps.setObject(1, purchaseId);
            ps.setObject(2, companyId);
            ps.setObject(3, productId);
            ps.execute();
        }

        var rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM product_purchase_entries_2025 WHERE id = '" + purchaseId + "'");
        rs.next();
        assertEquals(1, rs.getInt(1),
                "product_purchase_entries com iso_year=2025 deve ir para _2025");
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

    private static UUID insertProduct(UUID companyId, String sku, String name) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO products (id, company_id, sku, name) VALUES (?, ?, ?, ?)")) {
            ps.setObject(1, id); ps.setObject(2, companyId);
            ps.setString(3, sku); ps.setString(4, name);
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
            ps.setObject(1, id); ps.setObject(2, productId); ps.setObject(3, storeId);
            ps.setObject(4, marketplaceId); ps.setObject(5, companyId);
            ps.setString(6, title); ps.setBigDecimal(7, price);
            ps.execute();
        }
        return id;
    }
}
