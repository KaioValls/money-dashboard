package br.com.valls.moneycontrol.infrastructure.persistence.adapter;

import br.com.valls.moneycontrol.application.ports.out.TaxConfigRepository;
import br.com.valls.moneycontrol.domain.model.TaxConfig;
import br.com.valls.moneycontrol.infrastructure.persistence.entity.TaxConfigJpaEntity;
import br.com.valls.moneycontrol.infrastructure.persistence.repository.TaxConfigJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TaxConfigRepositoryAdapter implements TaxConfigRepository {

    private final TaxConfigJpaRepository jpaRepository;

    @Override
    public TaxConfig save(TaxConfig taxConfig) {
        return toDomain(jpaRepository.save(toEntity(taxConfig)));
    }

    @Override
    public Optional<TaxConfig> findById(UUID id) {
        return jpaRepository.findById(id).map(TaxConfigRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<TaxConfig> findActiveAt(UUID companyId, LocalDate date) {
        return jpaRepository.findActiveAt(companyId, date)
                .map(TaxConfigRepositoryAdapter::toDomain);
    }

    @Override
    public List<TaxConfig> findByCompanyId(UUID companyId) {
        return jpaRepository.findByCompanyId(companyId).stream()
                .map(TaxConfigRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<TaxConfig> findOpenByCompanyId(UUID companyId) {
        return jpaRepository.findOpenByCompanyId(companyId).stream()
                .map(TaxConfigRepositoryAdapter::toDomain)
                .findFirst();
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    private static TaxConfig toDomain(TaxConfigJpaEntity e) {
        return TaxConfig.reconstitute(e.getId(), e.getCompanyId(),
                e.getChargeMoment(), e.getCalculationBase(), e.getRatePercentage(),
                e.getValidFrom(), e.getValidUntil(), e.isActive(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private static TaxConfigJpaEntity toEntity(TaxConfig t) {
        return TaxConfigJpaEntity.builder()
                .id(t.getId())
                .companyId(t.getCompanyId())
                .chargeMoment(t.getChargeMoment())
                .calculationBase(t.getCalculationBase())
                .ratePercentage(t.getRatePercentage())
                .validFrom(t.getValidFrom())
                .validUntil(t.getValidUntil())
                .active(t.isActive())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
