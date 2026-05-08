package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.out.ListingRepository;
import br.com.valls.moneycontrol.application.ports.out.MarketplaceFeeRuleRepository;
import br.com.valls.moneycontrol.application.ports.out.ProductPurchaseEntryRepository;
import br.com.valls.moneycontrol.application.ports.out.SaleEntryRepository;
import br.com.valls.moneycontrol.application.ports.out.TaxConfigRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterSaleEntryCommand;
import br.com.valls.moneycontrol.domain.enums.AdsRatioMode;
import br.com.valls.moneycontrol.domain.enums.CalculationBase;
import br.com.valls.moneycontrol.domain.enums.ChargeMoment;
import br.com.valls.moneycontrol.domain.enums.ListingStatus;
import br.com.valls.moneycontrol.domain.enums.StoreStatus;
import br.com.valls.moneycontrol.domain.exception.DuplicateSaleEntryException;
import br.com.valls.moneycontrol.domain.model.Listing;
import br.com.valls.moneycontrol.domain.model.MarketplaceFeeRule;
import br.com.valls.moneycontrol.domain.model.SaleEntry;
import br.com.valls.moneycontrol.domain.model.TaxConfig;
import br.com.valls.moneycontrol.domain.model.WeekReference;
import br.com.valls.moneycontrol.domain.service.FeeCalculatorService;
import br.com.valls.moneycontrol.domain.service.ProfitCalculatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SaleEntryUseCaseImpl")
class SaleEntryUseCaseImplTest {

    @Mock SaleEntryRepository            saleEntryRepository;
    @Mock ListingRepository              listingRepository;
    @Mock TaxConfigRepository            taxConfigRepository;
    @Mock MarketplaceFeeRuleRepository   feeRuleRepository;
    @Mock ProductPurchaseEntryRepository purchaseRepository;
    @Mock ApplicationEventPublisher      eventPublisher;

    SaleEntryUseCaseImpl useCase;

    private static final UUID      COMPANY_ID = UUID.randomUUID();
    private static final UUID      STORE_ID   = UUID.randomUUID();
    private static final UUID      MP_ID      = UUID.randomUUID();
    private static final UUID      LISTING_ID = UUID.randomUUID();
    private static final UUID      PRODUCT_ID = UUID.randomUUID();
    private static final WeekReference WEEK   = WeekReference.of(LocalDate.of(2025, 6, 16));

    @BeforeEach
    void setUp() {
        FeeCalculatorService feeCalc = new FeeCalculatorService();
        useCase = new SaleEntryUseCaseImpl(
                saleEntryRepository, listingRepository, taxConfigRepository,
                feeRuleRepository, purchaseRepository,
                feeCalc, new ProfitCalculatorService(feeCalc), eventPublisher);
    }

    private Listing stubListing(ListingStatus status, BigDecimal price) {
        return Listing.reconstitute(LISTING_ID, PRODUCT_ID, STORE_ID, MP_ID, COMPANY_ID,
                null, "Título", price, null, status,
                null, null, status == ListingStatus.ACTIVE,
                Instant.now(), Instant.now());
    }

    private TaxConfig stubTaxConfig() {
        return TaxConfig.create(COMPANY_ID, ChargeMoment.ON_SALE,
                CalculationBase.GROSS_REVENUE, new BigDecimal("6.00"),
                LocalDate.of(2025, 1, 1));
    }

    private MarketplaceFeeRule stubFeeRule() {
        return MarketplaceFeeRule.create(MP_ID, BigDecimal.ZERO, null,
                new BigDecimal("16.00"), BigDecimal.ZERO,
                LocalDate.of(2025, 1, 1), 1);
    }

    private RegisterSaleEntryCommand cmd(int units) {
        return new RegisterSaleEntryCommand(STORE_ID, LISTING_ID, WEEK,
                units, null, null, null, null, null, null);
    }

