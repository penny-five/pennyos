package com.pennyos.marketdata;

import org.junit.Test;
import java.time.LocalDate;
import static org.junit.Assert.*;

public class PriceHistoryTest {
    @Test public void mondayUsesFridayAndSkipsCurrentCandle() {
        PriceHistory history = new PriceHistory();
        history.add(LocalDate.of(2026, 11, 13), 2);
        history.add(LocalDate.of(2026, 11, 16), 3);
        assertEquals(50, history.change(3, LocalDate.of(2026, 11, 16), "day"), 0.0001);
    }
    @Test public void weekUsesCalendarWeekNotSevenTradingSessions() {
        PriceHistory history = new PriceHistory();
        history.add(LocalDate.of(2026, 11, 6), 2);
        history.add(LocalDate.of(2026, 11, 9), 3);
        assertEquals(50, history.change(3, LocalDate.of(2026, 11, 15), "week"), 0.0001);
    }
    @Test public void monthEndUsesCalendarMonthAndPriorAvailableSession() {
        PriceHistory history = new PriceHistory();
        history.add(LocalDate.of(2026, 2, 27), 4);
        history.add(LocalDate.of(2026, 3, 2), 3);
        assertEquals(-25, history.change(3, LocalDate.of(2026, 3, 31), "month"), 0.0001);
    }
    @Test public void leapYearComparisonUsesPreviousYearFebruary28() {
        PriceHistory history = new PriceHistory();
        history.add(LocalDate.of(2023, 2, 28), 2);
        assertEquals(25, history.change(2.5, LocalDate.of(2024, 2, 29), "year"), 0.0001);
    }
    @Test public void missingHistoryIsUnavailableNotZero() {
        assertNull(new PriceHistory().change(2, LocalDate.of(2026, 11, 15), "year"));
    }
    @Test public void ignoresInvalidCloses() {
        PriceHistory history = new PriceHistory();
        history.add(LocalDate.of(2026, 11, 12), 2);
        history.add(LocalDate.of(2026, 11, 13), Double.NaN);
        history.add(LocalDate.of(2026, 11, 14), 0);
        assertEquals(50, history.change(3, LocalDate.of(2026, 11, 15), "day"), 0.0001);
    }
}
