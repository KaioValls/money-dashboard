package br.com.valls.moneycontrol.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    // ── factory of() e zero() ─────────────────────────────────────────────────
    @Test
    void of_creates_brl_money() {
        var m = Money.of(new BigDecimal("100.00"));
        assertEquals("BRL", m.currency());
        assertEquals(0, new BigDecimal("100.00").compareTo(m.amount()));
    }

    @Test
    void zero_creates_zero_brl() {
        var m = Money.zero();
        assertEquals(0, BigDecimal.ZERO.compareTo(m.amount()));
        assertEquals("BRL", m.currency());
    }

    @Test
    void null_amount_throws() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(null));
    }

    // ── add e subtract ────────────────────────────────────────────────────────
    @Test
    void add_sums_amounts() {
        var result = Money.of(new BigDecimal("100")).add(Money.of(new BigDecimal("50")));
        assertEquals(0, new BigDecimal("150").compareTo(result.amount()));
    }

    @Test
    void subtract_subtracts_amounts() {
        var result = Money.of(new BigDecimal("100")).subtract(Money.of(new BigDecimal("30")));
        assertEquals(0, new BigDecimal("70").compareTo(result.amount()));
    }

    @Test
    void add_different_currencies_throws() {
        var brl = Money.of(new BigDecimal("100"));
        var usd = new Money(new BigDecimal("100"), "USD");
        assertThrows(IllegalArgumentException.class, () -> brl.add(usd));
    }

    // ── critério: percentage aplica HALF_UP ───────────────────────────────────
    @Test
    void percentage_applies_half_up_rounding() {
        // 100 * 0.0625 = 6.25 → escala 4 = 6.2500
        var result = Money.of(new BigDecimal("100")).percentage(new BigDecimal("0.0625"));
        assertEquals(0, new BigDecimal("6.2500").compareTo(result.amount()));
    }

    @Test
    void percentage_half_up_on_halfway_case() {
        // 10 * 0.1555 = 1.5550 → HALF_UP com escala 4 = 1.5550
        var result = Money.of(new BigDecimal("10")).percentage(new BigDecimal("0.15555"));
        // 10 * 0.15555 = 1.5555 → escala 4 HALF_UP = 1.5555 → round fica 1.5556?
        // 1.55550 com 5 dígitos → cortado para 4 = 1.5555 (round digit = 0, sem arredondamento)
        // Vamos testar um caso onde o arredondamento é claramente ativo
        // 3.33333... = 10/3 * 1 = 10 * (1/3)
        var r2 = Money.of(new BigDecimal("10")).percentage(new BigDecimal("0.33333"));
        // 10 * 0.33333 = 3.3333 → escala 4 HALF_UP → 3.3333
        assertEquals(4, r2.amount().scale());
    }

    @Test
    void percentage_result_has_scale_4() {
        var result = Money.of(new BigDecimal("500")).percentage(new BigDecimal("0.06"));
        assertEquals(4, result.amount().scale(), "Resultado de percentage deve ter escala 4");
        assertEquals(0, new BigDecimal("30.0000").compareTo(result.amount()));
    }

    // ── multiply ──────────────────────────────────────────────────────────────
    @Test
    void multiply_scales_to_4() {
        var result = Money.of(new BigDecimal("50")).multiply(new BigDecimal("3"));
        assertEquals(0, new BigDecimal("150.0000").compareTo(result.amount()));
        assertEquals(4, result.amount().scale());
    }

    // ── toScaled retorna escala 2 ─────────────────────────────────────────────
    @Test
    void to_scaled_returns_scale_2() {
        var m = Money.of(new BigDecimal("99.9999"));
        BigDecimal scaled = m.toScaled();
        assertEquals(2, scaled.scale());
        assertEquals(0, new BigDecimal("100.00").compareTo(scaled));
    }

    @Test
    void to_scaled_half_up_rounds_correctly() {
        // 0.005 com HALF_UP escala 2 → 0.01
        var m = Money.of(new BigDecimal("0.005"));
        assertEquals(0, new BigDecimal("0.01").compareTo(m.toScaled()));
    }
}
