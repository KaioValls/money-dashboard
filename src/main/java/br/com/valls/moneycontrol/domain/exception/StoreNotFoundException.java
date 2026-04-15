package br.com.valls.moneycontrol.domain.exception;

import java.util.UUID;

public class StoreNotFoundException extends EntityNotFoundException {

    public StoreNotFoundException(UUID id) {
        super("Loja não encontrada: " + id);
    }

    public StoreNotFoundException(String message) {
        super(message);
    }
}
