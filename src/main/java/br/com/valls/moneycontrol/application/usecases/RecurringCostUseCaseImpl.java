package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.in.RecurringCostUseCase;
import br.com.valls.moneycontrol.application.ports.out.CompanyRepository;
import br.com.valls.moneycontrol.application.ports.out.RecurringCostDefinitionRepository;
import br.com.valls.moneycontrol.application.ports.out.RecurringCostOccurrenceRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterRecurringCostCommand;
import br.com.valls.moneycontrol.domain.exception.CompanyNotFoundException;
import br.com.valls.moneycontrol.domain.exception.EntityNotFoundException;
import br.com.valls.moneycontrol.domain.model.RecurringCostDefinition;
import br.com.valls.moneycontrol.domain.model.RecurringCostOccurrence;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementação do use case de custos recorrentes.
 * Instanciada exclusivamente em {@code BeanConfig} — sem @Service.
 *
 * <p>O método {@link #generateOccurrencesForMonth} é idempotente:
 * ocorrências já existentes são silenciosamente ignoradas.
 */
@RequiredArgsConstructor
public class RecurringCostUseCaseImpl implements RecurringCostUseCase {

    private final RecurringCostDefinitionRepository  definitionRepository;
    private final RecurringCostOccurrenceRepository  occurrenceRepository;
    private final CompanyRepository                  companyRepository;

    @Override
    public RecurringCostDefinition createDefinition(RegisterRecurringCostCommand command) {
        // Valida empresa
        companyRepository.findById(command.companyId())
                .orElseThrow(() -> new CompanyNotFoundException(command.companyId()));

        RecurringCostDefinition definition = RecurringCostDefinition.create(
                command.companyId(),
                command.name(),
                command.amount(),
                command.costCategory(),
                command.recurrence(),
                command.startDate(),
                command.endDate()
        );
        return definitionRepository.save(definition);
    }

    @Override
    public List<RecurringCostOccurrence> generateOccurrencesForMonth(UUID companyId,
                                                                      int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month deve estar entre 1 e 12: " + month);
        }

        List<RecurringCostDefinition> definitions =
                definitionRepository.findActiveByCompanyId(companyId);

        List<RecurringCostOccurrence> created = new ArrayList<>();
        for (RecurringCostDefinition def : definitions) {
            if (!def.shouldGenerateFor(year, month)) continue;
            if (occurrenceRepository.existsByDefinitionAndPeriod(def.getId(), year, month)) continue;

            RecurringCostOccurrence occ = RecurringCostOccurrence.create(
                    def.getId(), companyId, year, month, def.getAmount());
            created.add(occurrenceRepository.save(occ));
        }
        return List.copyOf(created);
    }

    @Override
    public RecurringCostOccurrence skipOccurrence(UUID occurrenceId, String reason) {
        RecurringCostOccurrence occ = occurrenceRepository.findById(occurrenceId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "RecurringCostOccurrence não encontrada: " + occurrenceId) {});
        occ.skip(reason);
        return occurrenceRepository.save(occ);
    }

    @Override
    public RecurringCostOccurrence markAsPaid(UUID occurrenceId) {
        RecurringCostOccurrence occ = occurrenceRepository.findById(occurrenceId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "RecurringCostOccurrence não encontrada: " + occurrenceId) {});
        occ.markAsPaid();
        return occurrenceRepository.save(occ);
    }

    @Override
    public List<RecurringCostDefinition> findByCompany(UUID companyId) {
        return List.copyOf(definitionRepository.findAllByCompanyId(companyId));
    }

    @Override
    public RecurringCostDefinition deactivateDefinition(UUID id) {
        RecurringCostDefinition definition = definitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "RecurringCostDefinition não encontrada: " + id) {});
        definition.deactivate(LocalDate.now());
        return definitionRepository.save(definition);
    }
}
