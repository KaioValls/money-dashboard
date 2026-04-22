package br.com.valls.moneycontrol.domain.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Value Object imutável com o resultado consolidado de apuração semanal de uma loja.
 *
 * Fórmulas:
 *   grossRevenue        = salePrice × unitsSold (soma de todos os lançamentos)
 *   grossProfit         = grossRevenue - marketplaceFeeTotal - totalCogs
 *   netProfit           = grossProfit - taxAmount - adsTotal - couponTotal
 *                         - freightTotal - returnTotal - otherCosts
 *   netMarginPercentage = (netProfit / grossRevenue) × 100
 *   roiPercentage       = (netProfit / totalCogs) × 100
 *
 * Os campos são calculados exclusivamente no use case e armazenados aqui como snapshot.
 */
public record WeeklyResult(
        UUID       storeId,
        WeekReference weekReference,
        BigDecimal grossRevenue,
        BigDecimal totalCogs,
        BigDecimal marketplaceFeeTotal,
        BigDecimal taxAmount,
        BigDecimal adsTotal,
        BigDecimal couponTotal,
        BigDecimal freightTotal,
        BigDecimal returnTotal,
        BigDecimal otherCosts,
        BigDecimal grossProfit,
        BigDecimal netProfit,
        BigDecimal netMarginPercentage,
        BigDecimal roiPercentage
) {

    /**
     * Retorna os principais KPIs como mapa — útil para serialização e auditoria.
     */
    public Map<String, BigDecimal> summary() {
        return Map.of(
                "grossRevenue",        grossRevenue,
                "totalCogs",           totalCogs,
                "marketplaceFeeTotal", marketplaceFeeTotal,
                "taxAmount",           taxAmount,
                "grossProfit",         grossProfit,
                "netProfit",           netProfit,
                "netMarginPercentage", netMarginPercentage,
                "roiPercentage",       roiPercentage
        );
    }
}
