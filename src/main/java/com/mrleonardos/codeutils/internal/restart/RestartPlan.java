package com.mrleonardos.codeutils.internal.restart;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codecore.api.util.Durations;
import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.api.message.BroadcastSink;
import com.mrleonardos.codeutils.api.restart.RestartPhase;
import com.mrleonardos.codeutils.api.restart.RestartPlanView;
import com.mrleonardos.codeutils.api.restart.RestartReason;
import com.mrleonardos.codeutils.api.when.ServerSnapshot;
import com.mrleonardos.codeutils.api.when.When;
import com.mrleonardos.codeutils.internal.Announcer;
import com.mrleonardos.codeutils.internal.Clocks;
import com.mrleonardos.codeutils.internal.Conditions;
import com.mrleonardos.codeutils.internal.SpiNames;
import com.mrleonardos.codeutils.internal.Subsystem;
import com.mrleonardos.codeutils.internal.command.UtilsMessages;

public final class RestartPlan implements Subsystem, SpiNames.Source {

    public enum Answer {

        ARMED,
        CANCELLED,
        NOTHING,
        TOO_LATE
    }

    private final Supplier<RestartFile> file;
    private final UtilsRegistry registry;
    private final Conditions conditions;
    private final Announcer announcer;
    private final Shutdown shutdown;
    private final RestartRunner runner;
    private final Supplier<ZoneId> zone;
    private final Logger log;

    private RestartSchedule schedule;
    private BroadcastSink sink;
    private When when;
    private long stopAt;
    private RestartReason reason = RestartReason.COMMAND;
    private boolean doorClosed;

    public RestartPlan(Supplier<RestartFile> file, UtilsRegistry registry, Conditions conditions, Announcer announcer,
        Shutdown shutdown, RestartRunner runner, Supplier<ZoneId> zone, Logger log) {
        this.file = file;
        this.registry = registry;
        this.conditions = conditions;
        this.announcer = announcer;
        this.shutdown = shutdown;
        this.runner = runner;
        this.zone = zone;
        this.log = log;
    }

    public void arm(long now) {
        RestartFile written = file.get();
        schedule = RestartSchedule.of(written.schedule.at, written.schedule.days, where(), log, zone);
        when = written.schedule.when.toWhen(where(), log);
        sink = registry.sink(word(written.warnings.sink))
            .orElse(null);
        if (written.schedule.enabled && schedule.idle()) {
            log.warn("{}: the schedule is on and holds no moment, the server stops only by command", where());
        }
    }

    @Override
    public Map<String, String> sinks() {
        Map<String, String> named = new LinkedHashMap<>();
        named.put(where(), word(file.get().warnings.sink));
        return named;
    }

    @Override
    public Map<String, String> runners() {
        return Collections.emptyMap();
    }

    @Override
    public List<String> guards() {
        return Collections.emptyList();
    }

    @Override
    public List<String> conditions() {
        RestartFile.Schedule written = file.get().schedule;
        if (!written.enabled || written.when == null || written.when.custom == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(written.when.custom);
    }

    @Override
    public void tick(long from, long to, ServerSnapshot snapshot) {
        if (runner.stopping()) {
            return;
        }
        if (stopAt == 0L) {
            fromSchedule(to, snapshot);
        }
        if (stopAt == 0L) {
            return;
        }
        warn(from, to);
        advance(to);
    }

    public Answer armAt(long stopMillis, RestartReason why) {
        if (runner.stopping()) {
            return Answer.TOO_LATE;
        }
        stopAt = stopMillis;
        reason = why;
        return Answer.ARMED;
    }

    public Answer now(long millis) {
        if (runner.stopping()) {
            return Answer.TOO_LATE;
        }
        stopAt = millis;
        reason = RestartReason.COMMAND;
        advance(millis);
        return Answer.ARMED;
    }

    public Answer cancel(long now) {
        if (runner.stopping()) {
            return Answer.TOO_LATE;
        }
        if (stopAt == 0L) {
            return Answer.NOTHING;
        }
        stopAt = 0L;
        if (doorClosed) {
            shutdown.openDoor();
            doorClosed = false;
        }
        announcer.say(sink, file.get().warnings.prefix, UtilsMessages.RESTART_CANCELLED);
        log.info("The restart is called off, the door is open again");
        return Answer.CANCELLED;
    }

    public boolean armed() {
        return stopAt != 0L;
    }

    public long stopAt() {
        return stopAt;
    }

    public RestartReason reason() {
        return reason;
    }

    public boolean doorClosed() {
        return doorClosed;
    }

    public int secondsLeft(long now) {
        return stopAt == 0L ? 0 : (int) Math.max(0L, (stopAt - now + Clocks.MILLIS - 1) / Clocks.MILLIS);
    }

    public long nextScheduled(long now) {
        RestartFile written = file.get();
        return schedule == null || !written.schedule.enabled ? 0L : schedule.nextStop(now);
    }

    public RestartPlanView view(long now) {
        return RestartPlanView.of(reason, stopAt, secondsLeft(now));
    }

    private void fromSchedule(long now, ServerSnapshot snapshot) {
        RestartFile written = file.get();
        if (schedule == null || !written.schedule.enabled || schedule.idle()) {
            return;
        }
        long next = schedule.nextStop(now);
        if (next == 0L || now < next - lead(written) * Clocks.MILLIS) {
            return;
        }
        if (!conditions.allows(when, snapshot)) {
            return;
        }
        armAt(next, RestartReason.SCHEDULE);
        log.info("The schedule starts a restart, the server stops in {}", Durations.format(secondsLeft(now)));
    }

    private void warn(long from, long to) {
        RestartFile written = file.get();
        for (Integer second : seconds(written)) {
            long at = stopAt - second.intValue() * Clocks.MILLIS;
            if (at > from && at <= to) {
                announcer.say(
                    sink,
                    written.warnings.prefix,
                    UtilsMessages.RESTART_WARN,
                    Durations.format(second.intValue()));
            }
        }
    }

    private void advance(long now) {
        RestartFile written = file.get();
        if (!doorClosed && now >= stopAt - Math.max(0, written.steps.closeDoorSeconds) * Clocks.MILLIS) {
            runner.steps(RestartPhase.BEFORE_DOOR, view(now));
            shutdown.closeDoor(written.messages.doorKey);
            doorClosed = true;
            log.info("The door is closed, the server stops in {}", Durations.format(secondsLeft(now)));
        }
        if (now >= stopAt - Math.max(0, written.steps.kickSeconds) * Clocks.MILLIS) {
            runner.begin(view(now));
        }
    }

    private static Set<Integer> seconds(RestartFile written) {
        Set<Integer> found = new LinkedHashSet<>();
        for (Integer second : written.warnings.seconds) {
            if (second != null && second.intValue() > 0) {
                found.add(second);
            }
        }
        return found;
    }

    private static int lead(RestartFile written) {
        int lead = Math.max(Math.max(0, written.steps.closeDoorSeconds), Math.max(0, written.steps.kickSeconds));
        for (Integer second : seconds(written)) {
            lead = Math.max(lead, second.intValue());
        }
        return lead;
    }

    private static String where() {
        return RestartFile.FILE_NAME;
    }

    private static String word(String value) {
        return value == null ? ""
            : value.trim()
                .toLowerCase(Locale.ROOT);
    }
}
