package br.com.valls.moneycontrol.domain.exception;

import java.util.UUID;

public class CompanyNotFoundException extends EntityNotFoundException {

    public CompanyNotFoundException(UUID id) {
        super("Empresa não encontrada: " + id);
    }

    public CompanyNotFoundException(String message) {
        super(message);
    }
}
