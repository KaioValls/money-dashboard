package br.com.valls.moneycontrol.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Dado de entrada imutável para o cálculo de lucro de um lançamento de venda.
 *
 * <p>Todos os custos variáveis (adsCost, couponDiscount, freightCost, returnAmount, otherCosts)
 * são totais do período/entrada — não por unidade.</p>
 *
 * @param storeId          identificador da loja
 * @param weekReference    semana de referência do lançamento
 * @param saleDate         data da venda (usada para filtrar regras de comissão vigentes)
 * @param salePrice        preço de venda unitário
 * @param unitsSold        quantidade de unidades vendidas (>= 1)
 * @param unitCost         custo unitário do produto (CMV)
 * @param feeRules         regras de comissão do marketplace para este lançamento
 * @param taxConfig        configuração tributária vigente na data da venda
 * @param adsCost          custo total com anúncios (ADS) — usar ZERO se não houver
 * @param couponDiscount   desconto total concedido via cupom — usar ZERO se não houver
 * @param freightCost      custo total de frete — usar ZERO se não houver
 * @param returnAmount     valor total de devoluções — usar ZERO se não houver
 * @param otherCosts       outros custos operacionais — usar ZERO se não houver
 */
public record SaleEntryInput(
        UUID                    storeId,
        WeekReference           weekReference,
        LocalDate               saleDate,
        BigDecimal              salePrice,
        int                     unitsSold,
        BigDecimal              unitCost,
        List<MarketplaceFeeRule> feeRules,
        TaxConfig               taxConfig,
        BigDecimal              adsCost,
        BigDecimal              couponDiscount,
        BigDecimal              freightCost,
        BigDecimal              returnAmount,
        BigDecimal              otherCosts
) {
    public SaleEntryInput {
        Objects.requireNonNull(storeId,       "storeId é obrigatório");
        Objects.requireNonNull(weekReference, "weekReference é obrigatório");
        Objects.requireNonNull(saleDate,      "saleDate é obrigatório");
        Objects.requireNonNull(salePrice,     "salePrice é obrigatório");
        Objects.requireNonNull(unitCost,      "unitCost é obrigatório");
        Objects.requireNonNull(feeRules,      "feeRules é obrigatório");
        Objects.requireNonNull(taxConfig,     "taxConfig é obrigatório");
        if (salePrice.signum() <= 0)  throw new IllegalArgumentException("salePrice deve ser positivo");
        if (unitsSold  < 1)           throw new IllegalArgumentException("unitsSold deve ser >= 1");
        if (unitCost.signum() < 0)    throw new IllegalArgumentException("unitCost não pode ser negativo");
        adsCost        = adsCost        != null ? adsCost        : BigDecimal.ZERO;
        couponDiscount = couponDiscount != null ? couponDiscount : BigDecimal.ZERO;
        freightCost    = freightCost    != null ? freightCost    : BigDecimal.ZERO;
        returnAmount   = returnAmount   != null ? returnAmount   : BigDecimal.ZERO;
        otherCosts     = otherCosts     != null ? otherCosts     : BigDecimal.ZERO;
    }
}
