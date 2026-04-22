package br.com.valls.moneycontrol.application.ports.out;

import br.com.valls.moneycontrol.domain.model.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de saída para persistência de produtos.
 */
public interface ProductRepository {

    /** Persiste ou atualiza um produto e retorna a instância salva. */
    Product save(Product product);

    /** Busca produto pelo ID. */
    Optional<Product> findById(UUID id);

    /** Busca produto pelo SKU de uma empresa. */
    Optional<Product> findByCompanyAndSku(UUID companyId, String sku);

    /** Retorna todos os produtos de uma empresa. */
    List<Product> findByCompanyId(UUID companyId);

    /** Verifica se já existe produto com o SKU para a empresa informada. */
    boolean existsByCompanyIdAndSku(UUID companyId, String sku);
}
