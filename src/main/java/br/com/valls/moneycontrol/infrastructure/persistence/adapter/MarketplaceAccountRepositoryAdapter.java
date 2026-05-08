package br.com.valls.moneycontrol.infrastructure.persistence.adapter;

import br.com.valls.moneycontrol.application.ports.out.MarketplaceAccountRepository;
import br.com.valls.moneycontrol.domain.model.MarketplaceAccount;
import br.com.valls.moneycontrol.infrastructure.persistence.entity.MarketplaceAccountJpaEntity;
import br.com.valls.moneycontrol.infrastructure.persistence.repository.MarketplaceAccountJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MarketplaceAccountRepositoryAdapter implements MarketplaceAccountRepository {

    private final MarketplaceAccountJpaRepository jpaRepository;

    @Override
    public MarketplaceAccount save(MarketplaceAccount account) {
        return toDomain(jpaRepository.save(toEntity(account)));
    }

    @Override
    public Optional<MarketplaceAccount> findById(UUID id) {
        return jpaRepository.findById(id).map(MarketplaceAccountRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<MarketplaceAccount> findByCompanyAndMarketplace(UUID companyId, UUID marketplaceId) {
        return jpaRepository.findByCompanyIdAndMarketplaceId(companyId, marketplaceId)
                .map(MarketplaceAccountRepositoryAdapter::toDomain);
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    private static MarketplaceAccount toDomain(MarketplaceAccountJpaEntity e) {
        return MarketplaceAccount.reconstitute(e.getId(), e.getCompanyId(),
                e.getMarketplaceId(), e.getSellerIdExternal(),
                e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static MarketplaceAccountJpaEntity toEntity(MarketplaceAccount a) {
        return MarketplaceAccountJpaEntity.builder()
                .id(a.getId())
                .companyId(a.getCompanyId())
                .marketplaceId(a.getMarketplaceId())
                .sellerIdExternal(a.getSellerIdExternal())
                .active(a.isActive())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
