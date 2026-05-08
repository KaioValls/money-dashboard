package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.in.MarketplaceUseCase;
import br.com.valls.moneycontrol.application.ports.out.MarketplaceRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterMarketplaceCommand;
import br.com.valls.moneycontrol.application.usecases.command.UpdateMarketplaceCommand;
import br.com.valls.moneycontrol.domain.exception.BusinessException;
import br.com.valls.moneycontrol.domain.exception.MarketplaceNotFoundException;
import br.com.valls.moneycontrol.domain.model.Marketplace;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Implementação do use case de gerenciamento de marketplaces.
 * Instanciada exclusivamente em {@code BeanConfig} — sem @Service.
 */
@RequiredArgsConstructor
public class MarketplaceUseCaseImpl implements MarketplaceUseCase {

    private final MarketplaceRepository marketplaceRepository;

    @Override
    public Marketplace register(RegisterMarketplaceCommand command) {
        if (marketplaceRepository.existsBySlug(command.slug())) {
            throw new DuplicateSlugException("Slug já cadastrado: " + command.slug());
        }
        Marketplace marketplace = Marketplace.create(
                command.name(), command.slug(), command.paymentCycleDays());
        if (command.logoUrl()    != null) marketplace.update(null, command.logoUrl(), null, 0);
        if (command.websiteUrl() != null) marketplace.update(null, null, command.websiteUrl(), 0);
        return marketplaceRepository.save(marketplace);
    }

    @Override
    public Marketplace findById(UUID id) {
        return marketplaceRepository.findById(id)
                .orElseThrow(() -> new MarketplaceNotFoundException(id));
    }

    @Override
    public List<Marketplace> findAll() {
        return List.copyOf(marketplaceRepository.findAll());
    }

    @Override
    public Marketplace update(UUID id, UpdateMarketplaceCommand command) {
        Marketplace marketplace = findById(id);
        marketplace.update(
                command.name(),
                command.logoUrl(),
                command.websiteUrl(),
                command.paymentCycleDays() != null ? command.paymentCycleDays() : 0
        );
        return marketplaceRepository.save(marketplace);
    }

    @Override
    public void activate(UUID id) {
        Marketplace marketplace = findById(id);
        marketplace.activate();
        marketplaceRepository.save(marketplace);
    }

    @Override
    public void deactivate(UUID id) {
        Marketplace marketplace = findById(id);
        marketplace.deactivate();
        marketplaceRepository.save(marketplace);
    }

    // ── exceção local ─────────────────────────────────────────────────────────

    public static class DuplicateSlugException extends BusinessException {
        public DuplicateSlugException(String message) { super(message); }
    }
}
