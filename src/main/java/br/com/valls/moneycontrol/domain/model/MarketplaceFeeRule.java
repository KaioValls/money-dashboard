package br.com.valls.moneycontrol.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Getter
public class MarketplaceFeeRule {

    private final UUID       id;
    private final UUID       marketplaceId;
    private final BigDecimal minPrice;
    private final BigDecimal maxPrice;        // NULL = sem limite superior
    private final BigDecimal feePercentage;
    private final BigDecimal fixedFee;
    private final LocalDate  validFrom;
    private       LocalDate  validUntil;      // NULL = vigência em aberto
    private final int        priority;
    private       boolean    active;
    private final Instant    createdAt;
    private       Instant    updatedAt;

    private MarketplaceFeeRule(UUID id, UUID marketplaceId, BigDecimal minPrice,
                               BigDecimal maxPrice, BigDecimal feePercentage,
                               BigDecimal fixedFee, LocalDate validFrom,
                               int priority, Instant createdAt) {
        this.id            = id;
        this.marketplaceId = marketplaceId;
        this.minPrice      = minPrice;
        this.maxPrice      = maxPrice;
        this.feePercentage = feePercentage;
        this.fixedFee      = fixedFee;
        this.validFrom     = validFrom;
        this.priority      = priority;
        this.active        = true;
        this.createdAt     = createdAt;
        this.updatedAt     = createdAt;
    }

    public static MarketplaceFeeRule create(UUID marketplaceId, BigDecimal minPrice,
                                            BigDecimal maxPrice, BigDecimal feePercentage,
                                            BigDecimal fixedFee, LocalDate validFrom,
                                            int priority) {
        Objects.requireNonNull(marketplaceId, "marketplaceId é obrigatório");
        Objects.requireNonNull(validFrom,     "validFrom é obrigatório");
        if (minPrice == null || minPrice.signum() < 0)       throw new IllegalArgumentException("minPrice deve ser >= 0");
        if (feePercentage == null || feePercentage.signum() < 0) throw new IllegalArgumentException("feePercentage deve ser >= 0");
        if (fixedFee == null || fixedFee.signum() < 0)       throw new IllegalArgumentException("fixedFee deve ser >= 0");
        if (maxPrice != null && maxPrice.compareTo(minPrice) < 0) throw new IllegalArgumentException("maxPrice deve ser >= minPrice");
        return new MarketplaceFeeRule(UUID.randomUUID(), marketplaceId, minPrice, maxPrice,
                                     feePercentage, fixedFee, validFrom, priority, Instant.now());
    }

    public static MarketplaceFeeRule reconstitute(UUID id, UUID marketplaceId,
                                                  BigDecimal minPrice, BigDecimal maxPrice,
                                                  BigDecimal feePercentage, BigDecimal fixedFee,
                                                  LocalDate validFrom, LocalDate validUntil,
                                                  int priority, boolean active,
                                                  Instant createdAt, Instant updatedAt) {
        MarketplaceFeeRule r = new MarketplaceFeeRule(id, marketplaceId, minPrice, maxPrice,
                                                      feePercentage, fixedFee, validFrom,
                                                      priority, createdAt);
        r.validUntil = validUntil;
        r.active     = active;
        r.updatedAt  = updatedAt;
        return r;
    }

    /**
     * Retorna true se o preço de venda está dentro da faixa desta regra.
     * minPrice <= price AND (maxPrice == null OR price <= maxPrice).
     */
    public boolean appliesTo(BigDecimal price) {
        Objects.requireNonNull(price, "price é obrigatório");
        if (price.compareTo(minPrice) < 0) return false;
        return maxPrice == null || price.compareTo(maxPrice) <= 0;
    }

    /**
     * Retorna true se a regra está vigente na data fornecida.
     */
    public boolean isActiveAt(LocalDate date) {
        Objects.requireNonNull(date, "date é obrigatório");
        if (validFrom.isAfter(date)) return false;
        return validUntil == null || !validUntil.isBefore(date);
    }

    public void deactivate() {
        this.active    = false;
        this.updatedAt = Instant.now();
    }

    public void closeAt(LocalDate until) {
        this.validUntil = until;
        this.active     = false;
        this.updatedAt  = Instant.now();
    }
}
