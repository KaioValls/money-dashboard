package br.com.valls.moneycontrol.domain.model;

import br.com.valls.moneycontrol.domain.enums.OccurrenceStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Materializa uma ocorrência periódica de um {@link RecurringCostDefinition}.
 * UNIQUE(definition_id, reference_year, reference_month) impede duplicata.
 */
@Getter
public class RecurringCostOccurrence {

    private final UUID             id;
    private final UUID             definitionId;
    private final UUID             companyId;
    private final int              referenceYear;
    private final int              referenceMonth;
    private       BigDecimal       amount;
    private       OccurrenceStatus status;
    private       boolean          paid;
    private       Instant          paidAt;
    private       String           skipReason;
    private final Instant          createdAt;
    private       Instant          updatedAt;

    private RecurringCostOccurrence(UUID id, UUID definitionId, UUID companyId,
                                     int referenceYear, int referenceMonth,
                                     BigDecimal amount, OccurrenceStatus status,
                                     boolean paid, Instant paidAt, String skipReason,
                                     Instant createdAt) {
        this.id             = id;
        this.definitionId   = definitionId;
        this.companyId      = companyId;
        this.referenceYear  = referenceYear;
        this.referenceMonth = referenceMonth;
        this.amount         = amount;
        this.status         = status;
        this.paid           = paid;
        this.paidAt         = paidAt;
        this.skipReason     = skipReason;
        this.createdAt      = createdAt;
        this.updatedAt      = createdAt;
    }

    public static RecurringCostOccurrence create(UUID definitionId, UUID companyId,
                                                  int referenceYear, int referenceMonth,
                                                  BigDecimal amount) {
        Objects.requireNonNull(definitionId, "definitionId é obrigatório");
        Objects.requireNonNull(companyId,    "companyId é obrigatório");
        Objects.requireNonNull(amount,       "amount é obrigatório");
        if (referenceMonth < 1 || referenceMonth > 12)
            throw new IllegalArgumentException("referenceMonth deve estar entre 1 e 12");
        if (amount.signum() <= 0)
            throw new IllegalArgumentException("amount deve ser positivo");
        return new RecurringCostOccurrence(UUID.randomUUID(), definitionId, companyId,
                referenceYear, referenceMonth, amount,
                OccurrenceStatus.ACTIVE, false, null, null, Instant.now());
    }

    public static RecurringCostOccurrence reconstitute(UUID id, UUID definitionId, UUID companyId,
                                                        int referenceYear, int referenceMonth,
                                                        BigDecimal amount, OccurrenceStatus status,
                                                        boolean paid, Instant paidAt, String skipReason,
                                                        Instant createdAt, Instant updatedAt) {
        RecurringCostOccurrence o = new RecurringCostOccurrence(id, definitionId, companyId,
                referenceYear, referenceMonth, amount, status, paid, paidAt, skipReason, createdAt);
        o.updatedAt = updatedAt;
        return o;
    }

    /**
     * Marca a ocorrência como SKIPPED com o motivo informado.
     * @throws IllegalStateException se já está paga ou cancelada.
     */
    public void skip(String reason) {
        if (paid) throw new IllegalStateException("Ocorrência já está paga — não pode ser ignorada");
        if (status == OccurrenceStatus.CANCELLED)
            throw new IllegalStateException("Ocorrência cancelada não pode ser ignorada");
        this.status     = OccurrenceStatus.SKIPPED;
        this.skipReason = reason;
        this.updatedAt  = Instant.now();
    }

    /**
     * Marca a ocorrência como paga.
     * @throws IllegalStateException se já está paga ou não está ACTIVE.
     */
    public void markAsPaid() {
        if (paid) throw new IllegalStateException("Ocorrência já está registrada como paga");
        if (status != OccurrenceStatus.ACTIVE)
            throw new IllegalStateException("Apenas ocorrências ACTIVE podem ser marcadas como pagas");
        this.paid      = true;
        this.paidAt    = Instant.now();
        this.updatedAt = Instant.now();
    }
}
