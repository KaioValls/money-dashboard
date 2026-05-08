package br.com.valls.moneycontrol.application.usecases.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Comando de criação de regra de comissão de marketplace.
 *
 * @param marketplaceId  marketplace ao qual a regra pertence
 * @param minPrice       preço mínimo da faixa (inclusive)
 * @param maxPrice       preço máximo da faixa (inclusive; null = sem limite)
 * @param feePercentage  percentual de comissão (ex.: 16.00 = 16%)
 * @param fixedFee       taxa fixa por transação (0 se não houver)
 * @param validFrom      início da vigência
 * @param priority       prioridade de seleção (maior número = maior prioridade)
 */
public record RegisterFeeRuleCommand(
        UUID       marketplaceId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal feePercentage,
        BigDecimal fixedFee,
        LocalDate  validFrom,
        int        priority
) {}
