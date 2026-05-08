package br.com.valls.moneycontrol.domain.model;

import br.com.valls.moneycontrol.domain.enums.CostCategory;
import br.com.valls.moneycontrol.domain.enums.RecurringCostFrequency;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Definição de custo recorrente por empresa.
 * Cada definição gera {@link RecurringCostOccurrence} mensais/trimestrais/anuais
 * dentro do intervalo [startDate, endDate] via use case.
 */
@Getter
public class RecurringCostDefinition {

    private final UUID                   id;
    private final UUID                   companyId;
    private final String                 name;
    private       BigDecimal             amount;
    private final CostCategory           costCategory;
    private final RecurringCostFrequency recurrence;
    private final LocalDate              startDate;
    private       LocalDate              endDate;      // null = sem data de encerramento
    private       boolean                active;
    private final Instant                createdAt;
    private       Instant                updatedAt;

    private RecurringCostDefinition(UUID id, UUID companyId, String name,
                                    BigDecimal amount, CostCategory costCategory,
                                    RecurringCostFrequency recurrence,
                                    LocalDate startDate, LocalDate endDate,
                                    boolean active, Instant createdAt) {
        this.id           = id;
        this.companyId    = companyId;
        this.name         = name;
        this.amount       = amount;
        this.costCategory = costCategory;
        this.recurrence   = recurrence;
        this.startDate    = startDate;
        this.endDate      = endDate;
        this.active       = active;
        this.createdAt    = createdAt;
        this.updatedAt    = createdAt;
    }

    public static RecurringCostDefinition create(UUID companyId, String name,
                                                  BigDecimal amount, CostCategory costCategory,
                                                  RecurringCostFrequency recurrence,
                                                  LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(companyId,    "companyId é obrigatório");
        Objects.requireNonNull(name,         "name é obrigatório");
        Objects.requireNonNull(amount,       "amount é obrigatório");
        Objects.requireNonNull(costCategory, "costCategory é obrigatório");
        Objects.requireNonNull(recurrence,   "recurrence é obrigatório");
        Objects.requireNonNull(startDate,    "startDate é obrigatório");
        if (name.isBlank())             throw new IllegalArgumentException("name não pode ser vazio");
        if (amount.signum() <= 0)       throw new IllegalArgumentException("amount deve ser positivo");
        if (endDate != null && !endDate.isAfter(startDate))
            throw new IllegalArgumentException("endDate deve ser posterior a startDate");
        return new RecurringCostDefinition(UUID.randomUUID(), companyId, name,
                amount, costCategory, recurrence, startDate, endDate, true, Instant.now());
    }

    public static RecurringCostDefinition reconstitute(UUID id, UUID companyId, String name,
                                                        BigDecimal amount, CostCategory costCategory,
                                                        RecurringCostFrequency recurrence,
                                                        LocalDate startDate, LocalDate endDate,
                                                        boolean active,
                                                        Instant createdAt, Instant updatedAt) {
        RecurringCostDefinition d = new RecurringCostDefinition(id, companyId, name,
                amount, costCategory, recurrence, startDate, endDate, active, createdAt);
        d.updatedAt = updatedAt;
        return d;
    }

    /**
     * Desativa a definição e, opcionalmente, fecha a data de vigência.
     */
    public void deactivate(LocalDate closedAt) {
        this.active    = false;
        this.endDate   = closedAt != null ? closedAt : this.endDate;
        this.updatedAt = Instant.now();
    }

    /**
     * Retorna {@code true} se esta definição deve gerar uma ocorrência para o
     * ano/mês informados, considerando o intervalo de vigência e a frequência.
     */
    public boolean shouldGenerateFor(int year, int month) {
        LocalDate target   = LocalDate.of(year, month, 1);
        LocalDate defStart = startDate.withDayOfMonth(1);
        if (target.isBefore(defStart)) return false;
        if (endDate != null && target.isAfter(endDate.withDayOfMonth(1))) return false;

        return switch (recurrence) {
            case MONTHLY  -> true;
            case QUARTERLY -> {
                int diffMonths = (year - startDate.getYear()) * 12
                               + (month - startDate.getMonthValue());
                yield diffMonths >= 0 && diffMonths % 3 == 0;
            }
            case ANNUAL   -> month == startDate.getMonthValue();
        };
    }
}
