package br.com.valls.moneycontrol.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Registro imutável de uma faixa de vigência de preço de um anúncio.
 * Criado exclusivamente por {@link Listing#updatePrice}.
 */
@Getter
public class ListingPriceHistory {

    private final UUID       id;
    private final UUID       listingId;
    private final BigDecimal salePrice;
    private final Instant    validFrom;
    private       Instant    validUntil;   // NULL = preço atualmente vigente
    private final UUID       changedBy;
    private final String     changeReason;
    private final Instant    createdAt;

    private ListingPriceHistory(UUID id, UUID listingId, BigDecimal salePrice,
                                Instant validFrom, Instant validUntil,
                                UUID changedBy, String changeReason) {
        this.id           = id;
        this.listingId    = listingId;
        this.salePrice    = salePrice;
        this.validFrom    = validFrom;
        this.validUntil   = validUntil;
        this.changedBy    = changedBy;
        this.changeReason = changeReason;
        this.createdAt    = Instant.now();
    }

    static ListingPriceHistory create(UUID listingId, BigDecimal salePrice,
                                      Instant validFrom, UUID changedBy, String changeReason) {
        return new ListingPriceHistory(
                UUID.randomUUID(), listingId, salePrice,
                validFrom, null, changedBy, changeReason);
    }

    public static ListingPriceHistory reconstitute(UUID id, UUID listingId, BigDecimal salePrice,
                                                   Instant validFrom, Instant validUntil,
                                                   UUID changedBy, String changeReason) {
        return new ListingPriceHistory(id, listingId, salePrice, validFrom, validUntil, changedBy, changeReason);
    }

    /** Fecha esta faixa de vigência (chamado pelo use case ao criar novo preço). */
    public void closeAt(Instant until) {
        this.validUntil = until;
    }
}
