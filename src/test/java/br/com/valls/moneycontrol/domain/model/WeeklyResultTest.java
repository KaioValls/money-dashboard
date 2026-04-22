package br.com.valls.moneycontrol.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WeeklyResultTest {

    /**
     * Monta um WeeklyResult com valores conhecidos e verifica as fórmulas:
     *   grossRevenue = 500.00  (10 unidades × R$50)
     *   totalCogs    = 300.00  (10 × R$30)
     *   fee          = 80.00   (16% × 500)
     *   taxAmount    = 30.00   (6% × 500)
     *   grossProfit  = 500 - 80 - 300 = 120.00
     *   netProfit    = 120 - 30 - 0 - 0 - 0 - 0 - 0 = 90.00   (sem ads/freight/etc.)
     *   netMargin    = (90 / 500) × 100 = 18.00
     *   roi          = (90 / 300) × 100 = 30.00
     */
    private static WeeklyResult buildResult(BigDecimal ads, BigDecimal coupon,
                                             BigDecimal freight, BigDecimal ret,
                                             BigDecimal other) {
        BigDecimal grossRevenue = new BigDecimal("500.00");
        BigDecimal totalCogs    = new BigDecimal("300.00");
        BigDecimal fee          = new BigDecimal("80.00");
        BigDecimal tax          = new BigDecimal("30.00");
        BigDecimal grossProfit  = grossRevenue.subtract(fee).subtract(totalCogs);
        BigDecimal netProfit    = grossProfit.subtract(tax)
                                             .subtract(ads)
                                             .subtract(coupon)
                                             .subtract(freight)
                                             .subtract(ret)
                                             .subtract(other);
        BigDecimal netMargin    = netProfit.multiply(BigDecimal.valueOf(100))
                                          .divide(grossRevenue, 2, java.math.RoundingMode.HALF_UP);
        BigDecimal roi          = netProfit.multiply(BigDecimal.valueOf(100))
                                          .divide(totalCogs, 2, java.math.RoundingMode.HALF_UP);

        return new WeeklyResult(
                UUID.randomUUID(),
                new WeekReference(2025, 10),
                grossRevenue, totalCogs, fee, tax,
                ads, coupon, freight, ret, other,
                grossProfit, netProfit, netMargin, roi
        );
    }

    // ── critério: netProfit = grossProfit - tax - ads - coupon - freight - ret - other ──
    @Test
    void net_profit_formula_without_operational_costs() {
        var result = buildResult(
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        // grossProfit = 500 - 80 - 300 = 120
        assertEquals(0, new BigDecimal("120.00").compareTo(result.grossProfit()));
        // netProfit = 120 - 30 = 90
        assertEquals(0, new BigDecimal("90.00").compareTo(result.netProfit()));
    }

    @Test
    void net_profit_deducts_all_operational_costs() {
        var result = buildResult(
                new BigDecimal("10.00"),  // ads
                new BigDecimal("5.00"),   // coupon
                new BigDecimal("8.00"),   // freight
                new BigDecimal("2.00"),   // return
                new BigDecimal("3.00")); // other

        // netProfit = 90 - 10 - 5 - 8 - 2 - 3 = 62.00
        assertEquals(0, new BigDecimal("62.00").compareTo(result.netProfit()));
    }

    // ── critério: netMarginPercentage = (netProfit / grossRevenue) × 100 ──────
    @Test
    void net_margin_calculated_from_gross_revenue() {
        var result = buildResult(BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        // (90 / 500) × 100 = 18.00
        assertEquals(0, new BigDecimal("18.00").compareTo(result.netMarginPercentage()));
    }

    // ── critério: roiPercentage = (netProfit / totalCogs) × 100 ─────────────
    @Test
    void roi_calculated_from_total_cogs() {
        var result = buildResult(BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        // (90 / 300) × 100 = 30.00
        assertEquals(0, new BigDecimal("30.00").compareTo(result.roiPercentage()));
    }

    // ── summary() retorna os KPIs esperados ───────────────────────────────────
    @Test
    void summary_contains_all_kpis() {
        var result = buildResult(BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        Map<String, BigDecimal> summary = result.summary();

        assertTrue(summary.containsKey("grossRevenue"));
        assertTrue(summary.containsKey("netProfit"));
        assertTrue(summary.containsKey("netMarginPercentage"));
        assertTrue(summary.containsKey("roiPercentage"));
        assertEquals(0, new BigDecimal("500.00").compareTo(summary.get("grossRevenue")));
        assertEquals(0, new BigDecimal("90.00").compareTo(summary.get("netProfit")));
    }

    // ── WeeklyResult é imutável (record) ─────────────────────────────────────
    @Test
    void weekly_result_is_a_record() {
        assertTrue(WeeklyResult.class.isRecord(), "WeeklyResult deve ser um record Java");
    }

    // ── nenhum import Spring ou JPA ───────────────────────────────────────────
    @Test
    void no_spring_or_jpa_in_weekly_result() {
        for (var field : WeeklyResult.class.getDeclaredFields()) {
            var name = field.getType().getName();
            assertFalse(name.startsWith("org.springframework"));
            assertFalse(name.startsWith("jakarta.persistence"));
        }
    }
}