    private void setupHappyPath(BigDecimal price) {
        when(listingRepository.findById(LISTING_ID))
                .thenReturn(Optional.of(stubListing(ListingStatus.ACTIVE, price)));
        when(saleEntryRepository.existsByListingAndWeek(LISTING_ID, WEEK))
                .thenReturn(false);
        when(purchaseRepository.findMostRecentBeforeWeek(PRODUCT_ID, WEEK))
                .thenReturn(Optional.empty()); // sem compra → custo 0
        when(taxConfigRepository.findActiveAt(eq(COMPANY_ID), any()))
                .thenReturn(Optional.of(stubTaxConfig()));
        when(feeRuleRepository.findActiveAt(eq(MP_ID), any()))
                .thenReturn(List.of(stubFeeRule()));
        when(saleEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── register — cenário principal ─────────────────────────────────────────

    @Test
    @DisplayName("register captura sale_price_snapshot do listing no momento do registro")
    void registerSnapshotsPrice() {
        setupHappyPath(new BigDecimal("50.00"));

        SaleEntry result = useCase.register(cmd(10));

        assertThat(result.getSalePriceSnapshot()).isEqualByComparingTo("50.00");
        assertThat(result.getUnitsSold()).isEqualTo(10);
    }

    @Test
    @DisplayName("register calcula grossRevenue = salePrice × unitsSold")
    void registerCalculatesGrossRevenue() {
        setupHappyPath(new BigDecimal("50.00"));

        SaleEntry result = useCase.register(cmd(10));

        // 10 × 50 = 500
        assertThat(result.getGrossRevenue()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("register calcula campos derivados completos: fee=80, tax=30, netProfit=90")
    void registerFullCalculation() {
        setupHappyPath(new BigDecimal("50.00"));
        // unit_cost = 30
        var purchaseEntry = br.com.valls.moneycontrol.domain.model.ProductPurchaseEntry.create(
                COMPANY_ID, PRODUCT_ID, 2025, 20, 100,
                new BigDecimal("30.00"), false, BigDecimal.ZERO);
        when(purchaseRepository.findMostRecentBeforeWeek(PRODUCT_ID, WEEK))
                .thenReturn(Optional.of(purchaseEntry));

        SaleEntry result = useCase.register(cmd(10));

        assertThat(result.getGrossRevenue()).isEqualByComparingTo("500.00");   // 10×50
        assertThat(result.getMarketplaceFeeTotal()).isEqualByComparingTo("80.00"); // 10×(50×16%)
        assertThat(result.getTaxAmount()).isEqualByComparingTo("30.00");       // 500×6%
        assertThat(result.getTotalCogs()).isEqualByComparingTo("300.00");      // 10×30
        assertThat(result.getGrossProfit()).isEqualByComparingTo("120.00");    // 500-80-300
        assertThat(result.getNetProfit()).isEqualByComparingTo("90.00");       // 120-30
    }

    @Test
    @DisplayName("register captura fee_percentage_snapshot e fee_rule_id")
    void registerSnapshotsFee() {
        // setupHappyPath já configura feeRuleRepository com stubFeeRule()
        // capturamos a regra antes de chamar setup para verificar o ID
        setupHappyPath(new BigDecimal("50.00"));

        SaleEntry result = useCase.register(cmd(5));

        assertThat(result.getFeePercentageSnapshot()).isEqualByComparingTo("16.00");
        assertThat(result.getFeeRuleId()).isNotNull();
    }

    @Test
    @DisplayName("register publica SaleEntryAuditEvent CREATE")
    void registerPublishesAuditEvent() {
        setupHappyPath(new BigDecimal("50.00"));

        useCase.register(cmd(1));

        verify(eventPublisher).publishEvent(
                argThat(evt -> evt instanceof SaleEntryUseCaseImpl.SaleEntryAuditEvent sae
                        && "CREATE".equals(sae.getAction())));
    }

    // ── validações ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register lança DuplicateSaleEntryException para listing duplicado na semana")
    void registerDuplicate() {
        when(listingRepository.findById(LISTING_ID))
                .thenReturn(Optional.of(stubListing(ListingStatus.ACTIVE, new BigDecimal("50.00"))));
        when(saleEntryRepository.existsByListingAndWeek(LISTING_ID, WEEK)).thenReturn(true);

        assertThatThrownBy(() -> useCase.register(cmd(1)))
                .isInstanceOf(DuplicateSaleEntryException.class);
    }

    @Test
    @DisplayName("register lança InactivListingException para listing PAUSED")
    void registerPausedListing() {
        when(listingRepository.findById(LISTING_ID))
                .thenReturn(Optional.of(stubListing(ListingStatus.PAUSED, new BigDecimal("50.00"))));

        assertThatThrownBy(() -> useCase.register(cmd(1)))
                .isInstanceOf(SaleEntryUseCaseImpl.InactivListingException.class);
    }

    @Test
    @DisplayName("register lança IllegalArgumentException para unitsSold = 0")
    void registerZeroUnits() {
        assertThatThrownBy(() -> useCase.register(cmd(0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unitsSold");
    }

    @Test
    @DisplayName("register lança IllegalArgumentException para adsCost negativo")
    void registerNegativeAdsCost() {
        assertThatThrownBy(() -> useCase.register(
                new RegisterSaleEntryCommand(STORE_ID, LISTING_ID, WEEK,
                        1, new BigDecimal("-1.00"), null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── findByStoreAndWeek ────────────────────────────────────────────────────

    @Test
    @DisplayName("findByStoreAndWeek retorna lista imutável")
    void findByStoreAndWeekImmutable() {
        when(saleEntryRepository.findByStoreAndWeek(STORE_ID, WEEK)).thenReturn(List.of());

        List<SaleEntry> result = useCase.findByStoreAndWeek(STORE_ID, WEEK);

        assertThatThrownBy(() -> result.add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
