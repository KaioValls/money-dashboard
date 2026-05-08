package br.com.valls.moneycontrol.infrastructure.persistence.adapter;

import br.com.valls.moneycontrol.application.ports.out.StoreRepository;
import br.com.valls.moneycontrol.domain.enums.StoreStatus;
import br.com.valls.moneycontrol.domain.model.Store;
import br.com.valls.moneycontrol.infrastructure.persistence.entity.StoreJpaEntity;
import br.com.valls.moneycontrol.infrastructure.persistence.repository.StoreJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StoreRepositoryAdapter implements StoreRepository {

    private final StoreJpaRepository jpaRepository;

    @Override
    public Store save(Store store) {
        return toDomain(jpaRepository.save(toEntity(store)));
    }

    @Override
    public Optional<Store> findById(UUID id) {
        return jpaRepository.findById(id).map(StoreRepositoryAdapter::toDomain);
    }

    @Override
    public List<Store> findByCompanyId(UUID companyId, StoreStatus status) {
        var entities = status != null
                ? jpaRepository.findByCompanyIdAndStatus(companyId, status)
                : jpaRepository.findByCompanyId(companyId);
        return entities.stream().map(StoreRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Store> findByMarketplaceId(UUID marketplaceId) {
        return jpaRepository.findByMarketplaceId(marketplaceId).stream()
                .map(StoreRepositoryAdapter::toDomain).toList();
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    private static Store toDomain(StoreJpaEntity e) {
        return Store.reconstitute(e.getId(), e.getMarketplaceAccountId(),
                e.getCompanyId(), e.getMarketplaceId(), e.getName(),
                e.getStatus(), e.getAdsRatioMode(), e.isActive(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private static StoreJpaEntity toEntity(Store s) {
        return StoreJpaEntity.builder()
                .id(s.getId())
                .marketplaceAccountId(s.getMarketplaceAccountId())
                .companyId(s.getCompanyId())
                .marketplaceId(s.getMarketplaceId())
                .name(s.getName())
                .status(s.getStatus())
                .adsRatioMode(s.getAdsRatioMode())
                .active(s.isActive())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
