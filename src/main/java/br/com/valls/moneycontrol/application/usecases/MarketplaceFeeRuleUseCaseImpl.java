package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.in.MarketplaceFeeRuleUseCase;
import br.com.valls.moneycontrol.application.ports.out.MarketplaceFeeRuleRepository;
import br.com.valls.moneycontrol.application.ports.out.MarketplaceRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterFeeRuleCommand;
import br.com.valls.moneycontrol.domain.exception.FeeRuleOverlapException;
import br.com.valls.moneycontrol.domain.exception.MarketplaceNotFoundException;
import br.com.valls.moneycontrol.domain.model.FeeResult;
import br.com.valls.moneycontrol.domain.model.MarketplaceFeeRule;
import br.com.valls.moneycontrol.domain.service.FeeCalculatorService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação do use case de regras de comissão de marketplace.
 * Instanciada exclusivamente em {@code BeanConfig} — sem @Service.
 *
 * <p>Validação de sobreposição: antes de salvar, verifica se alguma regra ativa
 * do marketplace conflita na faixa de preço (min/max). A sobreposição ocorre quando
 * as faixas [A.min, A.max] e [B.min, B.max] se intersectam.</p>
 */
@RequiredArgsConstructor
public class MarketplaceFeeRuleUseCaseImpl implements MarketplaceFeeRuleUseCase {

    private final MarketplaceFeeRuleRepository feeRuleRepository;
    private final MarketplaceRepository        marketplaceRepository;
    private final FeeCalculatorService         feeCalculatorService;

    @Override
    public MarketplaceFeeRule createRule(RegisterFeeRuleCommand command) {
        marketplaceRepository.findById(command.marketplaceId())
                .orElseThrow(() -> new MarketplaceNotFoundException(command.marketplaceId()));

        // Buscar regras ativas na data de início da nova regra
        List<MarketplaceFeeRule> activeRules =
                feeRuleRepository.findActiveAt(command.marketplaceId(), command.validFrom());

        // Validar sobreposição com cada regra existente
        for (MarketplaceFeeRule existing : activeRules) {
            if (overlaps(command.minPrice(), command.maxPrice(),
                         existing.getMinPrice(), existing.getMaxPrice())) {
                throw new FeeRuleOverlapException(
                        "Faixa de preço conflita com regra existente (id=" + existing.getId()
                        + ", min=" + existing.getMinPrice()
                        + ", max=" + (existing.getMaxPrice() != null ? existing.getMaxPrice() : "∞") + ")");
            }
        }

        MarketplaceFeeRule rule = MarketplaceFeeRule.create(
                command.marketplaceId(),
                command.minPrice(),
                command.maxPrice(),
                command.feePercentage(),
                command.fixedFee(),
                command.validFrom(),
                command.priority()
        );
        return feeRuleRepository.save(rule);
    }

    @Override
    public FeeResult findApplicableRule(UUID marketplaceId, BigDecimal salePrice, LocalDate date) {
        List<MarketplaceFeeRule> rules = feeRuleRepository.findActiveAt(marketplaceId, date);
        return feeCalculatorService.calculate(salePrice, rules, date);
    }

    @Override
    public List<MarketplaceFeeRule> findAllByMarketplace(UUID marketplaceId) {
        return List.copyOf(feeRuleRepository.findAllByMarketplaceId(marketplaceId));
    }

    @Override
    public void deactivate(UUID id) {
        // findById ou lançar EntityNotFoundException
        feeRuleRepository.findById(id).ifPresent(rule -> {
            rule.deactivate();
            feeRuleRepository.save(rule);
        });
    }

    // ── helper: detecta sobreposição entre duas faixas [a1,a2] e [b1,b2] ─────
    // null em a2 ou b2 significa sem limite superior (infinito)
    private static boolean overlaps(BigDecimal a1, BigDecimal a2,
                                     BigDecimal b1, BigDecimal b2) {
        // Não sobrepõe apenas se: a2 < b1 OU b2 < a1
        // Com null (infinito) nunca termina antes do início do outro
        boolean aEndsBeforeB = a2 != null && a2.compareTo(b1) < 0;
        boolean bEndsBeforeA = b2 != null && b2.compareTo(a1) < 0;
        return !aEndsBeforeB && !bEndsBeforeA;
    }
}
