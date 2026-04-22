package br.com.valls.moneycontrol.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MarketplaceFeeRuleTest {

    private static MarketplaceFeeRule rule(BigDecimal min, BigDecimal max) {
        return MarketplaceFeeRule.create(UUID.randomUUID(), min, max,
                new BigDecimal("0.1600"), BigDecimal.ZERO,
                LocalDate.of(2025, 1, 1), 0);
    }

    // ── critério: appliesTo(50.00) retorna false para regra min=100 ───────────
    @Test
    void applies_to_returns_false_when_price_below_min() {
        var r = rule(new BigDecimal("100.00"), null);
        assertFalse(r.appliesTo(new BigDecimal("50.00")));
    }

    @Test
    void applies_to_returns_true_when_price_equals_min() {
        var r = rule(new BigDecimal("100.00"), null);
        assertTrue(r.appliesTo(new BigDecimal("100.00")));
    }

    @Test
    void applies_to_returns_true_when_max_is_null() {
        var r = rule(new BigDecimal("0.00"), null);
        assertTrue(r.appliesTo(new BigDecimal("999999.99")));
    }

    @Test
    void applies_to_returns_false_when_price_above_max() {
        var r = rule(new BigDecimal("0.00"), new BigDecimal("100.00"));
        assertFalse(r.appliesTo(new BigDecimal("100.01")));
    }

    @Test
    void applies_to_returns_true_on_exact_max_boundary() {
        var r = rule(new BigDecimal("50.00"), new BigDecimal("100.00"));
        assertTrue(r.appliesTo(new BigDecimal("100.00")));
    }

    // ── isActiveAt ────────────────────────────────────────────────────────────
    @Test
    void is_active_at_returns_false_before_valid_from() {
        var r = rule(BigDecimal.ZERO, null);
        assertFalse(r.isActiveAt(LocalDate.of(2024, 12, 31)));
    }

    @Test
    void is_active_at_returns_true_with_null_valid_until() {
        var r = rule(BigDecimal.ZERO, null);
        assertTrue(r.isActiveAt(LocalDate.of(2030, 1, 1)));
    }

    // ── criação com dados inválidos ───────────────────────────────────────────
    @Test
    void create_with_max_less_than_min_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> rule(new BigDecimal("200.00"), new BigDecimal("100.00")));
    }

    @Test
    void create_with_negative_fee_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> MarketplaceFeeRule.create(UUID.randomUUID(),
                        BigDecimal.ZERO, null, new BigDecimal("-0.01"), BigDecimal.ZERO,
                        LocalDate.now(), 0));
    }
}
