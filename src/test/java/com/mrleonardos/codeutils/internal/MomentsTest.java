package com.mrleonardos.codeutils.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;

import org.junit.jupiter.api.Test;

class MomentsTest {

    @Test
    void aMomentIsReadAsMinutesFromMidnight() {
        assertEquals(0, Moments.parse("00:00"));
        assertEquals(4 * 60, Moments.parse("04:00"));
        assertEquals(23 * 60 + 59, Moments.parse("23:59"));
        assertEquals(6 * 60 + 5, Moments.parse(" 6:05 "));
    }

    @Test
    void whatIsNotAMomentIsRefused() {
        assertEquals(Moments.NONE, Moments.parse(null));
        assertEquals(Moments.NONE, Moments.parse(""));
        assertEquals(Moments.NONE, Moments.parse("24:00"));
        assertEquals(Moments.NONE, Moments.parse("12:60"));
        assertEquals(Moments.NONE, Moments.parse("12"));
        assertEquals(Moments.NONE, Moments.parse("12:"));
        assertEquals(Moments.NONE, Moments.parse(":30"));
        assertEquals(Moments.NONE, Moments.parse("полдень"));
    }

    @Test
    void aMomentIsPrintedBackWithTwoDigits() {
        assertEquals("04:00", Moments.print(4 * 60));
        assertEquals("00:07", Moments.print(7));
        assertEquals("23:59", Moments.print(23 * 60 + 59));
        assertEquals("", Moments.print(Moments.NONE));
    }

    @Test
    void aWindowFallsApartIntoTwoMoments() {
        assertEquals(23 * 60, Moments.windowStart("23:00-02:00"));
        assertEquals(2 * 60, Moments.windowEnd("23:00-02:00"));
        assertTrue(Moments.window("23:00-02:00"));
    }

    @Test
    void whatIsNotAWindowIsRefused() {
        assertFalse(Moments.window(""));
        assertFalse(Moments.window("23:00"));
        assertFalse(Moments.window("23:00-"));
        assertFalse(Moments.window("-02:00"));
        assertFalse(Moments.window("вечером"));
    }

    @Test
    void theWrittenCheckSeparatesEmptyFromMissing() {
        assertFalse(Moments.written(null));
        assertFalse(Moments.written("   "));
        assertTrue(Moments.written("23:00-02:00"));
    }

    @Test
    void daysAreReadByTheirThreeLetters() {
        assertEquals(DayOfWeek.MONDAY, Moments.day("mon"));
        assertEquals(DayOfWeek.SUNDAY, Moments.day("SUN"));
        assertEquals(DayOfWeek.FRIDAY, Moments.day(" fri "));
        assertNull(Moments.day("понедельник"));
        assertNull(Moments.day(null));
    }

    @Test
    void everyDayGoesBackToItsOwnWord() {
        for (DayOfWeek day : DayOfWeek.values()) {
            assertEquals(day, Moments.day(Moments.word(day)), day.name());
        }
    }
}
