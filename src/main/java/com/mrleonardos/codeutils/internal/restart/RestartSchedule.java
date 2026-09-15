package com.mrleonardos.codeutils.internal.restart;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codeutils.internal.Clocks;
import com.mrleonardos.codeutils.internal.Moments;

public final class RestartSchedule {

    private static final int DAYS_AHEAD = 8;

    private final List<Integer> moments;
    private final Set<DayOfWeek> days;
    private final Supplier<ZoneId> zone;

    private RestartSchedule(List<Integer> moments, Set<DayOfWeek> days, Supplier<ZoneId> zone) {
        this.moments = Collections.unmodifiableList(moments);
        this.days = days;
        this.zone = zone;
    }

    public static RestartSchedule of(List<String> at, List<String> days, String where, Logger log,
        Supplier<ZoneId> zone) {
        Set<DayOfWeek> chosen = EnumSet.noneOf(DayOfWeek.class);
        for (String written : days == null ? new ArrayList<String>() : days) {
            DayOfWeek day = Moments.day(written);
            if (day == null) {
                log.warn("{}: day {} is not one of mon, tue, wed, thu, fri, sat, sun, it is skipped", where, written);
                continue;
            }
            chosen.add(day);
        }
        return new RestartSchedule(Moments.moments(at, where, log), chosen, zone);
    }

    public boolean idle() {
        return moments.isEmpty();
    }

    public List<Integer> moments() {
        return moments;
    }

    public long nextStop(long now) {
        if (moments.isEmpty()) {
            return 0L;
        }
        ZoneId at = zone.get();
        LocalDate today = Clocks.local(now, at)
            .toLocalDate();
        for (int shift = 0; shift < DAYS_AHEAD; shift++) {
            LocalDate date = today.plusDays(shift);
            if (!days.isEmpty() && !days.contains(date.getDayOfWeek())) {
                continue;
            }
            for (Integer minute : moments) {
                long when = Clocks.millisOf(date, minute.intValue(), at);
                if (when > now) {
                    return when;
                }
            }
        }
        return 0L;
    }
}
