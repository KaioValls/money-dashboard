package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.in.SaleEntryUseCase;
import br.com.valls.moneycontrol.application.ports.out.ListingRepository;
import br.com.valls.moneycontrol.application.ports.out.MarketplaceFeeRuleRepository;
import br.com.valls.moneycontrol.application.ports.out.ProductPurchaseEntryRepository;
import br.com.valls.moneycontrol.application.ports.out.SaleEntryRepository;
import br.com.valls.moneycontrol.application.ports.out.TaxConfigRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterSaleEntryCommand;
import br.com.valls.moneycontrol.application.usecases.command.UpdateSaleEntryCommand;
import br.com.valls.moneycontrol.domain.enums.ListingStatus;
import br.com.valls.moneycontrol.domain.exception.BusinessException;
import br.com.valls.moneycontrol.domain.exception.DuplicateSaleEntryException;
import br.com.valls.moneycontrol.domain.exception.EntityNotFoundException;
import br.com.valls.moneycontrol.domain.exception.ListingNotFoundException;
import br.com.valls.moneycontrol.domain.exception.TaxConfigNotFoundException;
import br.com.valls.moneycontrol.domain.model.FeeResult;
import br.com.valls.moneycontrol.domain.model.Listing;
import br.com.valls.moneycontrol.domain.model.MarketplaceFeeRule;
import br.com.valls.moneycontrol.domain.model.ProductPurchaseEntry;
import br.com.valls.moneycontrol.domain.model.SaleEntry;
import br.com.valls.moneycontrol.domain.model.SaleEntryInput;
import br.com.valls.moneycontrol.domain.model.TaxConfig;
import br.com.valls.moneycontrol.domain.model.WeekReference;
import br.com.valls.moneycontrol.domain.model.WeeklyResult;
import br.com.valls.moneycontrol.domain.service.FeeCalculatorService;
import br.com.valls.moneycontrol.domain.service.ProfitCalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Implementação do use case de lançamentos de venda semanal.
 * Instanciada exclusivamente em {@code BeanConfig} — sem @Service.
 *
 * <p>Orquestração do registro:
 * <ol>
 *   <li>Valida listing ACTIVE e não duplicado na semana</li>
 *   <li>Captura sale_price_snapshot do listing</li>
 *   <li>Captura unit_cost_snapshot da compra mais recente anterior à semana</li>
 *   <li>Captura tax_rate_snapshot da config tributária vigente</li>
 *   <li>Captura fee snapshots via FeeCalculatorService</li>
 *   <li>Calcula todos os campos derivados via ProfitCalculatorService</li>
 *   <li>Persiste SaleEntry e publica AuditEvent</li>
 * </ol>
 */
@RequiredArgsConstructor
public class SaleEntryUseCaseImpl implements SaleEntryUseCase {

    private final SaleEntryRepository            saleEntryRepository;
    private final ListingRepository              listingRepository;
    private final TaxConfigRepository            taxConfigRepository;
    private final MarketplaceFeeRuleRepository   feeRuleRepository;
    private final ProductPurchaseEntryRepository purchaseRepository;
    private final FeeCalculatorService           feeCalculatorService;
    private final ProfitCalculatorService        profitCalculatorService;
    private final ApplicationEventPublisher      eventPublisher;

    @Override
    public SaleEntry register(RegisterSaleEntryCommand command) {
        validateOperationalCosts(command.adsCost(), command.couponDiscount(),
                command.freightCost(), command.returnAmount(), command.otherCosts());

        if (command.unitsSold() < 1) {
            throw new IllegalArgumentException("unitsSold deve ser >= 1");
        }

        // 1) Buscar e validar listing
        Listing listing = listingRepository.findById(command.listingId())
                .orElseThrow(() -> new ListingNotFoundException(command.listingId()));

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new InactivListingException(
                    "Anúncio não está ATIVO: " + command.listingId());
        }

        // 2) Verificar duplicata
        if (saleEntryRepository.existsByListingAndWeek(command.listingId(), command.weekReference())) {
            throw new DuplicateSaleEntryException(
                    "Já existe lançamento para este anúncio na semana: "
                    + command.weekReference());
        }

