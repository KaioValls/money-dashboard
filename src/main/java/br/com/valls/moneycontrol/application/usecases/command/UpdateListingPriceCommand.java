package br.com.valls.moneycontrol.application.usecases.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando de atualização de preço de anúncio.
 * Gera um {@code ListingPriceHistory} e fecha o registro anterior.
 *
 * @param newPrice  novo preço de venda (deve ser positivo)
 * @param changedBy identificador do usuário que realizou a alteração
 * @param reason    motivo da alteração (opcional)
 */
public record UpdateListingPriceCommand(
        BigDecimal newPrice,
        UUID       changedBy,
        String     reason
) {}
