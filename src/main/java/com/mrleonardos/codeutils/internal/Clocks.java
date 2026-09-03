package com.mrleonardos.codeutils.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class Clocks {

    public static final long MILLIS = 1000L;

    private Clocks() {}

    public static LocalDateTime local(long millis, ZoneId at) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), at);
    }

    public static long millisOf(LocalDateTime time, ZoneId at) {
        return time.atZone(at)
            .toInstant()
            .toEpochMilli();
    }

    public static long millisOf(LocalDate date, int minuteOfDay, ZoneId at) {
        return millisOf(LocalDateTime.of(date, LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)), at);
    }
}
