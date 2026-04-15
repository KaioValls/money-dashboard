package br.com.valls.moneycontrol.domain.exception;

import java.util.UUID;

public class ProductNotFoundException extends EntityNotFoundException {

    public ProductNotFoundException(UUID id) {
        super("Produto não encontrado: " + id);
    }

    public ProductNotFoundException(String message) {
        super(message);
    }
}
