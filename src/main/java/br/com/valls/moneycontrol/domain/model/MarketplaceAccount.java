package br.com.valls.moneycontrol.domain.model;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
public class MarketplaceAccount {

    private final UUID    id;
    private final UUID    companyId;
    private final UUID    marketplaceId;
    private       String  sellerIdExternal;
    private       boolean active;
    private final Instant createdAt;
    private       Instant updatedAt;

    private MarketplaceAccount(UUID id, UUID companyId, UUID marketplaceId, Instant createdAt) {
        this.id            = id;
        this.companyId     = companyId;
        this.marketplaceId = marketplaceId;
        this.active        = true;
        this.createdAt     = createdAt;
        this.updatedAt     = createdAt;
    }

    public static MarketplaceAccount create(UUID companyId, UUID marketplaceId) {
        Objects.requireNonNull(companyId,     "companyId é obrigatório");
        Objects.requireNonNull(marketplaceId, "marketplaceId é obrigatório");
        return new MarketplaceAccount(UUID.randomUUID(), companyId, marketplaceId, Instant.now());
    }

    public static MarketplaceAccount reconstitute(UUID id, UUID companyId, UUID marketplaceId,
                                                  String sellerIdExternal, boolean active,
                                                  Instant createdAt, Instant updatedAt) {
        MarketplaceAccount a = new MarketplaceAccount(id, companyId, marketplaceId, createdAt);
        a.sellerIdExternal   = sellerIdExternal;
        a.active             = active;
        a.updatedAt          = updatedAt;
        return a;
    }

    public void linkSellerId(String sellerIdExternal) {
        this.sellerIdExternal = sellerIdExternal;
        this.updatedAt        = Instant.now();
    }

    public void deactivate() { this.active = false; this.updatedAt = Instant.now(); }
}
