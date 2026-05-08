package br.com.valls.moneycontrol.infrastructure.persistence.repository;

import br.com.valls.moneycontrol.infrastructure.persistence.entity.RecurringCostDefinitionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecurringCostDefinitionJpaRepository
        extends JpaRepository<RecurringCostDefinitionJpaEntity, UUID> {

    List<RecurringCostDefinitionJpaEntity> findByCompanyIdAndActiveTrue(UUID companyId);

    List<RecurringCostDefinitionJpaEntity> findByCompanyId(UUID companyId);
}
