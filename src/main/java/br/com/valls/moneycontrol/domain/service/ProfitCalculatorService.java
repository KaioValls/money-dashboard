package br.com.valls.moneycontrol.domain.service;

import br.com.valls.moneycontrol.domain.model.FeeResult;
import br.com.valls.moneycontrol.domain.model.SaleEntryInput;
import br.com.valls.moneycontrol.domain.model.TaxConfig;
import br.com.valls.moneycontrol.domain.model.WeeklyResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Serviço de domínio responsável por apurar o resultado financeiro de um lançamento de venda.
 *
 * <p>Fórmulas aplicadas:</p>
 * <pre>
 *   grossRevenue        = salePrice × unitsSold
 *   totalCogs           = unitCost  × unitsSold
 *   feePerUnit          = FeeCalculatorService.calculate(salePrice, rules, date)
 *   marketplaceFeeTotal = feePerUnit.totalFeeAmount × unitsSold
 *
 *   taxAmount:
 *     ON_SALE     = grossRevenue × taxRate
 *     ON_PURCHASE = totalCogs    × taxRate
 *     SEPARATE    = ZERO
 *
 *   grossProfit = grossRevenue − marketplaceFeeTotal − totalCogs
 *   netProfit   = grossProfit  − taxAmount − adsCost − couponDiscount
 *                              − freightCost − returnAmount − otherCosts
 *
 *   netMarginPercentage = (netProfit / grossRevenue) × 100   [escala 4]
 *   roiPercentage       = (netProfit / totalCogs)    × 100   [escala 4]
 * </pre>
 */
public class ProfitCalculatorService {

    private final FeeCalculatorService feeCalculatorService;

    public ProfitCalculatorService(FeeCalculatorService feeCalculatorService) {
        this.feeCalculatorService = Objects.requireNonNull(feeCalculatorService,
                "feeCalculatorService é obrigatório");
    }

    /**
     * Calcula o resultado financeiro para um lançamento de venda.
     *
     * @param input dados do lançamento
     * @return {@link WeeklyResult} com todos os campos calculados
     */
    public WeeklyResult calculate(SaleEntryInput input) {
        Objects.requireNonNull(input, "input é obrigatório");

        BigDecimal units = BigDecimal.valueOf(input.unitsSold());

        // ── Receita bruta e CMV ──────────────────────────────────────────────────
        BigDecimal grossRevenue = input.salePrice()
                .multiply(units)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalCogs = input.unitCost()
                .multiply(units)
                .setScale(2, RoundingMode.HALF_UP);

        // ── Comissão do marketplace ──────────────────────────────────────────────
        FeeResult feeResult = feeCalculatorService.calculate(
                input.salePrice(), input.feeRules(), input.saleDate());

        BigDecimal marketplaceFeeTotal = feeResult.totalFeeAmount()
                .multiply(units)
                .setScale(2, RoundingMode.HALF_UP);

        // ── Tributação ───────────────────────────────────────────────────────────
        BigDecimal taxAmount = computeTax(input.taxConfig(), grossRevenue, totalCogs);

        // ── Lucro bruto e líquido ────────────────────────────────────────────────
        BigDecimal grossProfit = grossRevenue
                .subtract(marketplaceFeeTotal)
                .subtract(totalCogs)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal netProfit = grossProfit
                .subtract(taxAmount)
                .subtract(input.adsCost())
                .subtract(input.couponDiscount())
                .subtract(input.freightCost())
                .subtract(input.returnAmount())
                .subtract(input.otherCosts())
                .setScale(2, RoundingMode.HALF_UP);

        // ── Indicadores percentuais ──────────────────────────────────────────────
        BigDecimal netMarginPercentage = grossRevenue.signum() == 0
                ? BigDecimal.ZERO
                : netProfit.multiply(BigDecimal.valueOf(100))
                           .divide(grossRevenue, 4, RoundingMode.HALF_UP);

        BigDecimal roiPercentage = totalCogs.signum() == 0
                ? BigDecimal.ZERO
                : netProfit.multiply(BigDecimal.valueOf(100))
                           .divide(totalCogs, 4, RoundingMode.HALF_UP);

        return new WeeklyResult(
                input.storeId(),
                input.weekReference(),
                grossRevenue,
                totalCogs,
                marketplaceFeeTotal,
                taxAmount,
                input.adsCost(),
                input.couponDiscount(),
                input.freightCost(),
                input.returnAmount(),
                input.otherCosts(),
                grossProfit,
                netProfit,
                netMarginPercentage,
                roiPercentage
        );
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private BigDecimal computeTax(TaxConfig tax, BigDecimal grossRevenue, BigDecimal totalCogs) {
        BigDecimal rate = tax.getRatePercentage()
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        return switch (tax.getChargeMoment()) {
            case ON_SALE     -> grossRevenue.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            case ON_PURCHASE -> totalCogs.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            case SEPARATE    -> BigDecimal.ZERO.setScale(2);
        };
    }
}
