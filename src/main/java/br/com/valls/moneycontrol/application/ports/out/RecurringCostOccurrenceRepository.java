package br.com.valls.moneycontrol.application.ports.out;

import br.com.valls.moneycontrol.domain.model.RecurringCostOccurrence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de saída para persistência de ocorrências de custos recorrentes.
 */
public interface RecurringCostOccurrenceRepository {

    /** Persiste ou atualiza uma ocorrência e retorna a instância salva. */
    RecurringCostOccurrence save(RecurringCostOccurrence occurrence);

    /** Busca pelo ID. */
    Optional<RecurringCostOccurrence> findById(UUID id);

    /**
     * Verifica se já existe ocorrência para a definição no período informado.
     * Implementa a constraint UNIQUE(definition_id, reference_year, reference_month).
     */
    boolean existsByDefinitionAndPeriod(UUID definitionId, int year, int month);

    /**
     * Retorna todas as ocorrências de uma empresa em um período (mês/ano).
     * Usado para consolidar custos fixos de um período de apuração.
     */
    List<RecurringCostOccurrence> findByCompanyAndPeriod(UUID companyId, int year, int month);

    /** Retorna todas as ocorrências de uma definição. */
    List<RecurringCostOccurrence> findByDefinitionId(UUID definitionId);
}
