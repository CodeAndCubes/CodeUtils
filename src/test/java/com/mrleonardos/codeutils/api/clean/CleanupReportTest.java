package com.mrleonardos.codeutils.api.clean;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CleanupReportTest {

    @Test
    void theThreeNumbersOfAPassAreKeptApart() {
        CleanupReport report = CleanupReport.of("drops", 12_000, 340, 300, 7L);

        assertEquals("drops", report.rule());
        assertEquals(12_000, report.scanned());
        assertEquals(340, report.matched(), "подошло правилу это не всё просмотренное");
        assertEquals(300, report.removed(), "снято это не всё подошедшее: пороги и maxRemovals режут");
        assertEquals(7L, report.millis());
    }

    @Test
    void aCountingPassIsAReportWithNothingRemoved() {
        CleanupReport report = CleanupReport.of("drops", 900, 40, 0, 1L);

        assertEquals(40, report.matched());
        assertEquals(0, report.removed());
    }

    @Test
    void numbersBelowZeroDoNotGetIntoAReport() {
        CleanupReport report = CleanupReport.of("drops", -1, -2, -3, -4L);

        assertEquals(0, report.scanned());
        assertEquals(0, report.matched());
        assertEquals(0, report.removed());
        assertEquals(0L, report.millis());
    }
}
