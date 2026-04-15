package br.com.valls.moneycontrol.domain.exception;

public class DuplicateSaleEntryException extends BusinessException {

    public DuplicateSaleEntryException(String message) {
        super(message);
    }
}
