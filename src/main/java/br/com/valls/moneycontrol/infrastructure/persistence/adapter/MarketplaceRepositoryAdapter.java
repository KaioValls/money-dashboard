package br.com.valls.moneycontrol.infrastructure.persistence.adapter;

import br.com.valls.moneycontrol.application.ports.out.MarketplaceRepository;
import br.com.valls.moneycontrol.domain.model.Marketplace;
import br.com.valls.moneycontrol.infrastructure.persistence.entity.MarketplaceJpaEntity;
import br.com.valls.moneycontrol.infrastructure.persistence.repository.MarketplaceJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MarketplaceRepositoryAdapter implements MarketplaceRepository {

    private final MarketplaceJpaRepository jpaRepository;

    @Override
    public Marketplace save(Marketplace marketplace) {
        return toDomain(jpaRepository.save(toEntity(marketplace)));
    }

    @Override
    public Optional<Marketplace> findById(UUID id) {
        return jpaRepository.findById(id).map(MarketplaceRepositoryAdapter::toDomain);
    }

    @Override
    public List<Marketplace> findAll() {
        return jpaRepository.findAll().stream()
                .map(MarketplaceRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<Marketplace> findBySlug(String slug) {
        return jpaRepository.findBySlug(slug).map(MarketplaceRepositoryAdapter::toDomain);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return jpaRepository.existsBySlug(slug);
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    private static Marketplace toDomain(MarketplaceJpaEntity e) {
        return Marketplace.reconstitute(e.getId(), e.getName(), e.getSlug(),
                e.getLogoUrl(), e.getWebsiteUrl(), e.getPaymentCycleDays(),
                e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static MarketplaceJpaEntity toEntity(Marketplace m) {
        return MarketplaceJpaEntity.builder()
                .id(m.getId())
                .name(m.getName())
                .slug(m.getSlug())
                .logoUrl(m.getLogoUrl())
                .websiteUrl(m.getWebsiteUrl())
                .paymentCycleDays(m.getPaymentCycleDays())
                .active(m.isActive())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
