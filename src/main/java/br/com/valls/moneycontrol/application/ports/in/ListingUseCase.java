package br.com.valls.moneycontrol.application.ports.in;

import br.com.valls.moneycontrol.application.usecases.command.RegisterListingCommand;
import br.com.valls.moneycontrol.application.usecases.command.UpdateListingPriceCommand;
import br.com.valls.moneycontrol.domain.model.Listing;
import br.com.valls.moneycontrol.domain.model.ListingPriceHistory;

import java.util.List;
import java.util.UUID;

/**
 * Port de entrada para gerenciamento de anúncios.
 */
public interface ListingUseCase {

    /**
     * Registra um novo anúncio.
     * Valida que a loja pertence à mesma empresa do produto.
     */
    Listing register(RegisterListingCommand command);

    /** Retorna anúncio pelo ID ou lança {@code ListingNotFoundException}. */
    Listing findById(UUID id);

    /** Retorna anúncios de uma loja como lista imutável. */
    List<Listing> findByStore(UUID storeId);

    /** Retorna anúncios de um produto como lista imutável. */
    List<Listing> findByProduct(UUID productId);

    /**
     * Atualiza o preço do anúncio e persiste o histórico de preços.
     * Fecha o registro anterior ({@code valid_until = now}) e cria novo com {@code valid_from = now}.
     *
     * @return o novo {@code ListingPriceHistory} gerado
     */
    ListingPriceHistory updatePrice(UUID id, UpdateListingPriceCommand command);

    /** Pausa o anúncio (ACTIVE → PAUSED). */
    Listing pause(UUID id);

    /** Reativa o anúncio (PAUSED → ACTIVE). */
    Listing reactivate(UUID id);

    /** Fecha permanentemente o anúncio (qualquer estado → CLOSED). */
    Listing close(UUID id);
}
