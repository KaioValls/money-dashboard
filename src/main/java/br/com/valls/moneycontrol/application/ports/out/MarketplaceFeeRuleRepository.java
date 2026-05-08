package br.com.valls.moneycontrol.application.ports.out;

import br.com.valls.moneycontrol.domain.model.MarketplaceFeeRule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de saída para persistência de regras de comissão de marketplace.
 */
public interface MarketplaceFeeRuleRepository {

    /** Persiste ou atualiza uma regra de comissão. */
    MarketplaceFeeRule save(MarketplaceFeeRule rule);

    /** Busca regra pelo ID. */
    Optional<MarketplaceFeeRule> findById(UUID id);

    /** Retorna todas as regras de um marketplace (ativas e inativas). */
    List<MarketplaceFeeRule> findAllByMarketplaceId(UUID marketplaceId);

    /**
     * Retorna as regras ativas do marketplace vigentes na data informada.
     * Usado para validar sobreposição e para busca pelo FeeCalculatorService.
     */
    List<MarketplaceFeeRule> findActiveAt(UUID marketplaceId, LocalDate date);
}
