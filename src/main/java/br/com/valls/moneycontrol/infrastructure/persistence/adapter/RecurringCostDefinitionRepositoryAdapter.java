package br.com.valls.moneycontrol.infrastructure.persistence.adapter;

import br.com.valls.moneycontrol.application.ports.out.RecurringCostDefinitionRepository;
import br.com.valls.moneycontrol.domain.model.RecurringCostDefinition;
import br.com.valls.moneycontrol.infrastructure.persistence.entity.RecurringCostDefinitionJpaEntity;
import br.com.valls.moneycontrol.infrastructure.persistence.repository.RecurringCostDefinitionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RecurringCostDefinitionRepositoryAdapter implements RecurringCostDefinitionRepository {

    private final RecurringCostDefinitionJpaRepository jpaRepository;

    @Override
    public RecurringCostDefinition save(RecurringCostDefinition definition) {
        return toDomain(jpaRepository.save(toEntity(definition)));
    }

    @Override
    public Optional<RecurringCostDefinition> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(RecurringCostDefinitionRepositoryAdapter::toDomain);
    }

    @Override
    public List<RecurringCostDefinition> findActiveByCompanyId(UUID companyId) {
        return jpaRepository.findByCompanyIdAndActiveTrue(companyId).stream()
                .map(RecurringCostDefinitionRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<RecurringCostDefinition> findAllByCompanyId(UUID companyId) {
        return jpaRepository.findByCompanyId(companyId).stream()
                .map(RecurringCostDefinitionRepositoryAdapter::toDomain).toList();
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    private static RecurringCostDefinition toDomain(RecurringCostDefinitionJpaEntity e) {
        return RecurringCostDefinition.reconstitute(e.getId(), e.getCompanyId(), e.getName(),
                e.getAmount(), e.getCostCategory(), e.getRecurrence(),
                e.getStartDate(), e.getEndDate(), e.isActive(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private static RecurringCostDefinitionJpaEntity toEntity(RecurringCostDefinition d) {
        return RecurringCostDefinitionJpaEntity.builder()
                .id(d.getId())
                .companyId(d.getCompanyId())
                .name(d.getName())
                .amount(d.getAmount())
                .costCategory(d.getCostCategory())
                .recurrence(d.getRecurrence())
                .startDate(d.getStartDate())
                .endDate(d.getEndDate())
                .active(d.isActive())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
