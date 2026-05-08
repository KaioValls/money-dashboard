package br.com.valls.moneycontrol.infrastructure.persistence.adapter;

import br.com.valls.moneycontrol.application.ports.out.RecurringCostOccurrenceRepository;
import br.com.valls.moneycontrol.domain.model.RecurringCostOccurrence;
import br.com.valls.moneycontrol.infrastructure.persistence.entity.RecurringCostOccurrenceJpaEntity;
import br.com.valls.moneycontrol.infrastructure.persistence.repository.RecurringCostOccurrenceJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RecurringCostOccurrenceRepositoryAdapter implements RecurringCostOccurrenceRepository {

    private final RecurringCostOccurrenceJpaRepository jpaRepository;

    @Override
    public RecurringCostOccurrence save(RecurringCostOccurrence occurrence) {
        return toDomain(jpaRepository.save(toEntity(occurrence)));
    }

    @Override
    public Optional<RecurringCostOccurrence> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(RecurringCostOccurrenceRepositoryAdapter::toDomain);
    }

    @Override
    public boolean existsByDefinitionAndPeriod(UUID definitionId, int year, int month) {
        return jpaRepository.existsByDefinitionIdAndReferenceYearAndReferenceMonth(
                definitionId, year, month);
    }

    @Override
    public List<RecurringCostOccurrence> findByCompanyAndPeriod(UUID companyId, int year, int month) {
        return jpaRepository.findByCompanyIdAndReferenceYearAndReferenceMonth(companyId, year, month)
                .stream().map(RecurringCostOccurrenceRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<RecurringCostOccurrence> findByDefinitionId(UUID definitionId) {
        return jpaRepository.findByDefinitionId(definitionId).stream()
                .map(RecurringCostOccurrenceRepositoryAdapter::toDomain).toList();
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    private static RecurringCostOccurrence toDomain(RecurringCostOccurrenceJpaEntity e) {
        return RecurringCostOccurrence.reconstitute(e.getId(), e.getDefinitionId(),
                e.getCompanyId(), e.getReferenceYear(), e.getReferenceMonth(),
                e.getAmount(), e.getStatus(), e.isPaid(), e.getPaidAt(),
                e.getSkipReason(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static RecurringCostOccurrenceJpaEntity toEntity(RecurringCostOccurrence o) {
        return RecurringCostOccurrenceJpaEntity.builder()
                .id(o.getId())
                .definitionId(o.getDefinitionId())
                .companyId(o.getCompanyId())
                .referenceYear(o.getReferenceYear())
                .referenceMonth(o.getReferenceMonth())
                .amount(o.getAmount())
                .status(o.getStatus())
                .paid(o.isPaid())
                .paidAt(o.getPaidAt())
                .skipReason(o.getSkipReason())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
