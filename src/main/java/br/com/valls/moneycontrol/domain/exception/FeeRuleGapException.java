package br.com.valls.moneycontrol.domain.exception;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lançada quando nenhuma regra de comissão ativa é aplicável ao preço e à data informados.
 */
public class FeeRuleGapException extends BusinessException {

    public FeeRuleGapException(BigDecimal salePrice, LocalDate date) {
        super("Nenhuma regra de comissão aplicável encontrada para o preço R$"
                + salePrice.toPlainString() + " na data " + date);
    }
}
