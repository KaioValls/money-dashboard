package br.com.valls.moneycontrol.infrastructure.persistence.repository;

import br.com.valls.moneycontrol.infrastructure.persistence.entity.RecurringCostOccurrenceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecurringCostOccurrenceJpaRepository
        extends JpaRepository<RecurringCostOccurrenceJpaEntity, UUID> {

    boolean existsByDefinitionIdAndReferenceYearAndReferenceMonth(
            UUID definitionId, int referenceYear, int referenceMonth);

    List<RecurringCostOccurrenceJpaEntity> findByCompanyIdAndReferenceYearAndReferenceMonth(
            UUID companyId, int referenceYear, int referenceMonth);

    List<RecurringCostOccurrenceJpaEntity> findByDefinitionId(UUID definitionId);
}
