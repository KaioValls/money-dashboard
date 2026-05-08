package br.com.valls.moneycontrol.application.ports.in;

import br.com.valls.moneycontrol.application.usecases.command.RegisterFeeRuleCommand;
import br.com.valls.moneycontrol.domain.model.FeeResult;
import br.com.valls.moneycontrol.domain.model.MarketplaceFeeRule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Port de entrada para gerenciamento de regras de comissão de marketplace.
 */
public interface MarketplaceFeeRuleUseCase {

    /**
     * Cria nova regra de comissão.
     * Valida sobreposição de faixa de preço com regras ativas do mesmo marketplace.
     * Lança {@code FeeRuleOverlapException} se houver conflito.
     */
    MarketplaceFeeRule createRule(RegisterFeeRuleCommand command);

    /**
     * Retorna o resultado de comissão aplicável ao preço e data informados,
     * delegando ao {@code FeeCalculatorService}.
     * Lança {@code FeeRuleGapException} se nenhuma regra se aplicar.
     */
    FeeResult findApplicableRule(UUID marketplaceId, BigDecimal salePrice, LocalDate date);

    /** Retorna todas as regras de comissão de um marketplace. */
    List<MarketplaceFeeRule> findAllByMarketplace(UUID marketplaceId);

    /** Desativa uma regra de comissão. */
    void deactivate(UUID id);
}
