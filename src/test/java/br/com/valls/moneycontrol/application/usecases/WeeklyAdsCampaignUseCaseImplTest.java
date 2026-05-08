package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.in.SaleEntryUseCase;
import br.com.valls.moneycontrol.application.ports.out.StoreRepository;
import br.com.valls.moneycontrol.application.ports.out.WeeklyAdsCampaignRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterWeeklyAdsCampaignCommand;
import br.com.valls.moneycontrol.application.usecases.command.UpdateSaleEntryCommand;
import br.com.valls.moneycontrol.domain.enums.AdsRatioMethod;
import br.com.valls.moneycontrol.domain.enums.ListingStatus;
import br.com.valls.moneycontrol.domain.exception.StoreNotFoundException;
import br.com.valls.moneycontrol.domain.model.SaleEntry;
import br.com.valls.moneycontrol.domain.model.Store;
import br.com.valls.moneycontrol.domain.model.WeeklyAdsCampaign;
import br.com.valls.moneycontrol.domain.model.WeekReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeeklyAdsCampaignUseCaseImpl")
class WeeklyAdsCampaignUseCaseImplTest {

    @Mock WeeklyAdsCampaignRepository campaignRepository;
    @Mock StoreRepository             storeRepository;
    @Mock SaleEntryUseCase            saleEntryUseCase;

    WeeklyAdsCampaignUseCaseImpl useCase;

    private static final UUID          STORE_ID   = UUID.randomUUID();
    private static final UUID          COMPANY_ID = UUID.randomUUID();
    private static final UUID          MP_ID      = UUID.randomUUID();
    private static final WeekReference WEEK       = WeekReference.of(LocalDate.of(2025, 6, 16));

    @BeforeEach
    void setUp() {
        useCase = new WeeklyAdsCampaignUseCaseImpl(campaignRepository, storeRepository, saleEntryUseCase);
    }

    private Store stubStore() {
        UUID mpAccountId = UUID.randomUUID();
        return Store.reconstitute(STORE_ID, mpAccountId, COMPANY_ID, MP_ID, "Loja Teste",
                br.com.valls.moneycontrol.domain.enums.StoreStatus.ACTIVE,
                br.com.valls.moneycontrol.domain.enums.AdsRatioMode.CAMPAIGN,
                true, Instant.now(), Instant.now());
    }

    private SaleEntry stubSaleEntry(UUID id, BigDecimal grossRevenue, int unitsSold) {
        return SaleEntry.builder()
                .id(id)
                .storeId(STORE_ID)
                .companyId(COMPANY_ID)
                .isoYear(WEEK.year())
                .isoWeekNumber(WEEK.weekNumber())
                .unitsSold(unitsSold)
                .grossRevenue(grossRevenue)
                .marketplaceFeeTotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalCogs(BigDecimal.ZERO)
                .grossProfit(grossRevenue)
                .netProfit(grossRevenue)
                .netMarginPercentage(BigDecimal.ZERO)
                .roiPercentage(BigDecimal.ZERO)
                .salePriceSnapshot(new BigDecimal("50.00"))
                .unitCostSnapshot(BigDecimal.ZERO)
                .taxRateSnapshot(BigDecimal.ZERO)
                .feePercentageSnapshot(BigDecimal.ZERO)
                .fixedFeeSnapshot(BigDecimal.ZERO)
                .adsCost(BigDecimal.ZERO)
                .couponDiscount(BigDecimal.ZERO)
                .freightCost(BigDecimal.ZERO)
                .returnAmount(BigDecimal.ZERO)
                .otherCosts(BigDecimal.ZERO)
                .build();
    }

    // ── registerCampaign ─────────────────────────────────────────────────────

    @Test
    @DisplayName("registerCampaign cria nova campanha quando não existe")
    void registerCampaignCreatesNew() {
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(stubStore()));
        when(campaignRepository.findByStoreAndWeek(STORE_ID, WEEK)).thenReturn(Optional.empty());
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new RegisterWeeklyAdsCampaignCommand(
                STORE_ID, WEEK.year(), WEEK.weekNumber(),
                new BigDecimal("500.00"), AdsRatioMethod.BY_REVENUE);

        WeeklyAdsCampaign result = useCase.registerCampaign(cmd);

        assertThat(result.getStoreId()).isEqualTo(STORE_ID);
        assertThat(result.getTotalAdsBudget()).isEqualByComparingTo("500.00");
        assertThat(result.getRatioMethod()).isEqualTo(AdsRatioMethod.BY_REVENUE);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("registerCampaign atualiza campanha existente (upsert)")
    void registerCampaignUpdatesExisting() {
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(stubStore()));
        WeeklyAdsCampaign existing = WeeklyAdsCampaign.create(
                STORE_ID, WEEK.year(), WEEK.weekNumber(),
                new BigDecimal("200.00"), AdsRatioMethod.BY_UNITS);
        when(campaignRepository.findByStoreAndWeek(STORE_ID, WEEK)).thenReturn(Optional.of(existing));
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new RegisterWeeklyAdsCampaignCommand(
                STORE_ID, WEEK.year(), WEEK.weekNumber(),
                new BigDecimal("600.00"), AdsRatioMethod.BY_REVENUE);

        WeeklyAdsCampaign result = useCase.registerCampaign(cmd);

