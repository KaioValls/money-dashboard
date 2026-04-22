package br.com.valls.moneycontrol.domain.model;

import br.com.valls.moneycontrol.domain.enums.ListingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ListingTest {

    private static Listing newListing() {
        return Listing.create(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(),
                "Produto Teste", new BigDecimal("99.90"));
    }

    // ── critério: updatePrice retorna ListingPriceHistory com valid_from=now e valid_until=null ──
    @Test
    void update_price_returns_history_with_valid_from_now_and_null_valid_until() {
        var listing   = newListing();
        UUID changedBy = UUID.randomUUID();

        var history = listing.updatePrice(new BigDecimal("149.90"), changedBy, "promoção");

        assertNotNull(history);
        assertNotNull(history.getValidFrom());
        assertNull(history.getValidUntil(), "valid_until deve ser NULL para preço vigente");
        assertEquals(listing.getId(), history.getListingId());
        assertEquals(0, new BigDecimal("149.90").compareTo(history.getSalePrice()));
        assertEquals(changedBy, history.getChangedBy());
        assertEquals("promoção", history.getChangeReason());
    }

    @Test
    void update_price_changes_listing_sale_price() {
        var listing = newListing();
        listing.updatePrice(new BigDecimal("200.00"), UUID.randomUUID(), "reajuste");
        assertEquals(0, new BigDecimal("200.00").compareTo(listing.getSalePrice()));
    }

    @Test
    void update_price_with_zero_throws() {
        var listing = newListing();
        assertThrows(IllegalArgumentException.class,
                () -> listing.updatePrice(BigDecimal.ZERO, UUID.randomUUID(), "erro"));
    }

    @Test
    void update_price_with_negative_throws() {
        var listing = newListing();
        assertThrows(IllegalArgumentException.class,
                () -> listing.updatePrice(new BigDecimal("-10"), UUID.randomUUID(), "erro"));
    }

    // ── transições de status ──────────────────────────────────────────────────
    @Test
    void pause_and_reactivate() {
        var listing = newListing();
        listing.pause();
        assertEquals(ListingStatus.PAUSED, listing.getStatus());
        listing.reactivate();
        assertEquals(ListingStatus.ACTIVE, listing.getStatus());
    }

    @Test
    void close_sets_closed_at_and_inactive() {
        var listing = newListing();
        listing.close();
        assertEquals(ListingStatus.CLOSED, listing.getStatus());
        assertNotNull(listing.getClosedAt());
        assertFalse(listing.isActive());
    }

    @Test
    void pause_on_closed_throws() {
        var listing = newListing();
        listing.close();
        assertThrows(Exception.class, listing::pause);
    }

    // ── criação ───────────────────────────────────────────────────────────────
    @Test
    void create_with_blank_title_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> Listing.create(UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), UUID.randomUUID(), "", new BigDecimal("10")));
    }

    @Test
    void create_with_zero_price_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> Listing.create(UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), UUID.randomUUID(), "Título", BigDecimal.ZERO));
    }
}
