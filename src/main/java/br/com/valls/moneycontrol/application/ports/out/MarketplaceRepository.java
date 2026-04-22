package br.com.valls.moneycontrol.application.ports.out;

import br.com.valls.moneycontrol.domain.model.Marketplace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de saída para persistência de marketplaces.
 */
public interface MarketplaceRepository {

    /** Persiste ou atualiza um marketplace e retorna a instância salva. */
    Marketplace save(Marketplace marketplace);

    /** Busca marketplace pelo ID. */
    Optional<Marketplace> findById(UUID id);

    /** Retorna todos os marketplaces cadastrados. */
    List<Marketplace> findAll();

    /** Busca marketplace pelo slug único. */
    Optional<Marketplace> findBySlug(String slug);

    /** Verifica se já existe marketplace com o slug informado. */
    boolean existsBySlug(String slug);
}
