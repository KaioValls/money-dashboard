package br.com.valls.moneycontrol.application.ports.in;

import br.com.valls.moneycontrol.application.usecases.command.RegisterMarketplaceCommand;
import br.com.valls.moneycontrol.application.usecases.command.UpdateMarketplaceCommand;
import br.com.valls.moneycontrol.domain.model.Marketplace;

import java.util.List;
import java.util.UUID;

/**
 * Port de entrada para gerenciamento de marketplaces.
 */
public interface MarketplaceUseCase {

    /** Registra um novo marketplace. Valida slug único. */
    Marketplace register(RegisterMarketplaceCommand command);

    /** Retorna marketplace pelo ID ou lança {@code MarketplaceNotFoundException}. */
    Marketplace findById(UUID id);

    /** Retorna todos os marketplaces como lista imutável. */
    List<Marketplace> findAll();

    /** Atualiza campos não-nulos do marketplace. */
    Marketplace update(UUID id, UpdateMarketplaceCommand command);

    /** Ativa o marketplace. */
    void activate(UUID id);

    /** Desativa o marketplace (soft delete). */
    void deactivate(UUID id);
}
