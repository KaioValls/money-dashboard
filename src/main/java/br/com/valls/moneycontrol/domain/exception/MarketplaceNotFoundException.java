package br.com.valls.moneycontrol.domain.exception;

import java.util.UUID;

public class MarketplaceNotFoundException extends EntityNotFoundException {

    public MarketplaceNotFoundException(UUID id) {
        super("Marketplace não encontrado: " + id);
    }

    public MarketplaceNotFoundException(String message) {
        super(message);
    }
}