        assertThat(result.getTotalAdsBudget()).isEqualByComparingTo("600.00");
        assertThat(result.getRatioMethod()).isEqualTo(AdsRatioMethod.BY_REVENUE);
        // garantia de upsert: apenas um save
        verify(campaignRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("registerCampaign lança StoreNotFoundException para loja inexistente")
    void registerCampaignStoreNotFound() {
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.registerCampaign(
                new RegisterWeeklyAdsCampaignCommand(
                        STORE_ID, 2025, 25, new BigDecimal("300.00"), AdsRatioMethod.BY_REVENUE)))
                .isInstanceOf(StoreNotFoundException.class);
    }

    // ── distributeAds — BY_REVENUE ────────────────────────────────────────────

    @Test
    @DisplayName("distributeAds BY_REVENUE distribui proporcionalmente à receita")
    void distributeAdsByRevenue() {
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();
        // e1: 300 receita, e2: 200 receita → 60% / 40% de 500
        SaleEntry entry1 = stubSaleEntry(e1, new BigDecimal("300.00"), 6);
        SaleEntry entry2 = stubSaleEntry(e2, new BigDecimal("200.00"), 4);

        WeeklyAdsCampaign campaign = WeeklyAdsCampaign.create(
                STORE_ID, WEEK.year(), WEEK.weekNumber(),
                new BigDecimal("500.00"), AdsRatioMethod.BY_REVENUE);
        when(campaignRepository.findByStoreAndWeek(STORE_ID, WEEK)).thenReturn(Optional.of(campaign));
        when(saleEntryUseCase.findByStoreAndWeek(STORE_ID, WEEK)).thenReturn(List.of(entry1, entry2));

        // update devolve a própria entry (mock)
        when(saleEntryUseCase.update(eq(e1), any())).thenReturn(entry1);
        when(saleEntryUseCase.update(eq(e2), any())).thenReturn(entry2);

        List<SaleEntry> result = useCase.distributeAds(STORE_ID, WEEK);

        assertThat(result).hasSize(2);

        // Verifica os adsCost passados nos comandos de update
        var captor = ArgumentCaptor.forClass(UpdateSaleEntryCommand.class);
        verify(saleEntryUseCase).update(eq(e1), captor.capture());
        assertThat(captor.getValue().adsCost()).isEqualByComparingTo("300.00"); // 300/500 × 500

        verify(saleEntryUseCase).update(eq(e2), captor.capture());
        assertThat(captor.getValue().adsCost()).isEqualByComparingTo("200.00"); // 200/500 × 500
    }

    @Test
    @DisplayName("distributeAds BY_UNITS distribui proporcionalmente às unidades")
    void distributeAdsByUnits() {
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();
        // e1: 3 unidades, e2: 7 unidades → 30% / 70% de 100
        SaleEntry entry1 = stubSaleEntry(e1, new BigDecimal("150.00"), 3);
        SaleEntry entry2 = stubSaleEntry(e2, new BigDecimal("350.00"), 7);

        WeeklyAdsCampaign campaign = WeeklyAdsCampaign.create(
                STORE_ID, WEEK.year(), WEEK.weekNumber(),
                new BigDecimal("100.00"), AdsRatioMethod.BY_UNITS);
        when(campaignRepository.findByStoreAndWeek(STORE_ID, WEEK)).thenReturn(Optional.of(campaign));
        when(saleEntryUseCase.findByStoreAndWeek(STORE_ID, WEEK)).thenReturn(List.of(entry1, entry2));
        when(saleEntryUseCase.update(eq(e1), any())).thenReturn(entry1);
        when(saleEntryUseCase.update(eq(e2), any())).thenReturn(entry2);

        useCase.distributeAds(STORE_ID, WEEK);

        var captor = ArgumentCaptor.forClass(UpdateSaleEntryCommand.class);
        verify(saleEntryUseCase).update(eq(e1), captor.capture());
        assertThat(captor.getValue().adsCost()).isEqualByComparingTo("30.00"); // 3/10 × 100

        verify(saleEntryUseCase).update(eq(e2), captor.capture());
        assertThat(captor.getValue().adsCost()).isEqualByComparingTo("70.00"); // 7/10 × 100
    }

    @Test
    @DisplayName("distributeAds retorna lista vazia quando não há sale_entries")
    void distributeAdsNoEntries() {
        WeeklyAdsCampaign campaign = WeeklyAdsCampaign.create(
                STORE_ID, WEEK.year(), WEEK.weekNumber(),
                new BigDecimal("500.00"), AdsRatioMethod.BY_REVENUE);
        when(campaignRepository.findByStoreAndWeek(STORE_ID, WEEK)).thenReturn(Optional.of(campaign));
        when(saleEntryUseCase.findByStoreAndWeek(STORE_ID, WEEK)).thenReturn(List.of());

        List<SaleEntry> result = useCase.distributeAds(STORE_ID, WEEK);

        assertThat(result).isEmpty();
        verify(saleEntryUseCase, never()).update(any(), any());
    }

    @Test
    @DisplayName("distributeAds lança CampaignNotFoundException quando campanha não existe")
    void distributeAdsCampaignNotFound() {
        when(campaignRepository.findByStoreAndWeek(STORE_ID, WEEK)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.distributeAds(STORE_ID, WEEK))
                .isInstanceOf(WeeklyAdsCampaignUseCaseImpl.CampaignNotFoundException.class);
    }
}
