package br.com.valls.moneycontrol.infrastructure.persistence.entity;

import br.com.valls.moneycontrol.domain.enums.ChargeMoment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidade JPA para a tabela particionada {@code sale_entries}.
 * Usa {@link IdClass} com chave composta (id, iso_year).
 */
@Entity
@Table(name = "sale_entries")
@IdClass(SaleEntryId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SaleEntryJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "marketplace_id", nullable = false)
    private UUID marketplaceId;

    @Column(name = "tax_config_id")
    private UUID taxConfigId;

    @Column(name = "fee_rule_id")
    private UUID feeRuleId;

    @Column(name = "date_key", nullable = false)
    private int dateKey;

    @Id
    @Column(name = "iso_year", nullable = false)
    private int isoYear;

    @Column(name = "iso_week_number", nullable = false)
    private int isoWeekNumber;

    @Column(name = "units_sold", nullable = false)
    private int unitsSold;

    // Snapshots imutáveis
    @Column(name = "sale_price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePriceSnapshot;

    @Column(name = "unit_cost_snapshot", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitCostSnapshot;

    @Column(name = "tax_rate_snapshot", nullable = false, precision = 6, scale = 4)
    private BigDecimal taxRateSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_charge_moment_snapshot", nullable = false, length = 20)
    private ChargeMoment taxChargeMomentSnapshot;

    @Column(name = "fee_percentage_snapshot", nullable = false, precision = 6, scale = 4)
    private BigDecimal feePercentageSnapshot;

    @Column(name = "fixed_fee_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal fixedFeeSnapshot;

    // Custos operacionais
    @Column(name = "ads_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal adsCost;

    @Column(name = "coupon_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal couponDiscount;

    @Column(name = "freight_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal freightCost;

    @Column(name = "return_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal returnAmount;

    @Column(name = "other_costs", nullable = false, precision = 12, scale = 2)
    private BigDecimal otherCosts;

    @Column(name = "other_costs_description")
    private String otherCostsDescription;

    // Campos calculados
    @Column(name = "gross_revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossRevenue;

    @Column(name = "total_cogs", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCogs;

    @Column(name = "marketplace_fee_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal marketplaceFeeTotal;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "gross_profit", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossProfit;

    @Column(name = "net_profit", nullable = false, precision = 12, scale = 2)
    private BigDecimal netProfit;

    @Column(name = "net_margin_percentage", nullable = false, precision = 8, scale = 4)
    private BigDecimal netMarginPercentage;

    @Column(name = "roi_percentage", nullable = false, precision = 8, scale = 4)
    private BigDecimal roiPercentage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
