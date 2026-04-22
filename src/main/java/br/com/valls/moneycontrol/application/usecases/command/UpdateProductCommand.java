package br.com.valls.moneycontrol.application.usecases.command;

/**
 * Comando de atualização parcial de produto.
 * Campos {@code null} indicam que o valor não deve ser alterado (semântica PATCH).
 */
public record UpdateProductCommand(
        String  name,
        String  category,
        String  eanGtin,
        Integer weightGrams
) {}
