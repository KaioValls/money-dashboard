package br.com.valls.moneycontrol.application.usecases.command;

import br.com.valls.moneycontrol.domain.enums.AdsRatioMode;

import java.util.UUID;

/**
 * Comando de criação de loja.
 *
 * @param companyId     empresa dona da loja
 * @param marketplaceId marketplace ao qual a loja pertence
 * @param name          nome da loja no marketplace
 * @param adsRatioMode  modo de rateio de ADS (DIRECT ou CAMPAIGN)
 */
public record RegisterStoreCommand(
        UUID         companyId,
        UUID         marketplaceId,
        String       name,
        AdsRatioMode adsRatioMode
) {}
