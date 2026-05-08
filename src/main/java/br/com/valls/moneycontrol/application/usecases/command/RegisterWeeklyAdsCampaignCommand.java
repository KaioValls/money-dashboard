package br.com.valls.moneycontrol.application.usecases.command;

import br.com.valls.moneycontrol.domain.enums.AdsRatioMethod;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando para registrar ou atualizar (upsert) o orçamento de ADS semanal de uma loja.
 * Se já existir uma campanha para (storeId, isoYear, isoWeekNumber), ela é atualizada.
 */
public record RegisterWeeklyAdsCampaignCommand(
        UUID           storeId,
        int            isoYear,
        int            isoWeekNumber,
        BigDecimal     totalAdsBudget,
        AdsRatioMethod ratioMethod
) {}
