package br.com.valls.moneycontrol.domain.model;

import java.math.BigDecimal;

/**
 * Value Object imutável com a variação semana-a-semana dos principais KPIs de uma loja.
 * Percentuais de variação são calculados em relação à semana anterior.
 * Se a base da semana anterior for zero, o percentual retorna {@link BigDecimal#ZERO}.
 */
public record WeekOverWeekDelta(
        WeekReference  currentWeek,
        WeekReference  previousWeek,
        WeeklyResult   current,
        WeeklyResult   previous,
        BigDecimal     grossRevenueDelta,
        BigDecimal     netProfitDelta,
        BigDecimal     grossRevenueDeltaPct,
        BigDecimal     netProfitDeltaPct
) {}
