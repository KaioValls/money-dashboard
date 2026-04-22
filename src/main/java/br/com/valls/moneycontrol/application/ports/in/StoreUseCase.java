package br.com.valls.moneycontrol.application.ports.in;

import br.com.valls.moneycontrol.application.usecases.command.RegisterStoreCommand;
import br.com.valls.moneycontrol.application.usecases.command.UpdateStoreCommand;
import br.com.valls.moneycontrol.domain.enums.AdsRatioMode;
import br.com.valls.moneycontrol.domain.enums.StoreStatus;
import br.com.valls.moneycontrol.domain.model.Store;

import java.util.List;
import java.util.UUID;

/**
 * Port de entrada para gerenciamento de lojas.
 */
public interface StoreUseCase {

    /** Registra uma nova loja. Cria {@code MarketplaceAccount} se ainda não existir. */
    Store register(RegisterStoreCommand command);

    /** Retorna loja pelo ID ou lança {@code StoreNotFoundException}. */
    Store findById(UUID id);

    /** Retorna lojas da empresa. Pode filtrar por {@code status} (null = todas). */
    List<Store> findByCompany(UUID companyId, StoreStatus status);

    /** Retorna lojas do marketplace como lista imutável. */
    List<Store> findByMarketplace(UUID marketplaceId);

    /** Atualiza campos não-nulos da loja. */
    Store update(UUID id, UpdateStoreCommand command);

    /** Pausa a loja (ACTIVE → PAUSED). Lança {@code BusinessException} se CLOSED. */
    Store pause(UUID id);

    /** Reativa a loja (PAUSED → ACTIVE). Lança {@code BusinessException} se CLOSED. */
    Store reactivate(UUID id);

    /** Fecha permanentemente a loja (qualquer estado → CLOSED). */
    Store close(UUID id);

    /** Altera o modo de rateio de ADS da loja. */
    Store updateAdsRatioMode(UUID id, AdsRatioMode mode);
}
