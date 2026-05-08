package br.com.valls.moneycontrol.infrastructure.persistence.adapter;

import br.com.valls.moneycontrol.application.ports.out.ListingRepository;
import br.com.valls.moneycontrol.domain.model.Listing;
import br.com.valls.moneycontrol.domain.model.ListingPriceHistory;
import br.com.valls.moneycontrol.infrastructure.persistence.entity.ListingJpaEntity;
import br.com.valls.moneycontrol.infrastructure.persistence.entity.ListingPriceHistoryJpaEntity;
import br.com.valls.moneycontrol.infrastructure.persistence.repository.ListingJpaRepository;
import br.com.valls.moneycontrol.infrastructure.persistence.repository.ListingPriceHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ListingRepositoryAdapter implements ListingRepository {

    private final ListingJpaRepository             jpaRepository;
    private final ListingPriceHistoryJpaRepository historyJpaRepository;

    @Override
    public Listing save(Listing listing) {
        return toDomain(jpaRepository.save(toEntity(listing)));
    }

    @Override
    public Optional<Listing> findById(UUID id) {
        return jpaRepository.findById(id).map(ListingRepositoryAdapter::toDomain);
    }

    @Override
    public List<Listing> findByStoreId(UUID storeId) {
        return jpaRepository.findByStoreId(storeId).stream()
                .map(ListingRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Listing> findByProductId(UUID productId) {
        return jpaRepository.findByProductId(productId).stream()
                .map(ListingRepositoryAdapter::toDomain).toList();
    }

    @Override
    public ListingPriceHistory savePriceHistory(ListingPriceHistory history) {
        return toDomain(historyJpaRepository.save(toEntity(history)));
    }

    @Override
    public List<ListingPriceHistory> findPriceHistoryByListingId(UUID listingId) {
        return historyJpaRepository.findByListingId(listingId).stream()
                .map(ListingRepositoryAdapter::toDomain).toList();
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    private static Listing toDomain(ListingJpaEntity e) {
        return Listing.reconstitute(e.getId(), e.getProductId(), e.getStoreId(),
                e.getMarketplaceId(), e.getCompanyId(), e.getExternalListingId(),
                e.getTitle(), e.getSalePrice(), e.getListingType(), e.getStatus(),
                e.getPublishedAt(), e.getClosedAt(), e.isActive(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private static ListingJpaEntity toEntity(Listing l) {
        return ListingJpaEntity.builder()
                .id(l.getId())
                .productId(l.getProductId())
                .storeId(l.getStoreId())
                .marketplaceId(l.getMarketplaceId())
                .companyId(l.getCompanyId())
                .externalListingId(l.getExternalListingId())
                .title(l.getTitle())
                .salePrice(l.getSalePrice())
                .listingType(l.getListingType())
                .status(l.getStatus())
                .publishedAt(l.getPublishedAt())
                .closedAt(l.getClosedAt())
                .active(l.isActive())
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }

    private static ListingPriceHistory toDomain(ListingPriceHistoryJpaEntity e) {
        return ListingPriceHistory.reconstitute(e.getId(), e.getListingId(),
                e.getSalePrice(), e.getValidFrom(), e.getValidUntil(),
                e.getChangedBy(), e.getChangeReason());
    }

    private static ListingPriceHistoryJpaEntity toEntity(ListingPriceHistory h) {
        return ListingPriceHistoryJpaEntity.builder()
                .id(h.getId())
                .listingId(h.getListingId())
                .salePrice(h.getSalePrice())
                .validFrom(h.getValidFrom())
                .validUntil(h.getValidUntil())
                .changedBy(h.getChangedBy())
                .changeReason(h.getChangeReason())
                .createdAt(h.getCreatedAt())
                .build();
    }
}
