package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.in.ProductUseCase;
import br.com.valls.moneycontrol.application.ports.out.CompanyRepository;
import br.com.valls.moneycontrol.application.ports.out.ProductRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterProductCommand;
import br.com.valls.moneycontrol.application.usecases.command.UpdateProductCommand;
import br.com.valls.moneycontrol.domain.exception.BusinessException;
import br.com.valls.moneycontrol.domain.exception.CompanyNotFoundException;
import br.com.valls.moneycontrol.domain.exception.ProductNotFoundException;
import br.com.valls.moneycontrol.domain.model.Product;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação do use case de gerenciamento de produtos.
 * Instanciada exclusivamente em {@code BeanConfig} — sem @Service.
 */
@RequiredArgsConstructor
public class ProductUseCaseImpl implements ProductUseCase {

    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;

    @Override
    public Product register(RegisterProductCommand command) {
        companyRepository.findById(command.companyId())
                .orElseThrow(() -> new CompanyNotFoundException(command.companyId()));

        if (productRepository.existsByCompanyIdAndSku(command.companyId(), command.sku())) {
            throw new DuplicateSkuException(
                    "SKU já cadastrado para esta empresa: " + command.sku());
        }

        Product product = Product.create(command.companyId(), command.sku(), command.name());
        product.update(null, command.category(), command.eanGtin(), command.weightGrams());
        return productRepository.save(product);
    }

    @Override
    public Product findById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    public Optional<Product> findByCompanyAndSku(UUID companyId, String sku) {
        return productRepository.findByCompanyAndSku(companyId, sku);
    }

    @Override
    public List<Product> findAllByCompany(UUID companyId) {
        return List.copyOf(productRepository.findByCompanyId(companyId));
    }

    @Override
    public Product update(UUID id, UpdateProductCommand command) {
        Product product = findById(id);
        product.update(command.name(), command.category(), command.eanGtin(), command.weightGrams());
        return productRepository.save(product);
    }

    @Override
    public void deactivate(UUID id) {
        Product product = findById(id);
        product.deactivate();
        productRepository.save(product);
    }

    // ── exceção local ─────────────────────────────────────────────────────────

    public static class DuplicateSkuException extends BusinessException {
        public DuplicateSkuException(String message) { super(message); }
    }
}
