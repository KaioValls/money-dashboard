package br.com.valls.moneycontrol.application.usecases.command;

import java.math.BigDecimal;

/**
 * Comando de atualização de lançamento de venda.
 * Somente custos operacionais podem ser alterados — snapshots e campos calculados
 * são recalculados pelo use case após a atualização.
 * Campos {@code null} indicam que o valor não deve ser alterado.
 */
public record UpdateSaleEntryCommand(
        BigDecimal adsCost,
        BigDecimal couponDiscount,
        BigDecimal freightCost,
        BigDecimal returnAmount,
        BigDecimal otherCosts,
        String     otherCostsDescription
) {}
