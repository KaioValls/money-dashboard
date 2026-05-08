package br.com.valls.moneycontrol.infrastructure.persistence.entity;

import br.com.valls.moneycontrol.domain.enums.CalculationBase;
import br.com.valls.moneycontrol.domain.enums.ChargeMoment;
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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tax_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaxConfigJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_moment", nullable = false, length = 20)
    private ChargeMoment chargeMoment;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_base", nullable = false, length = 20)
    private CalculationBase calculationBase;

    @Column(name = "rate_percentage", nullable = false, precision = 6, scale = 4)
    private BigDecimal ratePercentage;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
