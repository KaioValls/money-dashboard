package br.com.valls.moneycontrol.application.ports.out;

import br.com.valls.moneycontrol.domain.model.Listing;
import br.com.valls.moneycontrol.domain.model.ListingPriceHistory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de saída para persistência de anúncios e histórico de preços.
 */
public interface ListingRepository {

    /** Persiste ou atualiza um anúncio e retorna a instância salva. */
    Listing save(Listing listing);

    /** Busca anúncio pelo ID. */
    Optional<Listing> findById(UUID id);

    /** Retorna anúncios de uma loja. */
    List<Listing> findByStoreId(UUID storeId);

    /** Retorna anúncios de um produto. */
    List<Listing> findByProductId(UUID productId);

    /** Persiste um registro de histórico de preço. */
    ListingPriceHistory savePriceHistory(ListingPriceHistory history);

    /** Retorna o histórico de preços de um anúncio em ordem decrescente de vigência. */
    List<ListingPriceHistory> findPriceHistoryByListingId(UUID listingId);
}
