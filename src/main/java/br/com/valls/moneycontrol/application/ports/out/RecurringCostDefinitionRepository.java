package br.com.valls.moneycontrol.application.ports.out;

import br.com.valls.moneycontrol.domain.model.RecurringCostDefinition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de saída para persistência de definições de custo recorrente.
 */
public interface RecurringCostDefinitionRepository {

    /** Persiste ou atualiza uma definição e retorna a instância salva. */
    RecurringCostDefinition save(RecurringCostDefinition definition);

    /** Busca pelo ID. */
    Optional<RecurringCostDefinition> findById(UUID id);

    /** Retorna todas as definições ativas de uma empresa. */
    List<RecurringCostDefinition> findActiveByCompanyId(UUID companyId);

    /** Retorna todas as definições (ativas e inativas) de uma empresa. */
    List<RecurringCostDefinition> findAllByCompanyId(UUID companyId);
}
