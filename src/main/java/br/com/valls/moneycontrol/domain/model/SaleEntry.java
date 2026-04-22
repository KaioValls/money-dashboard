package br.com.valls.moneycontrol.domain.model;

import br.com.valls.moneycontrol.domain.enums.ChargeMoment;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidade fato de venda semanal.
 * Construtor privado — instanciada exclusivamente pelo use case após cálculo completo.
 * Snapshots são imutáveis; apenas custos operacionais podem ser atualizados.
 */
@Getter
public class SaleEntry {

    private final UUID         id;
    private final UUID         companyId;
    private final UUID         storeId;
    private final UUID         listingId;
    private final UUID         marketplaceId;
    private final UUID         taxConfigId;
    private final UUID         feeRuleId;
    private final int          dateKey;
    private final int          isoYear;
    private final int          isoWeekNumber;
    private final int          unitsSold;
    // snapshots imutáveis
    private final BigDecimal   salePriceSnapshot;
    private final BigDecimal   unitCostSnapshot;
    private final BigDecimal   taxRateSnapshot;
    private final ChargeMoment taxChargeMomentSnapshot;
    private final BigDecimal   feePercentageSnapshot;
    private final BigDecimal   fixedFeeSnapshot;
    // custos operacionais (atualizáveis)
    private       BigDecimal   adsCost;
    private       BigDecimal   couponDiscount;
    private       BigDecimal   freightCost;
    private       BigDecimal   returnAmount;
    private       BigDecimal   otherCosts;
    private       String       otherCostsDescription;
    // campos calculados
    private       BigDecimal   grossRevenue;
    private       BigDecimal   totalCogs;
    private       BigDecimal   marketplaceFeeTotal;
    private       BigDecimal   taxAmount;
    private       BigDecimal   grossProfit;
    private       BigDecimal   netProfit;
    private       BigDecimal   netMarginPercentage;
    private       BigDecimal   roiPercentage;
    private final Instant      createdAt;
    private       Instant      updatedAt;

    @Builder
    private SaleEntry(UUID id, UUID companyId, UUID storeId, UUID listingId,
                      UUID marketplaceId, UUID taxConfigId, UUID feeRuleId,
                      int dateKey, int isoYear, int isoWeekNumber, int unitsSold,
                      BigDecimal salePriceSnapshot, BigDecimal unitCostSnapshot,
                      BigDecimal taxRateSnapshot, ChargeMoment taxChargeMomentSnapshot,
                      BigDecimal feePercentageSnapshot, BigDecimal fixedFeeSnapshot,
                      BigDecimal adsCost, BigDecimal couponDiscount, BigDecimal freightCost,
                      BigDecimal returnAmount, BigDecimal otherCosts, String otherCostsDescription,
                      BigDecimal grossRevenue, BigDecimal totalCogs, BigDecimal marketplaceFeeTotal,
                      BigDecimal taxAmount, BigDecimal grossProfit, BigDecimal netProfit,
                      BigDecimal netMarginPercentage, BigDecimal roiPercentage,
                      Instant createdAt, Instant updatedAt) {
        this.id                      = id != null ? id : UUID.randomUUID();
        this.companyId               = companyId;
        this.storeId                 = storeId;
        this.listingId               = listingId;
        this.marketplaceId           = marketplaceId;
        this.taxConfigId             = taxConfigId;
        this.feeRuleId               = feeRuleId;
        this.dateKey                 = dateKey;
        this.isoYear                 = isoYear;
        this.isoWeekNumber           = isoWeekNumber;
        this.unitsSold               = unitsSold;
        this.salePriceSnapshot       = salePriceSnapshot;
        this.unitCostSnapshot        = unitCostSnapshot;
        this.taxRateSnapshot         = taxRateSnapshot;
        this.taxChargeMomentSnapshot = taxChargeMomentSnapshot;
        this.feePercentageSnapshot   = feePercentageSnapshot;
        this.fixedFeeSnapshot        = fixedFeeSnapshot;
        this.adsCost                 = adsCost != null ? adsCost : BigDecimal.ZERO;
        this.couponDiscount          = couponDiscount != null ? couponDiscount : BigDecimal.ZERO;
        this.freightCost             = freightCost != null ? freightCost : BigDecimal.ZERO;
        this.returnAmount            = returnAmount != null ? returnAmount : BigDecimal.ZERO;
        this.otherCosts              = otherCosts != null ? otherCosts : BigDecimal.ZERO;
        this.otherCostsDescription   = otherCostsDescription;
        this.grossRevenue            = grossRevenue;
        this.totalCogs               = totalCogs;
        this.marketplaceFeeTotal     = marketplaceFeeTotal;
        this.taxAmount               = taxAmount;
        this.grossProfit             = grossProfit;
        this.netProfit               = netProfit;
        this.netMarginPercentage     = netMarginPercentage;
        this.roiPercentage           = roiPercentage;
        this.createdAt               = createdAt != null ? createdAt : Instant.now();
        this.updatedAt               = updatedAt != null ? updatedAt : Instant.now();
    }

    /** Atualiza custos operacionais e os campos calculados associados. */
    public void updateOperationalCosts(BigDecimal adsCost, BigDecimal couponDiscount,
                                       BigDecimal freightCost, BigDecimal returnAmount,
                                       BigDecimal otherCosts, String otherCostsDescription,
                                       BigDecimal grossProfit, BigDecimal netProfit,
                                       BigDecimal netMarginPercentage, BigDecimal roiPercentage) {
        this.adsCost               = adsCost;
        this.couponDiscount        = couponDiscount;
        this.freightCost           = freightCost;
        this.returnAmount          = returnAmount;
        this.otherCosts            = otherCosts;
        this.otherCostsDescription = otherCostsDescription;
        this.grossProfit           = grossProfit;
        this.netProfit             = netProfit;
        this.netMarginPercentage   = netMarginPercentage;
        this.roiPercentage         = roiPercentage;
        this.updatedAt             = Instant.now();
    }

    public WeekReference weekReference() {
        return new WeekReference(isoYear, isoWeekNumber);
    }
}
