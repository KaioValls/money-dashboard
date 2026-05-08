package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.in.SaleEntryUseCase;
import br.com.valls.moneycontrol.application.ports.in.WeeklyAdsCampaignUseCase;
import br.com.valls.moneycontrol.application.ports.out.StoreRepository;
import br.com.valls.moneycontrol.application.ports.out.WeeklyAdsCampaignRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterWeeklyAdsCampaignCommand;
import br.com.valls.moneycontrol.application.usecases.command.UpdateSaleEntryCommand;
import br.com.valls.moneycontrol.domain.enums.AdsRatioMethod;
import br.com.valls.moneycontrol.domain.exception.BusinessException;
import br.com.valls.moneycontrol.domain.exception.EntityNotFoundException;
import br.com.valls.moneycontrol.domain.exception.StoreNotFoundException;
import br.com.valls.moneycontrol.domain.model.SaleEntry;
import br.com.valls.moneycontrol.domain.model.WeeklyAdsCampaign;
import br.com.valls.moneycontrol.domain.model.WeekReference;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação do use case de campanhas de ADS semanais.
 * Instanciada exclusivamente em {@code BeanConfig} — sem @Service.
 *
 * <p>{@link #registerCampaign} faz upsert: atualiza se já existe, cria se não existe.
 * <p>{@link #distributeAds} distribui o orçamento total proporcionalmente entre
 * os lançamentos de venda da semana e recalcula os campos derivados de cada um.
 */
@RequiredArgsConstructor
public class WeeklyAdsCampaignUseCaseImpl implements WeeklyAdsCampaignUseCase {

    private final WeeklyAdsCampaignRepository campaignRepository;
    private final StoreRepository             storeRepository;
    private final SaleEntryUseCase            saleEntryUseCase;

    @Override
    public WeeklyAdsCampaign registerCampaign(RegisterWeeklyAdsCampaignCommand command) {
        // Valida loja
        storeRepository.findById(command.storeId())
                .orElseThrow(() -> new StoreNotFoundException(command.storeId()));

        WeekReference week = new WeekReference(command.isoYear(), command.isoWeekNumber());

        Optional<WeeklyAdsCampaign> existing =
                campaignRepository.findByStoreAndWeek(command.storeId(), week);

        if (existing.isPresent()) {
            WeeklyAdsCampaign campaign = existing.get();
            campaign.update(command.totalAdsBudget(), command.ratioMethod());
            return campaignRepository.save(campaign);
        }

        WeeklyAdsCampaign campaign = WeeklyAdsCampaign.create(
                command.storeId(),
                command.isoYear(),
                command.isoWeekNumber(),
                command.totalAdsBudget(),
                command.ratioMethod()
        );
        return campaignRepository.save(campaign);
    }

    @Override
    public Optional<WeeklyAdsCampaign> findByStoreAndWeek(UUID storeId, WeekReference week) {
        return campaignRepository.findByStoreAndWeek(storeId, week);
    }

    @Override
    public List<SaleEntry> distributeAds(UUID storeId, WeekReference week) {
        WeeklyAdsCampaign campaign = campaignRepository.findByStoreAndWeek(storeId, week)
                .orElseThrow(() -> new CampaignNotFoundException(
                        "Campanha de ADS não encontrada para loja " + storeId
                        + " na semana " + week));

        List<SaleEntry> entries = saleEntryUseCase.findByStoreAndWeek(storeId, week);
        if (entries.isEmpty()) return List.of();

        List<SaleEntry> updated = new ArrayList<>(entries.size());

        if (campaign.getRatioMethod() == AdsRatioMethod.BY_REVENUE) {
            BigDecimal totalRevenue = entries.stream()
                    .map(SaleEntry::getGrossRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalRevenue.signum() == 0) {
                throw new IllegalStateException(
                        "Receita bruta total é zero — impossível ratear ADS por receita");
            }
            for (SaleEntry entry : entries) {
                BigDecimal share = entry.getGrossRevenue()
                        .divide(totalRevenue, 10, RoundingMode.HALF_UP)
                        .multiply(campaign.getTotalAdsBudget())
                        .setScale(2, RoundingMode.HALF_UP);
                updated.add(saleEntryUseCase.update(entry.getId(),
                        new UpdateSaleEntryCommand(share, null, null, null, null, null)));
            }
        } else { // BY_UNITS
            int totalUnits = entries.stream().mapToInt(SaleEntry::getUnitsSold).sum();
            if (totalUnits == 0) {
                throw new IllegalStateException(
                        "Total de unidades vendidas é zero — impossível ratear ADS por unidades");
            }
            for (SaleEntry entry : entries) {
                BigDecimal share = campaign.getTotalAdsBudget()
                        .multiply(BigDecimal.valueOf(entry.getUnitsSold()))
                        .divide(BigDecimal.valueOf(totalUnits), 2, RoundingMode.HALF_UP);
                updated.add(saleEntryUseCase.update(entry.getId(),
                        new UpdateSaleEntryCommand(share, null, null, null, null, null)));
            }
        }
        return List.copyOf(updated);
    }

    // ── inner types ───────────────────────────────────────────────────────────

    public static class CampaignNotFoundException extends BusinessException {
        public CampaignNotFoundException(String message) { super(message); }
    }
}
