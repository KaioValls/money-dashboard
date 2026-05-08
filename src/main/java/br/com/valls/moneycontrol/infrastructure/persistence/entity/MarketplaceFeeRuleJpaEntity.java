package br.com.valls.moneycontrol.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "marketplace_fee_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MarketplaceFeeRuleJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "marketplace_id", nullable = false)
    private UUID marketplaceId;

    @Column(name = "min_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "max_price", precision = 12, scale = 2)
    private BigDecimal maxPrice;

    @Column(name = "fee_percentage", nullable = false, precision = 6, scale = 4)
    private BigDecimal feePercentage;

    @Column(name = "fixed_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal fixedFee;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
