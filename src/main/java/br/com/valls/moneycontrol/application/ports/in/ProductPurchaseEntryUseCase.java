package br.com.valls.moneycontrol.application.ports.in;

import br.com.valls.moneycontrol.application.usecases.command.RegisterProductPurchaseEntryCommand;
import br.com.valls.moneycontrol.domain.model.ProductPurchaseEntry;
import br.com.valls.moneycontrol.domain.model.WeekReference;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de entrada para gerenciamento de entradas de compra de produto (CMV).
 */
public interface ProductPurchaseEntryUseCase {

    /**
     * Registra uma entrada de compra de produto.
     * Valida que o produto e a empresa existem.
     * Opcionalmente anota fornecedor e nota fiscal se informados no comando.
     */
    ProductPurchaseEntry register(RegisterProductPurchaseEntryCommand command);

    /**
     * Anota informações de fornecedor e nota fiscal em uma entrada existente.
     * Ambos os campos são opcionais — {@code null} mantém o valor atual.
     */
    ProductPurchaseEntry annotate(UUID id, String supplierName, String invoiceNumber);

    /** Busca uma entrada pelo ID. */
    Optional<ProductPurchaseEntry> findById(UUID id);

    /** Retorna todas as entradas de uma empresa em uma semana. */
    List<ProductPurchaseEntry> findByCompanyAndWeek(UUID companyId, WeekReference week);

    /** Retorna todas as entradas de um produto. */
    List<ProductPurchaseEntry> findByProduct(UUID productId);
}
