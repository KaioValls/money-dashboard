package br.com.valls.moneycontrol.application.usecases.command;

import br.com.valls.moneycontrol.domain.enums.TaxRegime;

/**
 * Comando de atualização parcial de empresa.
 * Campos {@code null} indicam que o valor não deve ser alterado (semântica PATCH).
 */
public record UpdateCompanyCommand(
        String    legalName,
        String    tradeName,
        TaxRegime taxRegime,
        String    simplesAnnex
) {}
