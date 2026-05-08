package br.com.valls.moneycontrol.application.usecases.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando para registrar uma entrada de compra de produto (CMV).
 * {@code supplierName} e {@code invoiceNumber} são opcionais e podem ser anotados
 * posteriormente via {@code annotate}.
 */
public record RegisterProductPurchaseEntryCommand(
        UUID       companyId,
        UUID       productId,
        int        isoYear,
        int        isoWeekNumber,
        int        quantity,
        BigDecimal unitCost,
        boolean    hasFreight,
        BigDecimal freightCost,
        String     supplierName,
        String     invoiceNumber
) {}
