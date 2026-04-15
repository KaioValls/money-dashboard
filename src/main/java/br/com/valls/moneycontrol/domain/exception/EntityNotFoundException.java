package br.com.valls.moneycontrol.domain.exception;

public abstract class EntityNotFoundException extends RuntimeException {

    protected EntityNotFoundException(String message) {
        super(message);
    }
}
