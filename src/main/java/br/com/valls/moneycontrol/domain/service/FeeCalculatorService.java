package br.com.valls.moneycontrol.domain.service;

import br.com.valls.moneycontrol.domain.exception.FeeRuleGapException;
import br.com.valls.moneycontrol.domain.model.FeeResult;
import br.com.valls.moneycontrol.domain.model.MarketplaceFeeRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Serviço de domínio responsável por calcular a comissão do marketplace por unidade vendida.
 *
 * <p>Seleciona a regra vigente na data, aplicável ao preço e com maior prioridade
 * (maior valor do campo {@code priority}). Caso nenhuma regra seja encontrada,
 * lança {@link FeeRuleGapException}.</p>
 *
 * <p>Fórmula: {@code totalFeeAmount = (salePrice × feePercentage / 100) + fixedFee}</p>
 *
 * <p>Resultado com escala 4 (HALF_UP).</p>
 */
public class FeeCalculatorService {

    /**
     * Calcula a comissão por unidade para o preço e data informados.
     *
     * @param salePrice preço unitário de venda
     * @param rules     lista de regras de comissão do marketplace
     * @param date      data da venda (para checar vigência da regra)
     * @return {@link FeeResult} com snapshot dos parâmetros e o valor calculado por unidade
     * @throws FeeRuleGapException se nenhuma regra aplicável for encontrada
     */
    public FeeResult calculate(BigDecimal salePrice, List<MarketplaceFeeRule> rules, LocalDate date) {
        Objects.requireNonNull(salePrice, "salePrice é obrigatório");
        Objects.requireNonNull(rules,     "rules é obrigatório");
        Objects.requireNonNull(date,      "date é obrigatório");

        MarketplaceFeeRule rule = rules.stream()
                .filter(r -> r.isActive() && r.appliesTo(salePrice) && r.isActiveAt(date))
                .max(Comparator.comparingInt(MarketplaceFeeRule::getPriority))
                .orElseThrow(() -> new FeeRuleGapException(salePrice, date));

        // feePercentage armazenado como percentual (ex.: 16.00 = 16%)
        BigDecimal rate = rule.getFeePercentage()
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        BigDecimal totalFeeAmount = salePrice.multiply(rate)
                .add(rule.getFixedFee())
                .setScale(4, RoundingMode.HALF_UP);

        return new FeeResult(
                rule.getId(),
                rule.getFeePercentage(),
                rule.getFixedFee(),
                totalFeeAmount
        );
    }
}
