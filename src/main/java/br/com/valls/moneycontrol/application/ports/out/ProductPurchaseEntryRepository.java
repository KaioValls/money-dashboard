package br.com.valls.moneycontrol.application.ports.out;

import br.com.valls.moneycontrol.domain.model.ProductPurchaseEntry;
import br.com.valls.moneycontrol.domain.model.WeekReference;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de saída para persistência de entradas de compra de produtos.
 */
public interface ProductPurchaseEntryRepository {

    /** Persiste ou atualiza uma entrada de compra. */
    ProductPurchaseEntry save(ProductPurchaseEntry entry);

    /** Busca entrada pelo ID. */
    Optional<ProductPurchaseEntry> findById(UUID id);

    /**
     * Retorna a compra mais recente do produto anterior à semana de referência.
     * Usado para capturar o {@code unit_cost_snapshot} no momento do lançamento de venda.
     * ORDER BY iso_year DESC, iso_week_number DESC LIMIT 1.
     */
    Optional<ProductPurchaseEntry> findMostRecentBeforeWeek(UUID productId, WeekReference week);

    /** Retorna todas as compras de um produto. */
    List<ProductPurchaseEntry> findByProductId(UUID productId);

    /** Retorna compras de uma empresa em uma semana. */
    List<ProductPurchaseEntry> findByCompanyAndWeek(UUID companyId, WeekReference week);
}
