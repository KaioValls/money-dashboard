package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.out.SaleEntryRepository;
import br.com.valls.moneycontrol.domain.model.SaleEntry;
import br.com.valls.moneycontrol.domain.model.WeekOverWeekDelta;
import br.com.valls.moneycontrol.domain.model.WeekReference;
import br.com.valls.moneycontrol.domain.model.WeeklyResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeeklyResultUseCaseImpl")
class WeeklyResultUseCaseImplTest {

    @Mock SaleEntryRepository saleEntryRepository;

    WeeklyResultUseCaseImpl useCase;

    private static final UUID          STORE_ID   = UUID.randomUUID();
    private static final UUID          COMPANY_ID = UUID.randomUUID();
    private static final WeekReference WEEK       = WeekReference.of(LocalDate.of(2025, 6, 16));

    @BeforeEach
    void setUp() {
        useCase = new WeeklyResultUseCaseImpl(saleEntryRepository);
    }

    /**
     * Cria um SaleEntry com os valores financeiros especificados.
     * Campos não listados ficam com BigDecimal.ZERO.
     */
    private SaleEntry entry(BigDecimal grossRevenue, BigDecimal totalCogs,
                             BigDecimal feeTotal, BigDecimal taxAmount,
                             BigDecimal adsCost) {
        return SaleEntry.builder()
                .storeId(STORE_ID)
                .companyId(COMPANY_ID)
                .isoYear(WEEK.year())
                .isoWeekNumber(WEEK.weekNumber())
                .unitsSold(10)
                .salePriceSnapshot(new BigDecimal("50.00"))
                .unitCostSnapshot(BigDecimal.ZERO)
                .taxRateSnapshot(BigDecimal.ZERO)
                .feePercentageSnapshot(BigDecimal.ZERO)
                .fixedFeeSnapshot(BigDecimal.ZERO)
                .grossRevenue(grossRevenue)
                .totalCogs(totalCogs)
                .marketplaceFeeTotal(feeTotal)
                .taxAmount(taxAmount)
                .adsCost(adsCost)
                .couponDiscount(BigDecimal.ZERO)
                .freightCost(BigDecimal.ZERO)
                .returnAmount(BigDecimal.ZERO)
                .otherCosts(BigDecimal.ZERO)
                .grossProfit(grossRevenue.subtract(feeTotal).subtract(totalCogs))
                .netProfit(grossRevenue.subtract(feeTotal).subtract(totalCogs)
                        .subtract(taxAmount).subtract(adsCost))
                .netMarginPercentage(BigDecimal.ZERO)
                .roiPercentage(BigDecimal.ZERO)
                .build();
    }

    // ── computeByStore ────────────────────────────────────────────────────────

    @Test
    @DisplayName("computeByStore agrega dois lançamentos corretamente")
    void computeByStoreSumsTwoEntries() {
        // entry1: rev=500, cogs=300, fee=80, tax=30, ads=0
        // entry2: rev=200, cogs=100, fee=32, tax=12, ads=0
        SaleEntry e1 = entry(new BigDecimal("500.00"), new BigDecimal("300.00"),
                new BigDecimal("80.00"), new BigDecimal("30.00"), BigDecimal.ZERO);
        SaleEntry e2 = entry(new BigDecimal("200.00"), new BigDecimal("100.00"),
                new BigDecimal("32.00"), new BigDecimal("12.00"), BigDecimal.ZERO);

        when(saleEntryRepository.findByStoreAndWeek(STORE_ID, WEEK))
                .thenReturn(List.of(e1, e2));

        WeeklyResult result = useCase.computeByStore(STORE_ID, WEEK);

        assertThat(result.grossRevenue()).isEqualByComparingTo("700.00");
        assertThat(result.totalCogs()).isEqualByComparingTo("400.00");
        assertThat(result.marketplaceFeeTotal()).isEqualByComparingTo("112.00");
        assertThat(result.taxAmount()).isEqualByComparingTo("42.00");
        // grossProfit = 700 - 112 - 400 = 188
        assertThat(result.grossProfit()).isEqualByComparingTo("188.00");
        // netProfit = 188 - 42 = 146
        assertThat(result.netProfit()).isEqualByComparingTo("146.00");
    }

    @Test
    @DisplayName("computeByStore retorna resultado zerado quando sem lançamentos")
    void computeByStoreEmptyEntries() {
        when(saleEntryRepository.findByStoreAndWeek(STORE_ID, WEEK))
                .thenReturn(List.of());

        WeeklyResult result = useCase.computeByStore(STORE_ID, WEEK);

        assertThat(result.grossRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.netProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.netMarginPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.roiPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.storeId()).isEqualTo(STORE_ID);
        assertThat(result.weekReference()).isEqualTo(WEEK);
    }

