package br.com.valls.moneycontrol.domain.service;

import br.com.valls.moneycontrol.domain.exception.FeeRuleGapException;
import br.com.valls.moneycontrol.domain.model.FeeResult;
import br.com.valls.moneycontrol.domain.model.MarketplaceFeeRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FeeCalculatorService")
class FeeCalculatorServiceTest {

    private FeeCalculatorService service;

    private static final UUID   MP_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2025, 6, 15);

    @BeforeEach
    void setUp() {
        service = new FeeCalculatorService();
    }

    // ── fábrica de regras ──────────────────────────────────────────────────────

    private MarketplaceFeeRule rule(BigDecimal min, BigDecimal max,
                                    BigDecimal feePct, BigDecimal fixedFee,
                                    int priority) {
        return MarketplaceFeeRule.create(MP_ID, min, max, feePct, fixedFee,
                LocalDate.of(2024, 1, 1), priority);
    }

    // ── testes ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("calcula fee percentual simples sem fee fixo")
    void simplePercentageFee() {
        // 16% de R$50 = R$8, fixedFee = 0
        MarketplaceFeeRule r = rule(BigDecimal.ZERO, null,
                new BigDecimal("16.00"), BigDecimal.ZERO, 1);

        FeeResult result = service.calculate(new BigDecimal("50.00"), List.of(r), TODAY);

        assertThat(result.totalFeeAmount()).isEqualByComparingTo("8.0000");
        assertThat(result.feePercentageSnapshot()).isEqualByComparingTo("16.00");
        assertThat(result.fixedFeeSnapshot()).isEqualByComparingTo("0");
        assertThat(result.feeRuleId()).isEqualTo(r.getId());
    }

    @Test
    @DisplayName("calcula fee percentual com fee fixo")
    void percentagePlusFixedFee() {
        // 10% de R$100 = R$10 + R$2 fixo = R$12
        MarketplaceFeeRule r = rule(BigDecimal.ZERO, null,
                new BigDecimal("10.00"), new BigDecimal("2.00"), 1);

        FeeResult result = service.calculate(new BigDecimal("100.00"), List.of(r), TODAY);

        assertThat(result.totalFeeAmount()).isEqualByComparingTo("12.0000");
    }

    @Test
    @DisplayName("lança FeeRuleGapException quando nenhuma regra corresponde ao preço")
    void throwsWhenNoPriceMatch() {
        // regra só cobre até R$49.99
        MarketplaceFeeRule r = rule(BigDecimal.ZERO, new BigDecimal("49.99"),
                new BigDecimal("10.00"), BigDecimal.ZERO, 1);

        assertThatThrownBy(() ->
                service.calculate(new BigDecimal("50.00"), List.of(r), TODAY))
                .isInstanceOf(FeeRuleGapException.class)
                .hasMessageContaining("50.00");
    }

    @Test
    @DisplayName("lança FeeRuleGapException quando lista de regras está vazia")
    void throwsWhenEmptyRuleList() {
        assertThatThrownBy(() ->
                service.calculate(new BigDecimal("50.00"), List.of(), TODAY))
                .isInstanceOf(FeeRuleGapException.class);
    }

    @Test
    @DisplayName("lança FeeRuleGapException quando regra não está vigente na data")
    void throwsWhenRuleNotActiveOnDate() {
        // regra começa em 2026-01-01, mas TODAY é 2025-06-15
        MarketplaceFeeRule r = MarketplaceFeeRule.create(MP_ID,
                BigDecimal.ZERO, null,
                new BigDecimal("10.00"), BigDecimal.ZERO,
                LocalDate.of(2026, 1, 1), 1);

        assertThatThrownBy(() ->
                service.calculate(new BigDecimal("50.00"), List.of(r), TODAY))
                .isInstanceOf(FeeRuleGapException.class);
    }

    @Test
    @DisplayName("seleciona regra com maior priority quando duas se aplicam")
    void selectsHighestPriorityRule() {
        // priority 2 tem taxa 5%, priority 1 tem taxa 20%
        MarketplaceFeeRule low  = rule(BigDecimal.ZERO, null, new BigDecimal("20.00"), BigDecimal.ZERO, 1);
        MarketplaceFeeRule high = rule(BigDecimal.ZERO, null, new BigDecimal("5.00"),  BigDecimal.ZERO, 2);

        FeeResult result = service.calculate(new BigDecimal("100.00"), List.of(low, high), TODAY);

        // deve usar a regra de maior priority (priority=2, fee=5%)
        assertThat(result.totalFeeAmount()).isEqualByComparingTo("5.0000");
        assertThat(result.feeRuleId()).isEqualTo(high.getId());
    }

    @Test
    @DisplayName("regra com faixa de preço exata: limite inferior incluso")
    void lowerBoundInclusive() {
        MarketplaceFeeRule r = rule(new BigDecimal("50.00"), new BigDecimal("100.00"),
                new BigDecimal("10.00"), BigDecimal.ZERO, 1);

        assertThatNoException().isThrownBy(() ->
                service.calculate(new BigDecimal("50.00"), List.of(r), TODAY));
    }

    @Test
    @DisplayName("regra com faixa de preço exata: limite superior incluso")
    void upperBoundInclusive() {
        MarketplaceFeeRule r = rule(new BigDecimal("50.00"), new BigDecimal("100.00"),
                new BigDecimal("10.00"), BigDecimal.ZERO, 1);

        assertThatNoException().isThrownBy(() ->
                service.calculate(new BigDecimal("100.00"), List.of(r), TODAY));
    }

    @Test
    @DisplayName("ignora regras inativas (active=false)")
    void ignoresInactiveRules() {
        MarketplaceFeeRule r = rule(BigDecimal.ZERO, null, new BigDecimal("10.00"), BigDecimal.ZERO, 1);
        r.deactivate();

        assertThatThrownBy(() ->
                service.calculate(new BigDecimal("50.00"), List.of(r), TODAY))
                .isInstanceOf(FeeRuleGapException.class);
    }

    @Test
    @DisplayName("lança NullPointerException se salePrice for null")
    void throwsOnNullPrice() {
        assertThatThrownBy(() -> service.calculate(null, List.of(), TODAY))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("resultado tem escala 4 (HALF_UP)")
    void resultHasScale4() {
        // 7% de R$33.33 = 2.3331
        MarketplaceFeeRule r = rule(BigDecimal.ZERO, null, new BigDecimal("7.00"), BigDecimal.ZERO, 1);

        FeeResult result = service.calculate(new BigDecimal("33.33"), List.of(r), TODAY);

        assertThat(result.totalFeeAmount().scale()).isEqualTo(4);
        assertThat(result.totalFeeAmount()).isEqualByComparingTo("2.3331");
    }
}
