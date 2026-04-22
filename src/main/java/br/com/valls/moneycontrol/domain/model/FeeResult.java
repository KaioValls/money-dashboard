package br.com.valls.moneycontrol.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resultado do cálculo de taxa de marketplace para um lançamento.
 * Imutável — snapshot capturado no momento do cálculo.
 */
public record FeeResult(
        UUID       feeRuleId,
        BigDecimal feePercentageSnapshot,
        BigDecimal fixedFeeSnapshot,
        BigDecimal totalFeeAmount
) {}
