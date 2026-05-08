package br.com.valls.moneycontrol.application.ports.in;

import br.com.valls.moneycontrol.application.usecases.command.RegisterRecurringCostCommand;
import br.com.valls.moneycontrol.domain.model.RecurringCostDefinition;
import br.com.valls.moneycontrol.domain.model.RecurringCostOccurrence;

import java.util.List;
import java.util.UUID;

/**
 * Port de entrada para gerenciamento de custos recorrentes.
 */
public interface RecurringCostUseCase {

    /**
     * Cria uma nova definição de custo recorrente para a empresa.
     */
    RecurringCostDefinition createDefinition(RegisterRecurringCostCommand command);

    /**
     * Gera (idempotente) as ocorrências do mês/ano informados para todas as
     * definições ativas da empresa cuja frequência corresponda ao período.
     * Ocorrências já existentes são ignoradas (sem erro).
     *
     * @return lista das ocorrências efetivamente criadas (pode ser vazia)
     */
    List<RecurringCostOccurrence> generateOccurrencesForMonth(UUID companyId, int year, int month);

    /**
     * Marca uma ocorrência como SKIPPED com o motivo informado.
     * @throws IllegalStateException se já paga ou cancelada
     */
    RecurringCostOccurrence skipOccurrence(UUID occurrenceId, String reason);

    /**
     * Marca uma ocorrência como paga.
     * @throws IllegalStateException se já paga ou não está ACTIVE
     */
    RecurringCostOccurrence markAsPaid(UUID occurrenceId);

    /** Retorna todas as definições (ativas e inativas) de uma empresa. */
    List<RecurringCostDefinition> findByCompany(UUID companyId);

    /**
     * Desativa uma definição e fecha sua data de vigência para hoje.
     */
    RecurringCostDefinition deactivateDefinition(UUID id);
}
