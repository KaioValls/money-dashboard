package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.in.WeeklyResultUseCase;
import br.com.valls.moneycontrol.application.ports.out.SaleEntryRepository;
import br.com.valls.moneycontrol.domain.model.SaleEntry;
import br.com.valls.moneycontrol.domain.model.WeekOverWeekDelta;
import br.com.valls.moneycontrol.domain.model.WeekReference;
import br.com.valls.moneycontrol.domain.model.WeeklyResult;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Implementação do use case de apuração semanal.
 * Instanciada exclusivamente em {@code BeanConfig} — sem @Service.
 *
 * <p>Os resultados são calculados sob demanda a partir dos {@code SaleEntry}
 * persistidos — sem snapshot adicional. Percentuais de margem e ROI são
 * recalculados sobre os totais agregados (não se podem somar percentuais).
 */
@RequiredArgsConstructor
public class WeeklyResultUseCaseImpl implements WeeklyResultUseCase {

    private final SaleEntryRepository saleEntryRepository;

    @Override
    public WeeklyResult computeByStore(UUID storeId, WeekReference week) {
        List<SaleEntry> entries = saleEntryRepository.findByStoreAndWeek(storeId, week);
        return aggregate(storeId, week, entries);
    }

    @Override
    public WeeklyResult computeConsolidated(UUID companyId, WeekReference week) {
        List<SaleEntry> entries = saleEntryRepository.findByCompanyAndWeek(companyId, week);
        return aggregate(null, week, entries);   // storeId null = empresa inteira
    }

    @Override
    public WeekOverWeekDelta weekOverWeekDelta(UUID storeId, WeekReference week) {
        WeeklyResult current  = computeByStore(storeId, week);
        WeeklyResult previous = computeByStore(storeId, week.previous());

        BigDecimal grossRevenueDelta = current.grossRevenue()
                .subtract(previous.grossRevenue()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netProfitDelta = current.netProfit()
                .subtract(previous.netProfit()).setScale(2, RoundingMode.HALF_UP);

        BigDecimal grossRevenueDeltaPct = deltaPercentage(grossRevenueDelta, previous.grossRevenue());
        BigDecimal netProfitDeltaPct    = deltaPercentage(netProfitDelta,    previous.netProfit());

        return new WeekOverWeekDelta(
                week, week.previous(),
                current, previous,
                grossRevenueDelta, netProfitDelta,
                grossRevenueDeltaPct, netProfitDeltaPct);
    }

    // ── aggregation helpers ───────────────────────────────────────────────────

    private static WeeklyResult aggregate(UUID storeId, WeekReference week,
                                           List<SaleEntry> entries) {
        BigDecimal grossRevenue       = sum(entries, SaleEntry::getGrossRevenue);
        BigDecimal totalCogs          = sum(entries, SaleEntry::getTotalCogs);
        BigDecimal marketplaceFeeTotal = sum(entries, SaleEntry::getMarketplaceFeeTotal);
        BigDecimal taxAmount          = sum(entries, SaleEntry::getTaxAmount);
        BigDecimal adsTotal           = sum(entries, SaleEntry::getAdsCost);
        BigDecimal couponTotal        = sum(entries, SaleEntry::getCouponDiscount);
        BigDecimal freightTotal       = sum(entries, SaleEntry::getFreightCost);
        BigDecimal returnTotal        = sum(entries, SaleEntry::getReturnAmount);
        BigDecimal otherCosts         = sum(entries, SaleEntry::getOtherCosts);

        // grossProfit = grossRevenue - marketplaceFeeTotal - totalCogs
        BigDecimal grossProfit = grossRevenue
                .subtract(marketplaceFeeTotal)
                .subtract(totalCogs);

        // netProfit = grossProfit - taxAmount - adsTotal - couponTotal
        //           - freightTotal - returnTotal - otherCosts
        BigDecimal netProfit = grossProfit
                .subtract(taxAmount)
                .subtract(adsTotal)
                .subtract(couponTotal)
                .subtract(freightTotal)
                .subtract(returnTotal)
                .subtract(otherCosts);

        BigDecimal netMarginPct = percentage(netProfit,   grossRevenue);
        BigDecimal roiPct       = percentage(netProfit,   totalCogs);

        return new WeeklyResult(
                storeId, week,
                grossRevenue, totalCogs, marketplaceFeeTotal, taxAmount,
                adsTotal, couponTotal, freightTotal, returnTotal, otherCosts,
                grossProfit, netProfit, netMarginPct, roiPct);
    }

    private static BigDecimal sum(List<SaleEntry> entries,
                                  Function<SaleEntry, BigDecimal> getter) {
        return entries.stream()
                .map(getter)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Percentual: (numerator / denominator) × 100, arredondado para 2 casas.
     * Retorna {@link BigDecimal#ZERO} com escala 2 se denominator = 0.
     */
    private static BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.signum() == 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return numerator
                .divide(denominator, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Percentual de variação: (delta / base) × 100.
     * Retorna 0 quando a base for zero (evita divisão por zero).
     */
    private static BigDecimal deltaPercentage(BigDecimal delta, BigDecimal base) {
        if (base.signum() == 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return delta
                .divide(base.abs(), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
