package br.com.valls.moneycontrol.domain.model;

import br.com.valls.moneycontrol.domain.exception.InvalidWeekReferenceException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

/**
 * Value Object imutável que representa uma semana ISO (ano + número da semana).
 * Semanas ISO começam na segunda-feira e a semana 1 é a que contém a primeira quinta.
 */
public record WeekReference(int year, int weekNumber) implements Comparable<WeekReference> {

    public WeekReference {
        if (year < 2020) {
            throw new InvalidWeekReferenceException(
                    "year deve ser >= 2020, recebido: " + year);
        }
        if (weekNumber < 1 || weekNumber > 53) {
            throw new InvalidWeekReferenceException(
                    "weekNumber deve estar entre 1 e 53, recebido: " + weekNumber);
        }
    }

    /** Cria um WeekReference a partir de qualquer LocalDate usando regras ISO. */
    public static WeekReference of(LocalDate date) {
        int isoYear = date.get(IsoFields.WEEK_BASED_YEAR);
        int isoWeek = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return new WeekReference(isoYear, isoWeek);
    }

    /** Número da semana dentro do mês (1–5) para uma data. */
    public static int weekOfMonth(LocalDate date) {
        return (date.getDayOfMonth() - 1) / 7 + 1;
    }

    /** Segunda-feira que inicia esta semana ISO. */
    public LocalDate startDate() {
        // Jan 4 está sempre na semana ISO 1 do ano em questão
        return LocalDate.of(year, 1, 4)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, weekNumber)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Domingo que encerra esta semana ISO. */
    public LocalDate endDate() {
        return startDate().plusDays(6);
    }

    /** Semana anterior (navega para o ano anterior quando necessário). */
    public WeekReference previous() {
        return WeekReference.of(startDate().minusWeeks(1));
    }

    /** Próxima semana (navega para o ano seguinte quando necessário). */
    public WeekReference next() {
        return WeekReference.of(startDate().plusWeeks(1));
    }

    @Override
    public int compareTo(WeekReference other) {
        int cmp = Integer.compare(this.year, other.year);
        return cmp != 0 ? cmp : Integer.compare(this.weekNumber, other.weekNumber);
    }

    @Override
    public String toString() {
        return "Sem " + weekNumber + "/" + year;
    }
}
