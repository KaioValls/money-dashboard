package br.com.valls.moneycontrol.infrastructure.persistence.entity;

import br.com.valls.moneycontrol.domain.enums.OccurrenceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recurring_cost_occurrences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecurringCostOccurrenceJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "definition_id", nullable = false)
    private UUID definitionId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "reference_year", nullable = false)
    private int referenceYear;

    @Column(name = "reference_month", nullable = false)
    private int referenceMonth;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OccurrenceStatus status;

    @Column(name = "paid", nullable = false)
    private boolean paid;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "skip_reason")
    private String skipReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
