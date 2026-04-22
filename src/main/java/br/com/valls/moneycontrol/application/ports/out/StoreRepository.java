package br.com.valls.moneycontrol.application.ports.out;

import br.com.valls.moneycontrol.domain.enums.StoreStatus;
import br.com.valls.moneycontrol.domain.model.Store;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de saída para persistência de lojas.
 */
public interface StoreRepository {

    /** Persiste ou atualiza uma loja e retorna a instância salva. */
    Store save(Store store);

    /** Busca loja pelo ID. */
    Optional<Store> findById(UUID id);

    /** Retorna lojas da empresa, com filtro opcional por status ({@code null} = todas). */
    List<Store> findByCompanyId(UUID companyId, StoreStatus status);

    /** Retorna todas as lojas de um marketplace. */
    List<Store> findByMarketplaceId(UUID marketplaceId);
}
