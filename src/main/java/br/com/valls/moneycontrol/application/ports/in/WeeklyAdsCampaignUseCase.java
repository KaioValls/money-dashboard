package br.com.valls.moneycontrol.application.ports.in;

import br.com.valls.moneycontrol.application.usecases.command.RegisterWeeklyAdsCampaignCommand;
import br.com.valls.moneycontrol.domain.model.SaleEntry;
import br.com.valls.moneycontrol.domain.model.WeeklyAdsCampaign;
import br.com.valls.moneycontrol.domain.model.WeekReference;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de entrada para gerenciamento de campanhas de ADS semanais.
 */
public interface WeeklyAdsCampaignUseCase {

    /**
     * Registra ou atualiza (upsert) o orçamento de ADS de uma loja para uma semana.
     * Se já existir campanha para (storeId, isoYear, isoWeekNumber), ela é atualizada
     * com os novos valores; caso contrário é criada.
     */
    WeeklyAdsCampaign registerCampaign(RegisterWeeklyAdsCampaignCommand command);

    /**
     * Busca a campanha de uma loja em uma semana.
     */
    Optional<WeeklyAdsCampaign> findByStoreAndWeek(UUID storeId, WeekReference week);

    /**
     * Distribui proporcionalmente o orçamento total da campanha entre os
     * lançamentos de venda da semana, atualizando o campo {@code adsCost} de
     * cada {@link SaleEntry} e recalculando os campos derivados.
     *
     * <p>Método de rateio:
     * <ul>
     *   <li>{@code BY_REVENUE}: proporção da receita bruta de cada anúncio</li>
     *   <li>{@code BY_UNITS}: proporção de unidades vendidas de cada anúncio</li>
     * </ul>
     *
     * @return lista dos lançamentos atualizados com o adsCost distribuído
     * @throws CampaignNotFoundException se não houver campanha para a loja/semana
     * @throws IllegalStateException se a base de rateio for zero (sem vendas)
     */
    List<SaleEntry> distributeAds(UUID storeId, WeekReference week);
}
