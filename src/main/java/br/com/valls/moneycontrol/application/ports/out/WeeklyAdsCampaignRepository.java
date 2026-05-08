package br.com.valls.moneycontrol.application.ports.out;

import br.com.valls.moneycontrol.domain.model.WeeklyAdsCampaign;
import br.com.valls.moneycontrol.domain.model.WeekReference;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de saída para persistência de campanhas de ADS semanais.
 */
public interface WeeklyAdsCampaignRepository {

    /** Persiste ou atualiza uma campanha e retorna a instância salva. */
    WeeklyAdsCampaign save(WeeklyAdsCampaign campaign);

    /** Busca pelo ID. */
    Optional<WeeklyAdsCampaign> findById(UUID id);

    /**
     * Busca a campanha de uma loja em uma semana.
     * Implementa a constraint UNIQUE(store_id, iso_year, iso_week_number).
     */
    Optional<WeeklyAdsCampaign> findByStoreAndWeek(UUID storeId, WeekReference week);

    /** Retorna todas as campanhas de uma loja, ordenadas por ano/semana descendente. */
    List<WeeklyAdsCampaign> findByStoreId(UUID storeId);
}
