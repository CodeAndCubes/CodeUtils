package com.mrleonardos.codeutils.internal.job;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codeutils.internal.Clocks;
import com.mrleonardos.codeutils.internal.Moments;

public final class JobSchedule {

    private final List<Integer> moments;
    private final int everySeconds;
    private final Supplier<ZoneId> zone;

    private long deadline;

    private JobSchedule(List<Integer> moments, int everySeconds, Supplier<ZoneId> zone) {
        this.moments = Collections.unmodifiableList(moments);
        this.everySeconds = everySeconds;
        this.zone = zone;
    }

    public static JobSchedule of(List<String> at, int everySeconds, String where, Logger log, Supplier<ZoneId> zone) {
        return new JobSchedule(Moments.moments(at, where, log), Math.max(0, everySeconds), zone);
    }

    public void arm(long now) {
        deadline = everySeconds > 0 ? now + everySeconds * Clocks.MILLIS : 0L;
    }

    public boolean idle() {
        return moments.isEmpty() && everySeconds <= 0;
    }

    public List<Integer> moments() {
        return moments;
    }

    public int everySeconds() {
        return everySeconds;
    }

    public long deadline() {
        return deadline;
    }

    public int due(long from, long to) {
        int count = byMoments(from, to);
        if (everySeconds > 0 && deadline > 0 && to >= deadline) {
            deadline = to + everySeconds * Clocks.MILLIS;
            count++;
        }
        return count;
    }

    public long nextMoment(long now) {
        if (moments.isEmpty()) {
            return everySeconds > 0 ? deadline : 0L;
        }
        ZoneId at = zone.get();
        LocalDate day = Clocks.local(now, at)
            .toLocalDate();
        for (int shift = 0; shift <= 1; shift++) {
            LocalDate date = day.plusDays(shift);
            for (Integer minute : moments) {
                long when = Clocks.millisOf(date, minute.intValue(), at);
                if (when > now) {
                    return everySeconds > 0 && deadline > 0 ? Math.min(when, deadline) : when;
                }
            }
        }
        return everySeconds > 0 ? deadline : 0L;
    }

    private int byMoments(long from, long to) {
        if (moments.isEmpty() || to <= from) {
            return 0;
        }
        ZoneId at = zone.get();
        LocalDate first = Clocks.local(from, at)
            .toLocalDate();
        LocalDate last = Clocks.local(to, at)
            .toLocalDate();
        int count = 0;
        for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
            for (Integer minute : moments) {
                long when = Clocks.millisOf(date, minute.intValue(), at);
                if (when > from && when <= to) {
                    count++;
                }
            }
        }
        return count;
    }
}
