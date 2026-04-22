package br.com.valls.moneycontrol.domain.model;

import br.com.valls.moneycontrol.domain.enums.CalculationBase;
import br.com.valls.moneycontrol.domain.enums.ChargeMoment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TaxConfigTest {

    private static TaxConfig active2025() {
        return TaxConfig.create(UUID.randomUUID(), ChargeMoment.ON_SALE,
                CalculationBase.GROSS_REVENUE, new BigDecimal("0.0600"),
                LocalDate.of(2025, 1, 1));
    }

    // ── critério: isActiveAt retorna false para config expirada ───────────────
    @Test
    void is_active_at_returns_false_for_expired_config() {
        var config = active2025();
        config.closeAt(LocalDate.of(2025, 6, 30));
        assertFalse(config.isActiveAt(LocalDate.of(2025, 7, 1)), "Após expiração deve retornar false");
    }

    @Test
    void is_active_at_returns_false_before_valid_from() {
        var config = active2025();
        assertFalse(config.isActiveAt(LocalDate.of(2024, 12, 31)));
    }

    @Test
    void is_active_at_returns_true_on_valid_from_date() {
        var config = active2025();
        assertTrue(config.isActiveAt(LocalDate.of(2025, 1, 1)));
    }

    @Test
    void is_active_at_returns_true_when_valid_until_null() {
        var config = active2025();
        assertTrue(config.isActiveAt(LocalDate.of(2030, 12, 31)));
    }

    @Test
    void is_active_at_returns_true_on_last_day() {
        var config = active2025();
        config.closeAt(LocalDate.of(2025, 12, 31));
        assertTrue(config.isActiveAt(LocalDate.of(2025, 12, 31)));
    }

    // ── closeAt fecha a vigência ──────────────────────────────────────────────
    @Test
    void close_at_sets_valid_until_and_inactive() {
        var config = active2025();
        config.closeAt(LocalDate.of(2025, 6, 30));
        assertEquals(LocalDate.of(2025, 6, 30), config.getValidUntil());
        assertFalse(config.isActive());
    }

    // ── criação com rate inválido ─────────────────────────────────────────────
    @Test
    void create_with_zero_rate_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> TaxConfig.create(UUID.randomUUID(), ChargeMoment.ON_SALE,
                        CalculationBase.GROSS_REVENUE, BigDecimal.ZERO, LocalDate.now()));
    }

    @Test
    void create_with_null_charge_moment_throws() {
        assertThrows(NullPointerException.class,
                () -> TaxConfig.create(UUID.randomUUID(), null,
                        CalculationBase.GROSS_REVENUE, new BigDecimal("0.06"), LocalDate.now()));
    }
}