        LocalDate saleDate = command.weekReference().startDate();

        // 3) unit_cost_snapshot — compra mais recente anterior à semana
        BigDecimal unitCost = purchaseRepository
                .findMostRecentBeforeWeek(listing.getProductId(), command.weekReference())
                .map(ProductPurchaseEntry::getUnitCost)
                .orElse(BigDecimal.ZERO);

        // 4) TaxConfig vigente
        TaxConfig taxConfig = taxConfigRepository.findActiveAt(listing.getCompanyId(), saleDate)
                .orElseThrow(() -> new TaxConfigNotFoundException(
                        "Nenhuma configuração tributária ativa para a empresa em " + saleDate));

        // 5) Fee snapshots
        List<MarketplaceFeeRule> feeRules =
                feeRuleRepository.findActiveAt(listing.getMarketplaceId(), saleDate);

        FeeResult feeResult = feeCalculatorService.calculate(
                listing.getSalePrice(), feeRules, saleDate);

        // 6) Calcular todos os campos derivados
        SaleEntryInput input = new SaleEntryInput(
                listing.getStoreId(),
                command.weekReference(),
                saleDate,
                listing.getSalePrice(),
                command.unitsSold(),
                unitCost,
                feeRules,
                taxConfig,
                orZero(command.adsCost()),
                orZero(command.couponDiscount()),
                orZero(command.freightCost()),
                orZero(command.returnAmount()),
                orZero(command.otherCosts())
        );

        WeeklyResult result = profitCalculatorService.calculate(input);

        // 7) Criar e persistir SaleEntry
        int dateKey = toDateKey(saleDate);
        SaleEntry entry = SaleEntry.builder()
                .companyId(listing.getCompanyId())
                .storeId(listing.getStoreId())
                .listingId(command.listingId())
                .marketplaceId(listing.getMarketplaceId())
                .taxConfigId(taxConfig.getId())
                .feeRuleId(feeResult.feeRuleId())
                .dateKey(dateKey)
                .isoYear(command.weekReference().year())
                .isoWeekNumber(command.weekReference().weekNumber())
                .unitsSold(command.unitsSold())
                .salePriceSnapshot(listing.getSalePrice())
                .unitCostSnapshot(unitCost)
                .taxRateSnapshot(taxConfig.getRatePercentage())
                .taxChargeMomentSnapshot(taxConfig.getChargeMoment())
                .feePercentageSnapshot(feeResult.feePercentageSnapshot())
                .fixedFeeSnapshot(feeResult.fixedFeeSnapshot())
                .adsCost(orZero(command.adsCost()))
                .couponDiscount(orZero(command.couponDiscount()))
                .freightCost(orZero(command.freightCost()))
                .returnAmount(orZero(command.returnAmount()))
                .otherCosts(orZero(command.otherCosts()))
                .otherCostsDescription(command.otherCostsDescription())
                .grossRevenue(result.grossRevenue())
                .totalCogs(result.totalCogs())
                .marketplaceFeeTotal(result.marketplaceFeeTotal())
                .taxAmount(result.taxAmount())
                .grossProfit(result.grossProfit())
                .netProfit(result.netProfit())
                .netMarginPercentage(result.netMarginPercentage())
                .roiPercentage(result.roiPercentage())
                .build();

        SaleEntry saved = saleEntryRepository.save(entry);

        // 8) Publicar evento de auditoria (T7.2 implementa o listener)
        eventPublisher.publishEvent(new SaleEntryAuditEvent(this, saved, null, "CREATE"));

