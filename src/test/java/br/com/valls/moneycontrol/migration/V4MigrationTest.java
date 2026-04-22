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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Valida a migration V4 (dim_date) contra PostgreSQL 16 real.
 * Salta automaticamente se Docker não estiver disponível.
 */
@Testcontainers
@EnabledIfDockerAvailable
class V4MigrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    private static Connection conn;

    @BeforeAll
    static void runMigrationsAndConnect() throws Exception {
        long start = System.currentTimeMillis();

        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 30_000,
                "V4 deve migrar em menos de 30 segundos; levou: " + elapsed + "ms");

        conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    @AfterAll
    static void closeConnection() throws Exception {
        if (conn != null && !conn.isClosed()) conn.close();
    }

    // ── critério: V4 migra sem erros após V1-V3 ──────────────────────────────
    @Test
    void v4_migrates_after_v1_v2_v3_without_errors() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true");
        rs.next();
        assertTrue(rs.getInt(1) >= 4, "Devem existir ao menos 4 migrations executadas");
    }

    // ── critério: dim_date contém 7.305+ linhas (2020 a 2040) ────────────────
    @Test
    void dim_date_contains_at_least_7305_rows() throws Exception {
        var rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM dim_date");
        rs.next();
        int count = rs.getInt(1);
        assertTrue(count >= 7305,
                "dim_date deve ter ao menos 7.305 linhas; encontrado: " + count);
    }

    // ── critério: CURRENT_DATE retorna exatamente 1 linha ────────────────────
    @Test
    void current_date_has_exactly_one_row() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM dim_date WHERE full_date = CURRENT_DATE");
        rs.next();
        assertEquals(1, rs.getInt(1),
                "CURRENT_DATE deve ter exatamente 1 linha em dim_date");
    }

    // ── critério: 2025-01-01 tem iso_week_number=1 e iso_year=2025 ───────────
    @Test
    void date_2025_01_01_has_correct_iso_week() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT iso_week_number, iso_year FROM dim_date WHERE full_date = '2025-01-01'");
        assertTrue(rs.next(), "2025-01-01 deve existir em dim_date");
        assertEquals(1, rs.getInt("iso_week_number"),
                "2025-01-01 deve estar na semana ISO 1");
        assertEquals(2025, rs.getInt("iso_year"),
                "2025-01-01 deve ter iso_year=2025");
    }

    // ── critério: week_label está em português ("Sem X / Mês AAAA") ──────────
    @Test
    void week_label_is_in_portuguese_format() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT week_label FROM dim_date WHERE full_date = '2025-03-15'");
        assertTrue(rs.next());
        String label = rs.getString("week_label");
        assertTrue(label.startsWith("Sem "),
                "week_label deve começar com 'Sem '; encontrado: " + label);
        assertTrue(label.contains("Mar"),
                "week_label de março deve conter 'Mar'; encontrado: " + label);
        assertTrue(label.contains("2025"),
                "week_label deve conter o ano; encontrado: " + label);
    }

    // ── critério: day_name está em português ─────────────────────────────────
    @Test
    void day_name_is_in_portuguese() throws Exception {
        // 2025-01-01 = Wednesday = Quarta-feira
        var rs = conn.createStatement().executeQuery(
                "SELECT day_name FROM dim_date WHERE full_date = '2025-01-01'");
        assertTrue(rs.next());
        assertEquals("Quarta-feira", rs.getString("day_name"),
                "2025-01-01 (quarta-feira) deve ter day_name='Quarta-feira'");
    }

    // ── critério: is_weekend correto para sábado e segunda ───────────────────
    @Test
    void is_weekend_is_correct() throws Exception {
        // 2025-01-04 = Saturday
        var rs = conn.createStatement().executeQuery(
                "SELECT is_weekend FROM dim_date WHERE full_date = '2025-01-04'");
        assertTrue(rs.next());
        assertTrue(rs.getBoolean("is_weekend"), "Sábado deve ser weekend=true");

        // 2025-01-06 = Monday
        rs = conn.createStatement().executeQuery(
                "SELECT is_weekend FROM dim_date WHERE full_date = '2025-01-06'");
        assertTrue(rs.next());
        assertTrue(!rs.getBoolean("is_weekend"), "Segunda-feira deve ser weekend=false");
    }

    // ── critério: week_of_month correto (dias 1-7 = semana 1) ────────────────
    @Test
    void week_of_month_is_correct() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT full_date, week_of_month FROM dim_date " +
                "WHERE full_date BETWEEN '2025-01-01' AND '2025-01-31' " +
                "ORDER BY full_date");
        while (rs.next()) {
            int dom = rs.getDate("full_date").toLocalDate().getDayOfMonth();
            int expected = (dom - 1) / 7 + 1;
            assertEquals(expected, rs.getInt("week_of_month"),
                    "week_of_month incorreto para dia " + dom);
        }
    }

    // ── critério: índice idx_dim_date_iso_week existe ─────────────────────────
    @Test
    void index_iso_week_exists() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT indexname FROM pg_indexes WHERE indexname = 'idx_dim_date_iso_week'");
        assertTrue(rs.next(), "Índice idx_dim_date_iso_week deve existir");
    }

    // ── critério: índice idx_dim_date_month_year existe ───────────────────────
    @Test
    void index_month_year_exists() throws Exception {
        var rs = conn.createStatement().executeQuery(
                "SELECT indexname FROM pg_indexes WHERE indexname = 'idx_dim_date_month_year'");
        assertTrue(rs.next(), "Índice idx_dim_date_month_year deve existir");
    }
}
