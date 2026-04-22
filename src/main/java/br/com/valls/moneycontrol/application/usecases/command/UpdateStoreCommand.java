package br.com.valls.moneycontrol.application.usecases.command;

import br.com.valls.moneycontrol.domain.enums.AdsRatioMode;

/**
 * Comando de atualização parcial de loja.
 * Campos {@code null} indicam que o valor não deve ser alterado (semântica PATCH).
 */
public record UpdateStoreCommand(
        String       name,
        AdsRatioMode adsRatioMode
) {}