        return saved;
    }

    @Override
    public SaleEntry update(UUID id, UpdateSaleEntryCommand command) {
        SaleEntry entry = saleEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SaleEntry não encontrado: " + id) {});

        SaleEntry before = snapshotFor(entry);

        // Recalcular com snapshots imutáveis e novos custos operacionais
        LocalDate saleDate = entry.weekReference().startDate();
        TaxConfig taxConfig = taxConfigRepository.findById(entry.getTaxConfigId())
                .orElseThrow(() -> new TaxConfigNotFoundException(entry.getTaxConfigId()));

        List<MarketplaceFeeRule> feeRules =
                feeRuleRepository.findActiveAt(entry.getMarketplaceId(), saleDate);

        SaleEntryInput input = new SaleEntryInput(
                entry.getStoreId(),
                entry.weekReference(),
                saleDate,
                entry.getSalePriceSnapshot(),
                entry.getUnitsSold(),
                entry.getUnitCostSnapshot(),
                feeRules,
                taxConfig,
                orZero(command.adsCost(),       entry.getAdsCost()),
                orZero(command.couponDiscount(), entry.getCouponDiscount()),
                orZero(command.freightCost(),    entry.getFreightCost()),
                orZero(command.returnAmount(),   entry.getReturnAmount()),
                orZero(command.otherCosts(),     entry.getOtherCosts())
        );

        WeeklyResult recalc = profitCalculatorService.calculate(input);

        entry.updateOperationalCosts(
                input.adsCost(), input.couponDiscount(), input.freightCost(),
                input.returnAmount(), input.otherCosts(),
                command.otherCostsDescription() != null
                        ? command.otherCostsDescription()
                        : entry.getOtherCostsDescription(),
                recalc.grossProfit(), recalc.netProfit(),
                recalc.netMarginPercentage(), recalc.roiPercentage()
        );

        SaleEntry saved = saleEntryRepository.save(entry);
        eventPublisher.publishEvent(new SaleEntryAuditEvent(this, saved, before, "UPDATE"));
        return saved;
    }

    @Override
    public void delete(UUID id) {
        SaleEntry entry = saleEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SaleEntry não encontrado: " + id) {});
        saleEntryRepository.deleteById(id);
        eventPublisher.publishEvent(new SaleEntryAuditEvent(this, null, entry, "DELETE"));
    }

    @Override
    public List<SaleEntry> findByStoreAndWeek(UUID storeId, WeekReference week) {
        return List.copyOf(saleEntryRepository.findByStoreAndWeek(storeId, week));
    }

    @Override
    public List<SaleEntry> findByListingAndWeek(UUID listingId, WeekReference week) {
        return List.copyOf(saleEntryRepository.findByListingAndWeek(listingId, week));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /** Para update: se command field é null, mantém valor existente. */
    private static BigDecimal orZero(BigDecimal commandValue, BigDecimal existingValue) {
        return commandValue != null ? commandValue : existingValue;
    }

    private static int toDateKey(LocalDate date) {
        return date.getYear() * 10000 + date.getMonthValue() * 100 + date.getDayOfMonth();
    }

    private static void validateOperationalCosts(BigDecimal... costs) {
        for (BigDecimal cost : costs) {
            if (cost != null && cost.signum() < 0) {
                throw new IllegalArgumentException("Custos operacionais não podem ser negativos");
            }
        }
    }

    /** Snapshot raso do entry para auditoria (apenas campos calculados relevantes). */
    private static SaleEntry snapshotFor(SaleEntry entry) {
        return SaleEntry.builder()
                .id(entry.getId())
                .adsCost(entry.getAdsCost())
                .couponDiscount(entry.getCouponDiscount())
                .freightCost(entry.getFreightCost())
                .returnAmount(entry.getReturnAmount())
                .otherCosts(entry.getOtherCosts())
                .grossProfit(entry.getGrossProfit())
                .netProfit(entry.getNetProfit())
                .build();
    }

    // ── inner types ───────────────────────────────────────────────────────────

    public static class InactivListingException extends BusinessException {
        public InactivListingException(String message) { super(message); }
    }

    /**
     * Evento de auditoria publicado após cada operação sobre SaleEntry.
     * O listener em T7.2 persiste em audit_logs.
     */
    public static class SaleEntryAuditEvent extends org.springframework.context.ApplicationEvent {
        private final SaleEntry current;
        private final SaleEntry previous;
        private final String    action;

        public SaleEntryAuditEvent(Object source, SaleEntry current,
                                    SaleEntry previous, String action) {
            super(source);
            this.current  = current;
            this.previous = previous;
            this.action   = action;
        }

        public SaleEntry getCurrent()  { return current; }
        public SaleEntry getPrevious() { return previous; }
        public String    getAction()   { return action; }
    }
}
