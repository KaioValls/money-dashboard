package br.com.valls.moneycontrol.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductPurchaseEntryTest {

    private static ProductPurchaseEntry entry(boolean hasFreight, BigDecimal freight) {
        return ProductPurchaseEntry.create(
                UUID.randomUUID(), UUID.randomUUID(),
                2025, 10, 10,
                new BigDecimal("30.0000"),
                hasFreight, freight);
    }

    @Test
    void total_cost_is_unit_cost_times_quantity() {
        var e = entry(false, null);
        assertEquals(0, new BigDecimal("300.00").compareTo(e.totalCost()));
    }

    @Test
    void freight_per_unit_is_zero_when_no_freight() {
        var e = entry(false, null);
        assertEquals(0, BigDecimal.ZERO.compareTo(e.freightPerUnit()));
    }

    @Test
    void freight_per_unit_calculated_correctly() {
        // freight=50, quantity=10 → freightPerUnit=5.0000
        var e = entry(true, new BigDecimal("50.00"));
        assertEquals(0, new BigDecimal("5.0000").compareTo(e.freightPerUnit()));
    }

    @Test
    void total_landed_cost_includes_freight() {
        // totalCost=300, freight=50 → totalLanded=350
        var e = entry(true, new BigDecimal("50.00"));
        assertEquals(0, new BigDecimal("350.00").compareTo(e.totalLandedCost()));
    }

    @Test
    void total_landed_cost_equals_total_cost_when_no_freight() {
        var e = entry(false, null);
        assertEquals(0, e.totalCost().compareTo(e.totalLandedCost()));
    }

    @Test
    void create_with_zero_quantity_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ProductPurchaseEntry.create(UUID.randomUUID(), UUID.randomUUID(),
                        2025, 10, 0, new BigDecimal("10"), false, null));
    }

    @Test
    void create_with_negative_unit_cost_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ProductPurchaseEntry.create(UUID.randomUUID(), UUID.randomUUID(),
                        2025, 10, 5, new BigDecimal("-1"), false, null));
    }
}
