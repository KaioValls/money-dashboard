package br.com.valls.moneycontrol.infrastructure.persistence.adapter;

import br.com.valls.moneycontrol.application.ports.out.MarketplaceFeeRuleRepository;
import br.com.valls.moneycontrol.domain.model.MarketplaceFeeRule;
import br.com.valls.moneycontrol.infrastructure.persistence.entity.MarketplaceFeeRuleJpaEntity;
import br.com.valls.moneycontrol.infrastructure.persistence.repository.MarketplaceFeeRuleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MarketplaceFeeRuleRepositoryAdapter implements MarketplaceFeeRuleRepository {

    private final MarketplaceFeeRuleJpaRepository jpaRepository;

    @Override
    public MarketplaceFeeRule save(MarketplaceFeeRule rule) {
        return toDomain(jpaRepository.save(toEntity(rule)));
    }

    @Override
    public Optional<MarketplaceFeeRule> findById(UUID id) {
        return jpaRepository.findById(id).map(MarketplaceFeeRuleRepositoryAdapter::toDomain);
    }

    @Override
    public List<MarketplaceFeeRule> findAllByMarketplaceId(UUID marketplaceId) {
        return jpaRepository.findByMarketplaceId(marketplaceId).stream()
                .map(MarketplaceFeeRuleRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<MarketplaceFeeRule> findActiveAt(UUID marketplaceId, LocalDate date) {
        return jpaRepository.findActiveAt(marketplaceId, date).stream()
                .map(MarketplaceFeeRuleRepositoryAdapter::toDomain).toList();
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    private static MarketplaceFeeRule toDomain(MarketplaceFeeRuleJpaEntity e) {
        return MarketplaceFeeRule.reconstitute(e.getId(), e.getMarketplaceId(),
                e.getMinPrice(), e.getMaxPrice(), e.getFeePercentage(), e.getFixedFee(),
                e.getValidFrom(), e.getValidUntil(), e.getPriority(), e.isActive(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private static MarketplaceFeeRuleJpaEntity toEntity(MarketplaceFeeRule r) {
        return MarketplaceFeeRuleJpaEntity.builder()
                .id(r.getId())
                .marketplaceId(r.getMarketplaceId())
                .minPrice(r.getMinPrice())
                .maxPrice(r.getMaxPrice())
                .feePercentage(r.getFeePercentage())
                .fixedFee(r.getFixedFee())
                .validFrom(r.getValidFrom())
                .validUntil(r.getValidUntil())
                .priority(r.getPriority())
                .active(r.isActive())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
