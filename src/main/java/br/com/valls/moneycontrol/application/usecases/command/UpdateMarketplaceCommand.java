package br.com.valls.moneycontrol.application.usecases.command;

/**
 * Comando de atualização parcial de marketplace.
 * Campos {@code null} indicam que o valor não deve ser alterado (semântica PATCH).
 */
public record UpdateMarketplaceCommand(
        String  name,
        String  logoUrl,
        String  websiteUrl,
        Integer paymentCycleDays
) {}
