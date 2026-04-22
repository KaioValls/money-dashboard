package br.com.valls.moneycontrol.domain.model;

import br.com.valls.moneycontrol.domain.exception.InvalidWeekReferenceException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeekReferenceTest {

    // ── critério: of(2025-01-01) → year=2025, weekNumber=1 ───────────────────
    @Test
    void of_date_returns_correct_iso_week() {
        var ref = WeekReference.of(LocalDate.of(2025, 1, 1));
        assertEquals(2025, ref.year());
        assertEquals(1, ref.weekNumber());
    }

    // ── critério: weekNumber=0 lança InvalidWeekReferenceException ────────────
    @Test
    void invalid_week_number_zero_throws() {
        assertThrows(InvalidWeekReferenceException.class,
                () -> new WeekReference(2025, 0));
    }

    // ── critério: weekNumber=54 lança InvalidWeekReferenceException ───────────
    @Test
    void invalid_week_number_54_throws() {
        assertThrows(InvalidWeekReferenceException.class,
                () -> new WeekReference(2025, 54));
    }

    // ── critério: year=2019 lança InvalidWeekReferenceException ──────────────
    @Test
    void invalid_year_below_2020_throws() {
        assertThrows(InvalidWeekReferenceException.class,
                () -> new WeekReference(2019, 1));
    }

    // ── critério: implementa Comparable — ordena cronologicamente ─────────────
    @Test
    void comparable_sorts_chronologically() {
        var w1 = new WeekReference(2025, 1);
        var w2 = new WeekReference(2025, 10);
        var w3 = new WeekReference(2026, 1);

        List<WeekReference> list = Arrays.asList(w3, w1, w2);
        Collections.sort(list);

        assertEquals(List.of(w1, w2, w3), list);
    }

    // ── critério: compareTo retorna negativo/zero/positivo corretamente ───────
    @Test
    void compare_to_same_week_returns_zero() {
        assertEquals(0, new WeekReference(2025, 5).compareTo(new WeekReference(2025, 5)));
    }

    @Test
    void compare_to_later_week_returns_negative() {
        assertTrue(new WeekReference(2025, 3).compareTo(new WeekReference(2025, 5)) < 0);
    }

    @Test
    void compare_to_earlier_year_returns_positive() {
        assertTrue(new WeekReference(2026, 1).compareTo(new WeekReference(2025, 52)) > 0);
    }

    // ── startDate é segunda-feira ─────────────────────────────────────────────
    @Test
    void start_date_is_monday() {
        var ref = WeekReference.of(LocalDate.of(2025, 1, 1));  // semana 1/2025
        LocalDate start = ref.startDate();
        assertEquals(java.time.DayOfWeek.MONDAY, start.getDayOfWeek());
    }

    // ── endDate é domingo 6 dias após startDate ───────────────────────────────
    @Test
    void end_date_is_sunday_six_days_after_start() {
        var ref = new WeekReference(2025, 5);
        assertEquals(ref.startDate().plusDays(6), ref.endDate());
        assertEquals(java.time.DayOfWeek.SUNDAY, ref.endDate().getDayOfWeek());
    }

    // ── of(startDate) reproduz o mesmo WeekReference ─────────────────────────
    @Test
    void of_start_date_round_trips() {
        var ref = new WeekReference(2025, 20);
        assertEquals(ref, WeekReference.of(ref.startDate()));
    }

    // ── previous() navega corretamente ────────────────────────────────────────
    @Test
    void previous_from_week_5_returns_week_4() {
        var prev = new WeekReference(2025, 5).previous();
        assertEquals(new WeekReference(2025, 4), prev);
    }

    @Test
    void previous_from_week_1_returns_last_week_of_previous_year() {
        var prev = new WeekReference(2025, 1).previous();
        assertEquals(2024, prev.year());
        assertTrue(prev.weekNumber() >= 52);
    }

    // ── next() navega corretamente ────────────────────────────────────────────
    @Test
    void next_from_week_4_returns_week_5() {
        var next = new WeekReference(2025, 4).next();
        assertEquals(new WeekReference(2025, 5), next);
    }

    @Test
    void next_then_previous_returns_original() {
        var ref = new WeekReference(2025, 15);
        assertEquals(ref, ref.next().previous());
    }

    // ── weekOfMonth ───────────────────────────────────────────────────────────
    @Test
    void week_of_month_days_1_to_7_return_1() {
        for (int d = 1; d <= 7; d++) {
            assertEquals(1, WeekReference.weekOfMonth(LocalDate.of(2025, 3, d)),
                    "dia " + d + " deve ser semana 1 do mês");
        }
    }

    @Test
    void week_of_month_days_8_to_14_return_2() {
        for (int d = 8; d <= 14; d++) {
            assertEquals(2, WeekReference.weekOfMonth(LocalDate.of(2025, 3, d)),
                    "dia " + d + " deve ser semana 2 do mês");
        }
    }

    // ── nenhum import Spring ou JPA ───────────────────────────────────────────
    @Test
    void no_spring_or_jpa_imports() throws Exception {
        var source = WeekReference.class;
        for (var field : source.getDeclaredFields()) {
            var typeName = field.getType().getName();
            assertFalse(typeName.startsWith("org.springframework"),
                    "Campo " + field.getName() + " não deve ser de Spring");
            assertFalse(typeName.startsWith("jakarta.persistence"),
                    "Campo " + field.getName() + " não deve ser de JPA");
        }
    }
}
