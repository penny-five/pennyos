package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DateLinesTest {

    @Test
    public void theWeekdayComesOutOfEachShippedOrder() {
        assertEquals("d MMMM", DateLines.withoutWeekday("EEEE  d MMMM"));        // values (English)
        assertEquals("d. MMMM", DateLines.withoutWeekday("cccc d. MMMM"));       // values-fi
        assertEquals("d. MMMM", DateLines.withoutWeekday("EEEE, d. MMMM"));      // values-de
        assertEquals("M월 d일", DateLines.withoutWeekday("M월 d일 EEEE"));          // values-ko: weekday last
        assertEquals("d MMMM", DateLines.withoutWeekday("d MMMM EEEE"));         // values-tr
        assertEquals("d-MMMM", DateLines.withoutWeekday("EEEE  d-MMMM"));        // values-uz
    }

    @Test
    public void quotedWordsAndShortFieldsStay() {
        assertEquals("d 'de' MMMM", DateLines.withoutWeekday("EEEE d 'de' MMMM"));
        // 'Ecuador' inside quotes is text, not a weekday; EEE alone is an abbreviation the line keeps.
        assertEquals("d 'Ecuador' MMMM", DateLines.withoutWeekday("EEEE d 'Ecuador' MMMM"));
        assertNull(DateLines.withoutWeekday("EEE d MMMM"));
    }

    @Test
    public void aPatternWithNoWeekdayStaysOnOneLine() {
        assertNull(DateLines.withoutWeekday("d MMMM"));
        assertNull(DateLines.withoutWeekday("EEEE"));
    }
}
