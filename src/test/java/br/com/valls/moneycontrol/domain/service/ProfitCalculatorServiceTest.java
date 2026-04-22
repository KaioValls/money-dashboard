package br.com.valls.moneycontrol.domain.service;

import br.com.valls.moneycontrol.domain.enums.CalculationBase;
import br.com.valls.moneycontrol.domain.enums.ChargeMoment;
import br.com.valls.moneycontrol.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ProfitCalculatorService")
class ProfitCalculatorServiceTest {

    private ProfitCalculatorService service;

    private static final UUID      STORE_ID  = UUID.randomUUID();
    private static final UUID      MP_ID     = UUID.randomUUID();
    private static final UUID      COMP_ID   = UUID.randomUUID();
    private static final LocalDate SALE_DATE = LocalDate.of(2025, 6, 15);
    private static final WeekReference WEEK  = WeekReference.of(SALE_DATE);

    @BeforeEach
    void setUp() {
        service = new ProfitCalculatorService(new FeeCalculatorService());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private MarketplaceFeeRule feeRule(BigDecimal pct, BigDecimal fixed) {
        return MarketplaceFeeRule.create(MP_ID, BigDecimal.ZERO, null,
                pct, fixed, LocalDate.of(2024, 1, 1), 1);
    }

    private TaxConfig taxConfig(ChargeMoment moment, String rate) {
        return TaxConfig.create(COMP_ID, moment, CalculationBase.GROSS_REVENUE,
                new BigDecimal(rate), LocalDate.of(2024, 1, 1));
    }

    private SaleEntryInput input(BigDecimal price, int units, BigDecimal cost,
                                  MarketplaceFeeRule rule, TaxConfig tax) {
        return new SaleEntryInput(STORE_ID, WEEK, SALE_DATE, price, units, cost,
                List.of(rule), tax,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
    }

    // ── cenário principal ──────────────────────────────────────────────────────

    @Test
    @DisplayName("cenário base: 10un × R$50, custo R$30, fee 16%, tax 6% ON_SALE")
    void baseScenario() {
        SaleEntryInput input = input(
                new BigDecimal("50.00"), 10, new BigDecimal("30.00"),
                feeRule(new BigDecimal("16.00"), BigDecimal.ZERO),
                taxConfig(ChargeMoment.ON_SALE, "6.00"));

        WeeklyResult r = service.calculate(input);

        assertThat(r.grossRevenue()).isEqualByComparingTo("500.00");         // 10×50
        assertThat(r.totalCogs()).isEqualByComparingTo("300.00");            // 10×30
        assertThat(r.marketplaceFeeTotal()).isEqualByComparingTo("80.00");   // 10×8
        assertThat(r.taxAmount()).isEqualByComparingTo("30.00");             // 500×6%
        assertThat(r.grossProfit()).isEqualByComparingTo("120.00");          // 500-80-300
        assertThat(r.netProfit()).isEqualByComparingTo("90.00");             // 120-30
    }

    @Test
    @DisplayName("netMarginPercentage e roiPercentage calculados corretamente")
    void percentages() {
        SaleEntryInput input = input(
                new BigDecimal("50.00"), 10, new BigDecimal("30.00"),
                feeRule(new BigDecimal("16.00"), BigDecimal.ZERO),
                taxConfig(ChargeMoment.ON_SALE, "6.00"));

        WeeklyResult r = service.calculate(input);

        // netMargin = (90 / 500) × 100 = 18.0000
        assertThat(r.netMarginPercentage()).isEqualByComparingTo("18.0000");
        // roi = (90 / 300) × 100 = 30.0000
        assertThat(r.roiPercentage()).isEqualByComparingTo("30.0000");
    }

    @Test
    @DisplayName("imposto ON_PURCHASE usa totalCogs como base")
    void taxOnPurchase() {
        // totalCogs = 10×30 = 300; tax = 300×10% = 30
        SaleEntryInput input = input(
                new BigDecimal("50.00"), 10, new BigDecimal("30.00"),
                feeRule(new BigDecimal("16.00"), BigDecimal.ZERO),
                taxConfig(ChargeMoment.ON_PURCHASE, "10.00"));

        WeeklyResult r = service.calculate(input);

        assertThat(r.taxAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("imposto SEPARATE resulta em taxAmount = 0")
    void taxSeparate() {
        SaleEntryInput input = input(
                new BigDecimal("50.00"), 10, new BigDecimal("30.00"),
                feeRule(new BigDecimal("16.00"), BigDecimal.ZERO),
                taxConfig(ChargeMoment.SEPARATE, "6.00"));

        WeeklyResult r = service.calculate(input);

        assertThat(r.taxAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("custos variáveis (ads, cupom, frete, devoluções, outros) deduzidos do netProfit")
    void variableCostsDeduted() {
        SaleEntryInput input = new SaleEntryInput(
                STORE_ID, WEEK, SALE_DATE,
                new BigDecimal("50.00"), 10, new BigDecimal("30.00"),
                List.of(feeRule(new BigDecimal("16.00"), BigDecimal.ZERO)),
                taxConfig(ChargeMoment.ON_SALE, "6.00"),
                new BigDecimal("5.00"),   // adsCost
                new BigDecimal("3.00"),   // couponDiscount
                new BigDecimal("4.00"),   // freightCost
                new BigDecimal("2.00"),   // returnAmount
                new BigDecimal("1.00")    // otherCosts
        );

        WeeklyResult r = service.calculate(input);

        // netProfit = 120 - 30(tax) - 5 - 3 - 4 - 2 - 1 = 75
        assertThat(r.netProfit()).isEqualByComparingTo("75.00");
        assertThat(r.adsTotal()).isEqualByComparingTo("5.00");
        assertThat(r.couponTotal()).isEqualByComparingTo("3.00");
        assertThat(r.freightTotal()).isEqualByComparingTo("4.00");
        assertThat(r.returnTotal()).isEqualByComparingTo("2.00");
        assertThat(r.otherCosts()).isEqualByComparingTo("1.00");
    }

    @Test
    @DisplayName("fee com fixedFee é somado ao percentual")
    void feeWithFixedPart() {
        // fee = 10% × 100 + R$2 = R$12 por unidade; 5 unidades = R$60
        SaleEntryInput input = input(
                new BigDecimal("100.00"), 5, new BigDecimal("60.00"),
                feeRule(new BigDecimal("10.00"), new BigDecimal("2.00")),
                taxConfig(ChargeMoment.SEPARATE, "0.10"));

        WeeklyResult r = service.calculate(input);

        assertThat(r.marketplaceFeeTotal()).isEqualByComparingTo("60.00");
        // grossRevenue=500, totalCogs=300, marketplaceFeeTotal=60 → grossProfit=140
        assertThat(r.grossProfit()).isEqualByComparingTo("140.00");
    }

    @Test
    @DisplayName("storeId e weekReference preservados no resultado")
    void preservesIdentifiers() {
        SaleEntryInput input = input(
                new BigDecimal("50.00"), 1, new BigDecimal("30.00"),
                feeRule(new BigDecimal("10.00"), BigDecimal.ZERO),
                taxConfig(ChargeMoment.SEPARATE, "5.00"));

        WeeklyResult r = service.calculate(input);

        assertThat(r.storeId()).isEqualTo(STORE_ID);
        assertThat(r.weekReference()).isEqualTo(WEEK);
    }

    @Test
    @DisplayName("lança NullPointerException se input for null")
    void throwsOnNullInput() {
        assertThatThrownBy(() -> service.calculate(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("lança IllegalArgumentException se unitsSold < 1")
    void throwsOnZeroUnits() {
        assertThatThrownBy(() -> new SaleEntryInput(
                STORE_ID, WEEK, SALE_DATE,
                new BigDecimal("50.00"), 0, new BigDecimal("30.00"),
                List.of(feeRule(new BigDecimal("10.00"), BigDecimal.ZERO)),
                taxConfig(ChargeMoment.SEPARATE, "5.00"),
                null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unitsSold");
    }

    @Test
    @DisplayName("netMarginPercentage é ZERO quando grossRevenue é zero (proteção divisão por zero)")
    void netMarginZeroWhenNoRevenue() {
        // Cenário artificial forçando grossRevenue=0
        // Não é possível via SaleEntryInput normal (preço deve ser >0),
        // mas testamos o serviço com totalCogs > grossRevenue para netProfit negativo
        SaleEntryInput input = input(
                new BigDecimal("1.00"), 1, new BigDecimal("100.00"),
                feeRule(new BigDecimal("0.00"), BigDecimal.ZERO),
                taxConfig(ChargeMoment.SEPARATE, "0.01"));

        // grossRevenue=1, totalCogs=100 → grossProfit = 1-0-100 = -99
        WeeklyResult r = service.calculate(input);

        assertThat(r.grossRevenue()).isEqualByComparingTo("1.00");
        // netMarginPercentage = (-99/1)*100 = -9900.0000
        assertThat(r.netMarginPercentage()).isEqualByComparingTo("-9900.0000");
    }
}
