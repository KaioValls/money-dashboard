package br.com.valls.moneycontrol.application.usecases.command;

import br.com.valls.moneycontrol.domain.model.WeekReference;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando de registro de lançamento de venda semanal.
 * Snapshots e campos calculados são resolvidos pelo use case — não vêm do cliente.
 *
 * @param storeId              loja do lançamento
 * @param listingId            anúncio associado
 * @param weekReference        semana de referência
 * @param unitsSold            unidades vendidas (>= 1)
 * @param adsCost              custo com anúncios (>= 0; null = 0)
 * @param couponDiscount       desconto de cupom (>= 0; null = 0)
 * @param freightCost          custo de frete (>= 0; null = 0)
 * @param returnAmount         valor de devoluções (>= 0; null = 0)
 * @param otherCosts           outros custos (>= 0; null = 0)
 * @param otherCostsDescription descrição dos outros custos (opcional)
 */
public record RegisterSaleEntryCommand(
        UUID          storeId,
        UUID          listingId,
        WeekReference weekReference,
        int           unitsSold,
        BigDecimal    adsCost,
        BigDecimal    couponDiscount,
        BigDecimal    freightCost,
        BigDecimal    returnAmount,
        BigDecimal    otherCosts,
        String        otherCostsDescription
) {}
