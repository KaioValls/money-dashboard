package br.com.valls.moneycontrol.domain.model;

import br.com.valls.moneycontrol.domain.enums.AdsRatioMode;
import br.com.valls.moneycontrol.domain.enums.StoreStatus;
import br.com.valls.moneycontrol.domain.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StoreTest {

    private static Store newStore() {
        return Store.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Loja Teste");
    }

    // ── critério: Store.pause() em CLOSED lança BusinessException ────────────
    @Test
    void pause_on_closed_store_throws_business_exception() {
        var s = newStore();
        s.close();
        assertThrows(BusinessException.class, s::pause);
    }

    @Test
    void reactivate_on_closed_store_throws_business_exception() {
        var s = newStore();
        s.close();
        assertThrows(BusinessException.class, s::reactivate);
    }

    @Test
    void pause_on_paused_store_throws() {
        var s = newStore();
        s.pause();
        assertThrows(BusinessException.class, s::pause);
    }

    @Test
    void reactivate_on_active_store_throws() {
        var s = newStore();
        assertThrows(BusinessException.class, s::reactivate);
    }

    @Test
    void close_on_closed_store_throws() {
        var s = newStore();
        s.close();
        assertThrows(BusinessException.class, s::close);
    }

    // ── transições válidas ────────────────────────────────────────────────────
    @Test
    void active_to_paused_to_active() {
        var s = newStore();
        assertEquals(StoreStatus.ACTIVE, s.getStatus());
        s.pause();
        assertEquals(StoreStatus.PAUSED, s.getStatus());
        s.reactivate();
        assertEquals(StoreStatus.ACTIVE, s.getStatus());
    }

    @Test
    void close_sets_active_false() {
        var s = newStore();
        s.close();
        assertEquals(StoreStatus.CLOSED, s.getStatus());
        assertFalse(s.isActive());
    }

    // ── updateAdsRatioMode ────────────────────────────────────────────────────
    @Test
    void update_ads_ratio_mode() {
        var s = newStore();
        s.updateAdsRatioMode(AdsRatioMode.CAMPAIGN);
        assertEquals(AdsRatioMode.CAMPAIGN, s.getAdsRatioMode());
    }

    @Test
    void update_ads_ratio_mode_null_throws() {
        var s = newStore();
        assertThrows(NullPointerException.class, () -> s.updateAdsRatioMode(null));
    }

    // ── criação ───────────────────────────────────────────────────────────────
    @Test
    void create_with_blank_name_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> Store.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "  "));
    }

    @Test
    void default_status_is_active_and_direct() {
        var s = newStore();
        assertEquals(StoreStatus.ACTIVE, s.getStatus());
        assertEquals(AdsRatioMode.DIRECT, s.getAdsRatioMode());
        assertTrue(s.isActive());
    }
}
