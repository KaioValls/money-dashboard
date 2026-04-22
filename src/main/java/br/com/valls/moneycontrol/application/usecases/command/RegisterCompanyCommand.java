package br.com.valls.moneycontrol.application.usecases.command;

import br.com.valls.moneycontrol.domain.enums.TaxRegime;

/**
 * Comando de criação de empresa.
 *
 * @param cnpjDigits  CNPJ com ou sem formatação — validado pelo domínio
 * @param legalName   razão social
 * @param tradeName   nome fantasia (opcional)
 * @param taxRegime   regime tributário
 * @param simplesAnnex anexo do Simples Nacional (opcional; ex.: "III")
 */
public record RegisterCompanyCommand(
        String    cnpjDigits,
        String    legalName,
        String    tradeName,
        TaxRegime taxRegime,
        String    simplesAnnex
) {}
