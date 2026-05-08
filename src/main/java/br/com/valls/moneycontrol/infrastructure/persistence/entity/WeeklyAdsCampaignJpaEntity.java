package br.com.valls.moneycontrol.infrastructure.persistence.entity;

import br.com.valls.moneycontrol.domain.enums.AdsRatioMethod;
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
@Table(name = "weekly_ads_campaigns")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WeeklyAdsCampaignJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "iso_year", nullable = false)
    private int isoYear;

    @Column(name = "iso_week_number", nullable = false)
    private int isoWeekNumber;

    @Column(name = "total_ads_budget", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAdsBudget;

    @Enumerated(EnumType.STRING)
    @Column(name = "ratio_method", nullable = false, length = 20)
    private AdsRatioMethod ratioMethod;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
