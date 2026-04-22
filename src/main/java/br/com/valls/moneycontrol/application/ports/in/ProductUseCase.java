package br.com.valls.moneycontrol.application.ports.in;

import br.com.valls.moneycontrol.application.usecases.command.RegisterProductCommand;
import br.com.valls.moneycontrol.application.usecases.command.UpdateProductCommand;
import br.com.valls.moneycontrol.domain.model.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de entrada para gerenciamento de produtos.
 */
public interface ProductUseCase {

    /** Registra um novo produto. Valida unicidade de SKU por empresa. */
    Product register(RegisterProductCommand command);

    /** Retorna produto pelo ID ou lança {@code ProductNotFoundException}. */
    Product findById(UUID id);

    /** Retorna produto pelo SKU de uma empresa. */
    Optional<Product> findByCompanyAndSku(UUID companyId, String sku);

    /** Retorna todos os produtos de uma empresa como lista imutável. */
    List<Product> findAllByCompany(UUID companyId);

    /** Atualiza campos não-nulos do produto. */
    Product update(UUID id, UpdateProductCommand command);

    /** Desativa o produto (soft delete). */
    void deactivate(UUID id);
}
