package br.com.valls.moneycontrol.application.usecases.command;

import br.com.valls.moneycontrol.domain.enums.CostCategory;
import br.com.valls.moneycontrol.domain.enums.RecurringCostFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Comando para criar uma definição de custo recorrente.
 * {@code endDate} null indica vigência indefinida.
 */
public record RegisterRecurringCostCommand(
        UUID                   companyId,
        String                 name,
        BigDecimal             amount,
        CostCategory           costCategory,
        RecurringCostFrequency recurrence,
        LocalDate              startDate,
        LocalDate              endDate
) {}
