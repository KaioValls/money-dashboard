package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.in.ProductPurchaseEntryUseCase;
import br.com.valls.moneycontrol.application.ports.out.ProductPurchaseEntryRepository;
import br.com.valls.moneycontrol.application.ports.out.ProductRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterProductPurchaseEntryCommand;
import br.com.valls.moneycontrol.domain.exception.EntityNotFoundException;
import br.com.valls.moneycontrol.domain.exception.ProductNotFoundException;
import br.com.valls.moneycontrol.domain.model.Product;
import br.com.valls.moneycontrol.domain.model.ProductPurchaseEntry;
import br.com.valls.moneycontrol.domain.model.WeekReference;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação do use case de entradas de compra de produto (CMV).
 * Instanciada exclusivamente em {@code BeanConfig} — sem @Service.
 */
@RequiredArgsConstructor
public class ProductPurchaseEntryUseCaseImpl implements ProductPurchaseEntryUseCase {

    private final ProductPurchaseEntryRepository purchaseRepository;
    private final ProductRepository              productRepository;

    @Override
    public ProductPurchaseEntry register(RegisterProductPurchaseEntryCommand command) {
        // Valida produto
        Product product = productRepository.findById(command.productId())
                .orElseThrow(() -> new ProductNotFoundException(command.productId()));

        // Valida que o produto pertence à empresa informada
        if (!product.getCompanyId().equals(command.companyId())) {
            throw new IllegalArgumentException(
                    "Produto " + command.productId() + " não pertence à empresa " + command.companyId());
        }

        ProductPurchaseEntry entry = ProductPurchaseEntry.create(
                command.companyId(),
                command.productId(),
                command.isoYear(),
                command.isoWeekNumber(),
                command.quantity(),
                command.unitCost(),
                command.hasFreight(),
                command.freightCost()
        );

        if (command.supplierName() != null || command.invoiceNumber() != null) {
            entry.annotate(command.supplierName(), command.invoiceNumber());
        }

        return purchaseRepository.save(entry);
    }

    @Override
    public ProductPurchaseEntry annotate(UUID id, String supplierName, String invoiceNumber) {
        ProductPurchaseEntry entry = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ProductPurchaseEntry não encontrado: " + id) {});
        entry.annotate(supplierName, invoiceNumber);
        return purchaseRepository.save(entry);
    }

    @Override
    public Optional<ProductPurchaseEntry> findById(UUID id) {
        return purchaseRepository.findById(id);
    }

    @Override
    public List<ProductPurchaseEntry> findByCompanyAndWeek(UUID companyId, WeekReference week) {
        return List.copyOf(purchaseRepository.findByCompanyAndWeek(companyId, week));
    }

    @Override
    public List<ProductPurchaseEntry> findByProduct(UUID productId) {
        return List.copyOf(purchaseRepository.findByProductId(productId));
    }
}
