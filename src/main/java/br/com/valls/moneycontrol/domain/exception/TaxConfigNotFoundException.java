package br.com.valls.moneycontrol.domain.exception;

import java.util.UUID;

public class TaxConfigNotFoundException extends EntityNotFoundException {

    public TaxConfigNotFoundException(UUID companyId) {
        super("Configuração tributária não encontrada para a empresa: " + companyId);
    }

    public TaxConfigNotFoundException(String message) {
        super(message);
    }
}