    @Test
    @DisplayName("computeByStore calcula netMarginPercentage = (netProfit / grossRevenue) × 100")
    void computeByStoreNetMargin() {
        // netProfit = 90, grossRevenue = 500 → margem = 18%
        SaleEntry e = entry(new BigDecimal("500.00"), new BigDecimal("300.00"),
                new BigDecimal("80.00"), new BigDecimal("30.00"), BigDecimal.ZERO);
        when(saleEntryRepository.findByStoreAndWeek(STORE_ID, WEEK))
                .thenReturn(List.of(e));

        WeeklyResult result = useCase.computeByStore(STORE_ID, WEEK);
        // grossProfit = 500 - 80 - 300 = 120; netProfit = 120 - 30 = 90
        assertThat(result.netProfit()).isEqualByComparingTo("90.00");
        // netMargin = 90 / 500 × 100 = 18.00%
        assertThat(result.netMarginPercentage()).isEqualByComparingTo("18.00");
    }

    // ── computeConsolidated ───────────────────────────────────────────────────

    @Test
    @DisplayName("computeConsolidated usa findByCompanyAndWeek com storeId=null no resultado")
    void computeConsolidatedUsesCompanyRepo() {
        when(saleEntryRepository.findByCompanyAndWeek(COMPANY_ID, WEEK))
                .thenReturn(List.of());

        WeeklyResult result = useCase.computeConsolidated(COMPANY_ID, WEEK);

        assertThat(result.storeId()).isNull();
        verify(saleEntryRepository).findByCompanyAndWeek(COMPANY_ID, WEEK);
        verify(saleEntryRepository, never()).findByStoreAndWeek(any(), any());
    }

    @Test
    @DisplayName("computeConsolidated soma lançamentos de múltiplas lojas")
    void computeConsolidatedSumsAllStores() {
        SaleEntry e1 = entry(new BigDecimal("1000.00"), new BigDecimal("600.00"),
                new BigDecimal("160.00"), new BigDecimal("60.00"), BigDecimal.ZERO);
        SaleEntry e2 = entry(new BigDecimal("500.00"), new BigDecimal("300.00"),
                new BigDecimal("80.00"), new BigDecimal("30.00"), BigDecimal.ZERO);
        when(saleEntryRepository.findByCompanyAndWeek(COMPANY_ID, WEEK))
                .thenReturn(List.of(e1, e2));

        WeeklyResult result = useCase.computeConsolidated(COMPANY_ID, WEEK);

        assertThat(result.grossRevenue()).isEqualByComparingTo("1500.00");
        assertThat(result.netProfit()).isEqualByComparingTo("270.00"); // (1000-160-600-60)+(500-80-300-30)
    }

    // ── weekOverWeekDelta ─────────────────────────────────────────────────────

    @Test
    @DisplayName("weekOverWeekDelta calcula delta e percentual corretamente")
    void weekOverWeekDeltaCalculates() {
        WeekReference prevWeek = WEEK.previous();

        // Semana atual: grossRevenue=500, netProfit=90
        SaleEntry eCurrent = entry(new BigDecimal("500.00"), new BigDecimal("300.00"),
                new BigDecimal("80.00"), new BigDecimal("30.00"), BigDecimal.ZERO);
        // Semana anterior: grossRevenue=400, netProfit=72
        SaleEntry ePrev = entry(new BigDecimal("400.00"), new BigDecimal("240.00"),
                new BigDecimal("64.00"), new BigDecimal("24.00"), BigDecimal.ZERO);

        when(saleEntryRepository.findByStoreAndWeek(STORE_ID, WEEK))
                .thenReturn(List.of(eCurrent));
        when(saleEntryRepository.findByStoreAndWeek(STORE_ID, prevWeek))
                .thenReturn(List.of(ePrev));

        WeekOverWeekDelta delta = useCase.weekOverWeekDelta(STORE_ID, WEEK);

        assertThat(delta.currentWeek()).isEqualTo(WEEK);
        assertThat(delta.previousWeek()).isEqualTo(prevWeek);
        // grossRevenueDelta = 500 - 400 = 100
        assertThat(delta.grossRevenueDelta()).isEqualByComparingTo("100.00");
        // grossRevenueDeltaPct = 100 / 400 × 100 = 25%
        assertThat(delta.grossRevenueDeltaPct()).isEqualByComparingTo("25.00");
        // netProfitDelta = 90 - 72 = 18
        assertThat(delta.netProfitDelta()).isEqualByComparingTo("18.00");
    }

    @Test
    @DisplayName("weekOverWeekDelta retorna 0% quando semana anterior está zerada")
    void weekOverWeekDeltaZeroBase() {
        WeekReference prevWeek = WEEK.previous();

        SaleEntry eCurrent = entry(new BigDecimal("500.00"), new BigDecimal("300.00"),
                new BigDecimal("80.00"), new BigDecimal("30.00"), BigDecimal.ZERO);

        when(saleEntryRepository.findByStoreAndWeek(STORE_ID, WEEK))
                .thenReturn(List.of(eCurrent));
        when(saleEntryRepository.findByStoreAndWeek(STORE_ID, prevWeek))
                .thenReturn(List.of()); // sem lançamentos na semana anterior

        WeekOverWeekDelta delta = useCase.weekOverWeekDelta(STORE_ID, WEEK);

        assertThat(delta.grossRevenueDeltaPct()).isEqualByComparingTo("0.00");
        assertThat(delta.netProfitDeltaPct()).isEqualByComparingTo("0.00");
    }
}
