package br.com.valls.moneycontrol.application.usecases.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando de criação de anúncio.
 *
 * @param productId          produto anunciado
 * @param storeId            loja que publica o anúncio
 * @param title              título do anúncio no marketplace
 * @param salePrice          preço de venda inicial
 * @param listingType        tipo de anúncio (ex.: "CLASSIC", "PREMIUM") — opcional
 * @param externalListingId  ID externo no marketplace (ex.: MLB123456) — opcional
 */
public record RegisterListingCommand(
        UUID       productId,
        UUID       storeId,
        String     title,
        BigDecimal salePrice,
        String     listingType,
        String     externalListingId
) {}
