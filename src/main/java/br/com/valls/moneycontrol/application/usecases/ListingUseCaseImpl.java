package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.in.ListingUseCase;
import br.com.valls.moneycontrol.application.ports.out.ListingRepository;
import br.com.valls.moneycontrol.application.ports.out.ProductRepository;
import br.com.valls.moneycontrol.application.ports.out.StoreRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterListingCommand;
import br.com.valls.moneycontrol.application.usecases.command.UpdateListingPriceCommand;
import br.com.valls.moneycontrol.domain.exception.BusinessException;
import br.com.valls.moneycontrol.domain.exception.ListingNotFoundException;
import br.com.valls.moneycontrol.domain.exception.ProductNotFoundException;
import br.com.valls.moneycontrol.domain.exception.StoreNotFoundException;
import br.com.valls.moneycontrol.domain.model.Listing;
import br.com.valls.moneycontrol.domain.model.ListingPriceHistory;
import br.com.valls.moneycontrol.domain.model.Product;
import br.com.valls.moneycontrol.domain.model.Store;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementação do use case de gerenciamento de anúncios.
 * Instanciada exclusivamente em {@code BeanConfig} — sem @Service.
 */
@RequiredArgsConstructor
public class ListingUseCaseImpl implements ListingUseCase {

    private final ListingRepository listingRepository;
    private final StoreRepository   storeRepository;
    private final ProductRepository productRepository;

    @Override
    public Listing register(RegisterListingCommand command) {
        Product product = productRepository.findById(command.productId())
                .orElseThrow(() -> new ProductNotFoundException(command.productId()));

        Store store = storeRepository.findById(command.storeId())
                .orElseThrow(() -> new StoreNotFoundException(command.storeId()));

        // cross-company listing proibido
        if (!product.getCompanyId().equals(store.getCompanyId())) {
            throw new CrossCompanyListingException(
                    "A loja e o produto pertencem a empresas diferentes");
        }

        // Criar anúncio com campos obrigatórios; listingType e externalListingId são opcionais
        // e podem ser enriquecidos via reconstitute quando retornados da persistência
        Listing listing = Listing.create(
                command.productId(),
                command.storeId(),
                store.getMarketplaceId(),
                store.getCompanyId(),
                command.title(),
                command.salePrice()
        );

        // Para incluir listingType e externalListingId usamos reconstitute logo após o create,
        // já que Listing não expõe setters para esses campos opcionais.
        if (command.listingType() != null || command.externalListingId() != null) {
            listing = Listing.reconstitute(
                    listing.getId(), listing.getProductId(), listing.getStoreId(),
                    listing.getMarketplaceId(), listing.getCompanyId(),
                    command.externalListingId(),
                    listing.getTitle(), listing.getSalePrice(),
                    command.listingType(),
                    listing.getStatus(), listing.getPublishedAt(),
                    listing.getClosedAt(), listing.isActive(),
                    listing.getCreatedAt(), listing.getUpdatedAt());
        }

        return listingRepository.save(listing);
    }

    @Override
    public Listing findById(UUID id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));
    }

    @Override
    public List<Listing> findByStore(UUID storeId) {
        return List.copyOf(listingRepository.findByStoreId(storeId));
    }

    @Override
    public List<Listing> findByProduct(UUID productId) {
        return List.copyOf(listingRepository.findByProductId(productId));
    }

    @Override
    public ListingPriceHistory updatePrice(UUID id, UpdateListingPriceCommand command) {
        Listing listing = findById(id);

        // Fechar o registro de preço anterior (se existir)
        List<ListingPriceHistory> history =
                listingRepository.findPriceHistoryByListingId(id);
        if (!history.isEmpty()) {
            ListingPriceHistory previous = history.get(0); // mais recente primeiro
            if (previous.getValidUntil() == null) {
                previous.closeAt(Instant.now());
                listingRepository.savePriceHistory(previous);
            }
        }

        // Gerar novo registro de histórico via domínio
        ListingPriceHistory newHistory = listing.updatePrice(
                command.newPrice(), command.changedBy(), command.reason());
        listingRepository.save(listing);
        return listingRepository.savePriceHistory(newHistory);
    }

    @Override
    public Listing pause(UUID id) {
        Listing listing = findById(id);
        listing.pause();
        return listingRepository.save(listing);
    }

    @Override
    public Listing reactivate(UUID id) {
        Listing listing = findById(id);
        listing.reactivate();
        return listingRepository.save(listing);
    }

    @Override
    public Listing close(UUID id) {
        Listing listing = findById(id);
        listing.close();
        return listingRepository.save(listing);
    }

    // ── exceção local ─────────────────────────────────────────────────────────

    public static class CrossCompanyListingException extends BusinessException {
        public CrossCompanyListingException(String message) { super(message); }
    }
}
