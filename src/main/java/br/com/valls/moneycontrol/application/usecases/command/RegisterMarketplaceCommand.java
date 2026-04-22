package br.com.valls.moneycontrol.application.usecases.command;

/**
 * Comando de criação de marketplace.
 *
 * @param name             nome do marketplace (ex.: "Mercado Livre")
 * @param slug             identificador URL-friendly único (ex.: "mercado-livre")
 * @param logoUrl          URL do logotipo (opcional)
 * @param websiteUrl       URL do site (opcional)
 * @param paymentCycleDays dias do ciclo de repasse financeiro
 */
public record RegisterMarketplaceCommand(
        String name,
        String slug,
        String logoUrl,
        String websiteUrl,
        int    paymentCycleDays
) {}
