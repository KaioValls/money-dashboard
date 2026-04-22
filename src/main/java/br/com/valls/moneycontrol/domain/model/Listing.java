package br.com.valls.moneycontrol.domain.model;

import br.com.valls.moneycontrol.domain.enums.ListingStatus;
import br.com.valls.moneycontrol.domain.exception.BusinessException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
public class Listing {

    private final UUID          id;
    private final UUID          productId;
    private final UUID          storeId;
    private final UUID          marketplaceId;
    private final UUID          companyId;
    private       String        externalListingId;
    private       String        title;
    private       BigDecimal    salePrice;
    private       String        listingType;
    private       ListingStatus status;
    private       Instant       publishedAt;
    private       Instant       closedAt;
    private       boolean       active;
    private final Instant       createdAt;
    private       Instant       updatedAt;

    private Listing(UUID id, UUID productId, UUID storeId, UUID marketplaceId,
                    UUID companyId, String title, BigDecimal salePrice, Instant createdAt) {
        this.id            = id;
        this.productId     = productId;
        this.storeId       = storeId;
        this.marketplaceId = marketplaceId;
        this.companyId     = companyId;
        this.title         = title;
        this.salePrice     = salePrice;
        this.status        = ListingStatus.ACTIVE;
        this.active        = true;
        this.createdAt     = createdAt;
        this.updatedAt     = createdAt;
    }

    public static Listing create(UUID productId, UUID storeId, UUID marketplaceId,
                                 UUID companyId, String title, BigDecimal salePrice) {
        Objects.requireNonNull(productId,     "productId é obrigatório");
        Objects.requireNonNull(storeId,       "storeId é obrigatório");
        Objects.requireNonNull(marketplaceId, "marketplaceId é obrigatório");
        Objects.requireNonNull(companyId,     "companyId é obrigatório");
        if (title == null || title.isBlank())              throw new IllegalArgumentException("Título não pode ser vazio");
        if (salePrice == null || salePrice.signum() <= 0)  throw new IllegalArgumentException("Preço deve ser positivo");
        return new Listing(UUID.randomUUID(), productId, storeId, marketplaceId, companyId, title, salePrice, Instant.now());
    }

    public static Listing reconstitute(UUID id, UUID productId, UUID storeId,
                                       UUID marketplaceId, UUID companyId,
                                       String externalListingId, String title,
                                       BigDecimal salePrice, String listingType,
                                       ListingStatus status, Instant publishedAt,
                                       Instant closedAt, boolean active,
                                       Instant createdAt, Instant updatedAt) {
        Listing l          = new Listing(id, productId, storeId, marketplaceId, companyId, title, salePrice, createdAt);
        l.externalListingId = externalListingId;
        l.listingType      = listingType;
        l.status           = status;
        l.publishedAt      = publishedAt;
        l.closedAt         = closedAt;
        l.active           = active;
        l.updatedAt        = updatedAt;
        return l;
    }

    /**
     * Atualiza o preço do anúncio e retorna o novo registro de histórico.
     * O use case é responsável por fechar (valid_until) o registro anterior.
     */
    public ListingPriceHistory updatePrice(BigDecimal newPrice, UUID changedBy, String reason) {
        if (newPrice == null || newPrice.signum() <= 0) {
            throw new IllegalArgumentException("Novo preço deve ser positivo");
        }
        this.salePrice = newPrice;
        this.updatedAt = Instant.now();
        return ListingPriceHistory.create(this.id, newPrice, this.updatedAt, changedBy, reason);
    }

    public void pause() {
        if (status == ListingStatus.CLOSED) throw new ListingTransitionException("Anúncio fechado não pode ser pausado");
        if (status == ListingStatus.PAUSED) throw new ListingTransitionException("Anúncio já está pausado");
        this.status    = ListingStatus.PAUSED;
        this.updatedAt = Instant.now();
    }

    public void reactivate() {
        if (status == ListingStatus.CLOSED) throw new ListingTransitionException("Anúncio fechado não pode ser reativado");
        if (status == ListingStatus.ACTIVE) throw new ListingTransitionException("Anúncio já está ativo");
        this.status    = ListingStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void close() {
        if (status == ListingStatus.CLOSED) throw new ListingTransitionException("Anúncio já está fechado");
        this.status    = ListingStatus.CLOSED;
        this.closedAt  = Instant.now();
        this.active    = false;
        this.updatedAt = Instant.now();
    }

    public void publish() {
        this.publishedAt = Instant.now();
        this.updatedAt   = Instant.now();
    }

    public static class ListingTransitionException extends BusinessException {
        public ListingTransitionException(String message) { super(message); }
    }
}
