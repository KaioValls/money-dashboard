package br.com.valls.moneycontrol.domain.exception;

import java.util.UUID;

public class ListingNotFoundException extends EntityNotFoundException {

    public ListingNotFoundException(UUID id) {
        super("Anúncio não encontrado: " + id);
    }

    public ListingNotFoundException(String message) {
        super(message);
    }
}
