package br.com.valls.moneycontrol.application.usecases.command;

import br.com.valls.moneycontrol.domain.enums.CalculationBase;
import br.com.valls.moneycontrol.domain.enums.ChargeMoment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Comando de criação de configuração tributária para uma empresa.
 *
 * @param companyId       empresa dona da configuração
 * @param chargeMoment    momento de cobrança (ON_SALE, ON_PURCHASE, SEPARATE)
 * @param calculationBase base de cálculo (GROSS_REVENUE, NET_REVENUE, PROFIT)
 * @param ratePercentage  alíquota em percentual (ex.: 6.00 = 6%)
 * @param validFrom       início da vigência
 */
public record RegisterTaxConfigCommand(
        UUID            companyId,
        ChargeMoment    chargeMoment,
        CalculationBase calculationBase,
        BigDecimal      ratePercentage,
        LocalDate       validFrom
) {}
