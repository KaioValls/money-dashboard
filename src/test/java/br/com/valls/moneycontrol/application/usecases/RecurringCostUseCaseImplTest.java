package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.out.CompanyRepository;
import br.com.valls.moneycontrol.application.ports.out.RecurringCostDefinitionRepository;
import br.com.valls.moneycontrol.application.ports.out.RecurringCostOccurrenceRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterRecurringCostCommand;
import br.com.valls.moneycontrol.domain.enums.CostCategory;
import br.com.valls.moneycontrol.domain.enums.OccurrenceStatus;
import br.com.valls.moneycontrol.domain.enums.RecurringCostFrequency;
import br.com.valls.moneycontrol.domain.exception.CompanyNotFoundException;
import br.com.valls.moneycontrol.domain.model.Company;
import br.com.valls.moneycontrol.domain.model.RecurringCostDefinition;
import br.com.valls.moneycontrol.domain.model.RecurringCostOccurrence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecurringCostUseCaseImpl")
class RecurringCostUseCaseImplTest {

    @Mock RecurringCostDefinitionRepository definitionRepository;
    @Mock RecurringCostOccurrenceRepository occurrenceRepository;
    @Mock CompanyRepository                 companyRepository;

    RecurringCostUseCaseImpl useCase;

    private static final UUID COMPANY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new RecurringCostUseCaseImpl(
                definitionRepository, occurrenceRepository, companyRepository);
    }

    private Company stubCompany() {
        return Company.reconstitute(COMPANY_ID, "12345678000195", "Empresa Ltda",
                null, null, null, true, Instant.now(), Instant.now());
    }

    private RegisterRecurringCostCommand monthlyCmd() {
        return new RegisterRecurringCostCommand(COMPANY_ID, "Assinatura Software",
                new BigDecimal("500.00"), CostCategory.SOFTWARE,
                RecurringCostFrequency.MONTHLY, LocalDate.of(2025, 1, 1), null);
    }

    // ── createDefinition ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createDefinition persiste definição com dados corretos")
    void createDefinitionPersists() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(stubCompany()));
        when(definitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecurringCostDefinition result = useCase.createDefinition(monthlyCmd());

        assertThat(result.getCompanyId()).isEqualTo(COMPANY_ID);
        assertThat(result.getName()).isEqualTo("Assinatura Software");
        assertThat(result.getAmount()).isEqualByComparingTo("500.00");
        assertThat(result.getRecurrence()).isEqualTo(RecurringCostFrequency.MONTHLY);
        assertThat(result.isActive()).isTrue();
        verify(definitionRepository).save(any(RecurringCostDefinition.class));
    }

    @Test
    @DisplayName("createDefinition lança CompanyNotFoundException para empresa inexistente")
    void createDefinitionCompanyNotFound() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.createDefinition(monthlyCmd()))
                .isInstanceOf(CompanyNotFoundException.class);
    }

    // ── generateOccurrencesForMonth ───────────────────────────────────────────

    @Test
    @DisplayName("generateOccurrencesForMonth cria ocorrência para definição MONTHLY")
    void generateMonthly() {
        RecurringCostDefinition def = RecurringCostDefinition.create(
                COMPANY_ID, "Assinatura", new BigDecimal("100.00"),
                CostCategory.SOFTWARE, RecurringCostFrequency.MONTHLY,
                LocalDate.of(2025, 1, 1), null);
        when(definitionRepository.findActiveByCompanyId(COMPANY_ID)).thenReturn(List.of(def));
        when(occurrenceRepository.existsByDefinitionAndPeriod(def.getId(), 2025, 6)).thenReturn(false);
        when(occurrenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<RecurringCostOccurrence> created = useCase.generateOccurrencesForMonth(COMPANY_ID, 2025, 6);

        assertThat(created).hasSize(1);
        assertThat(created.get(0).getReferenceYear()).isEqualTo(2025);
        assertThat(created.get(0).getReferenceMonth()).isEqualTo(6);
        assertThat(created.get(0).getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("generateOccurrencesForMonth é idempotente — ignora ocorrência já existente")
    void generateIdempotent() {
        RecurringCostDefinition def = RecurringCostDefinition.create(
                COMPANY_ID, "Assinatura", new BigDecimal("100.00"),
                CostCategory.SOFTWARE, RecurringCostFrequency.MONTHLY,
                LocalDate.of(2025, 1, 1), null);
        when(definitionRepository.findActiveByCompanyId(COMPANY_ID)).thenReturn(List.of(def));
        when(occurrenceRepository.existsByDefinitionAndPeriod(def.getId(), 2025, 6)).thenReturn(true);

        List<RecurringCostOccurrence> created = useCase.generateOccurrencesForMonth(COMPANY_ID, 2025, 6);

        assertThat(created).isEmpty();
        verify(occurrenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("generateOccurrencesForMonth ignora definição QUARTERLY fora de ciclo")
    void generateQuarterlySkipsWrongMonth() {
        // Definição QUARTERLY com startDate = 2025-01-01: meses 1, 4, 7, 10
        RecurringCostDefinition def = RecurringCostDefinition.create(
                COMPANY_ID, "Custo Trimestral", new BigDecimal("300.00"),
                CostCategory.LOGISTICS, RecurringCostFrequency.QUARTERLY,
                LocalDate.of(2025, 1, 1), null);
        when(definitionRepository.findActiveByCompanyId(COMPANY_ID)).thenReturn(List.of(def));

        // Mês 2 não é mês de ocorrência para QUARTERLY com startDate em jan
        List<RecurringCostOccurrence> created = useCase.generateOccurrencesForMonth(COMPANY_ID, 2025, 2);

        assertThat(created).isEmpty();
        verify(occurrenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("generateOccurrencesForMonth cria ocorrência QUARTERLY no mês correto")
    void generateQuarterlyHitsCorrectMonth() {
        RecurringCostDefinition def = RecurringCostDefinition.create(
                COMPANY_ID, "Custo Trimestral", new BigDecimal("300.00"),
                CostCategory.LOGISTICS, RecurringCostFrequency.QUARTERLY,
                LocalDate.of(2025, 1, 1), null);
        when(definitionRepository.findActiveByCompanyId(COMPANY_ID)).thenReturn(List.of(def));
        when(occurrenceRepository.existsByDefinitionAndPeriod(def.getId(), 2025, 4)).thenReturn(false);
        when(occurrenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<RecurringCostOccurrence> created = useCase.generateOccurrencesForMonth(COMPANY_ID, 2025, 4);

        assertThat(created).hasSize(1);
    }

    // ── skipOccurrence ────────────────────────────────────────────────────────

    @Test
    @DisplayName("skipOccurrence muda status para SKIPPED com motivo")
    void skipOccurrenceChangesStatus() {
        UUID occId = UUID.randomUUID();
        RecurringCostOccurrence occ = RecurringCostOccurrence.create(
                UUID.randomUUID(), COMPANY_ID, 2025, 5, new BigDecimal("100.00"));
        when(occurrenceRepository.findById(occId)).thenReturn(Optional.of(occ));
        when(occurrenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecurringCostOccurrence result = useCase.skipOccurrence(occId, "Férias coletivas");

        assertThat(result.getStatus()).isEqualTo(OccurrenceStatus.SKIPPED);
        assertThat(result.getSkipReason()).isEqualTo("Férias coletivas");
    }

    // ── markAsPaid ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("markAsPaid marca ocorrência como paga")
    void markAsPaidSetsFlag() {
        UUID occId = UUID.randomUUID();
        RecurringCostOccurrence occ = RecurringCostOccurrence.create(
                UUID.randomUUID(), COMPANY_ID, 2025, 5, new BigDecimal("100.00"));
        when(occurrenceRepository.findById(occId)).thenReturn(Optional.of(occ));
        when(occurrenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecurringCostOccurrence result = useCase.markAsPaid(occId);

        assertThat(result.isPaid()).isTrue();
        assertThat(result.getPaidAt()).isNotNull();
    }

    // ── deactivateDefinition ──────────────────────────────────────────────────

    @Test
    @DisplayName("deactivateDefinition desativa definição existente")
    void deactivateDefinitionSetsActiveToFalse() {
        UUID defId = UUID.randomUUID();
        RecurringCostDefinition def = RecurringCostDefinition.create(
                COMPANY_ID, "Custo X", new BigDecimal("200.00"),
                CostCategory.OTHER, RecurringCostFrequency.MONTHLY,
                LocalDate.of(2025, 1, 1), null);
        when(definitionRepository.findById(defId)).thenReturn(Optional.of(def));
        when(definitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecurringCostDefinition result = useCase.deactivateDefinition(defId);

        assertThat(result.isActive()).isFalse();
        assertThat(result.getEndDate()).isNotNull();
    }
}
