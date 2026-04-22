package br.com.valls.moneycontrol.application.ports.out;

import br.com.valls.moneycontrol.domain.model.MarketplaceAccount;

import java.util.Optional;
import java.util.UUID;

/**
 * Port de saída para persistência de contas de marketplace por empresa.
 */
public interface MarketplaceAccountRepository {

    /** Persiste ou atualiza uma conta e retorna a instância salva. */
    MarketplaceAccount save(MarketplaceAccount account);

    /** Busca conta pelo ID. */
    Optional<MarketplaceAccount> findById(UUID id);

    /**
     * Busca a conta que associa empresa e marketplace.
     * Retorna {@code Optional.empty()} se a empresa ainda não tem conta neste marketplace.
     */
    Optional<MarketplaceAccount> findByCompanyAndMarketplace(UUID companyId, UUID marketplaceId);
}
