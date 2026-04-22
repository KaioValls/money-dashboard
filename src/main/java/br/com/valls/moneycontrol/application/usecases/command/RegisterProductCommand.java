package br.com.valls.moneycontrol.application.usecases.command;

import java.util.UUID;

/**
 * Comando de criação de produto.
 *
 * @param companyId   empresa dona do produto
 * @param sku         código SKU único por empresa
 * @param name        nome do produto
 * @param category    categoria (opcional)
 * @param eanGtin     código EAN/GTIN (opcional)
 * @param weightGrams peso em gramas (opcional)
 */
public record RegisterProductCommand(
        UUID    companyId,
        String  sku,
        String  name,
        String  category,
        String  eanGtin,
        Integer weightGrams
) {}
