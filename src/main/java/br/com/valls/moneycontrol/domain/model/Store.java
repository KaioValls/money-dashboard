package br.com.valls.moneycontrol.domain.model;

import br.com.valls.moneycontrol.domain.enums.AdsRatioMode;
import br.com.valls.moneycontrol.domain.enums.StoreStatus;
import br.com.valls.moneycontrol.domain.exception.BusinessException;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
public class Store {

    private final UUID         id;
    private final UUID         marketplaceAccountId;
    private final UUID         companyId;
    private final UUID         marketplaceId;
    private       String       name;
    private       StoreStatus  status;
    private       AdsRatioMode adsRatioMode;
    private       boolean      active;
    private final Instant      createdAt;
    private       Instant      updatedAt;

    private Store(UUID id, UUID marketplaceAccountId, UUID companyId,
                  UUID marketplaceId, String name, Instant createdAt) {
        this.id                   = id;
        this.marketplaceAccountId = marketplaceAccountId;
        this.companyId            = companyId;
        this.marketplaceId        = marketplaceId;
        this.name                 = name;
        this.status               = StoreStatus.ACTIVE;
        this.adsRatioMode         = AdsRatioMode.DIRECT;
        this.active               = true;
        this.createdAt            = createdAt;
        this.updatedAt            = createdAt;
    }

    public static Store create(UUID marketplaceAccountId, UUID companyId,
                               UUID marketplaceId, String name) {
        Objects.requireNonNull(marketplaceAccountId, "marketplaceAccountId é obrigatório");
        Objects.requireNonNull(companyId,            "companyId é obrigatório");
        Objects.requireNonNull(marketplaceId,        "marketplaceId é obrigatório");
        if (name == null || name.isBlank())          throw new IllegalArgumentException("Nome da loja não pode ser vazio");
        return new Store(UUID.randomUUID(), marketplaceAccountId, companyId, marketplaceId, name, Instant.now());
    }

    public static Store reconstitute(UUID id, UUID marketplaceAccountId, UUID companyId,
                                     UUID marketplaceId, String name, StoreStatus status,
                                     AdsRatioMode adsRatioMode, boolean active,
                                     Instant createdAt, Instant updatedAt) {
        Store s       = new Store(id, marketplaceAccountId, companyId, marketplaceId, name, createdAt);
        s.status      = status;
        s.adsRatioMode = adsRatioMode;
        s.active      = active;
        s.updatedAt   = updatedAt;
        return s;
    }

    /** ACTIVE → PAUSED. Lança BusinessException se CLOSED ou já PAUSED. */
    public void pause() {
        if (status == StoreStatus.CLOSED) throw new StoreTransitionException("Loja fechada não pode ser pausada");
        if (status == StoreStatus.PAUSED) throw new StoreTransitionException("Loja já está pausada");
        this.status    = StoreStatus.PAUSED;
        this.updatedAt = Instant.now();
    }

    /** PAUSED → ACTIVE. Lança BusinessException se CLOSED ou já ACTIVE. */
    public void reactivate() {
        if (status == StoreStatus.CLOSED) throw new StoreTransitionException("Loja fechada não pode ser reativada");
        if (status == StoreStatus.ACTIVE) throw new StoreTransitionException("Loja já está ativa");
        this.status    = StoreStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    /** ACTIVE/PAUSED → CLOSED. Lança BusinessException se já CLOSED. */
    public void close() {
        if (status == StoreStatus.CLOSED) throw new StoreTransitionException("Loja já está fechada");
        this.status    = StoreStatus.CLOSED;
        this.active    = false;
        this.updatedAt = Instant.now();
    }

    public void updateAdsRatioMode(AdsRatioMode mode) {
        Objects.requireNonNull(mode, "AdsRatioMode é obrigatório");
        this.adsRatioMode = mode;
        this.updatedAt    = Instant.now();
    }

    public void updateName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Nome não pode ser vazio");
        this.name      = name;
        this.updatedAt = Instant.now();
    }

    // ── exceção de transição de status (subtipo de BusinessException) ─────────
    public static class StoreTransitionException extends BusinessException {
        public StoreTransitionException(String message) { super(message); }
    }
}
