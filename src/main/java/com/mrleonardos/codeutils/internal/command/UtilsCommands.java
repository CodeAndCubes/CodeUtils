package com.mrleonardos.codeutils.internal.command;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codecore.api.command.ArgumentTypes;
import com.mrleonardos.codecore.api.command.CommandContext;
import com.mrleonardos.codecore.api.command.CommandNode;
import com.mrleonardos.codecore.api.command.CommandService;
import com.mrleonardos.codecore.api.util.Durations;
import com.mrleonardos.codeutils.api.Subsystems;
import com.mrleonardos.codeutils.api.clean.CleanupReport;
import com.mrleonardos.codeutils.api.restart.RestartReason;
import com.mrleonardos.codeutils.api.run.RunOutcome;
import com.mrleonardos.codeutils.internal.Clocks;
import com.mrleonardos.codeutils.internal.Moments;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastEngine;
import com.mrleonardos.codeutils.internal.broadcast.Sent;
import com.mrleonardos.codeutils.internal.clean.CleanupEngine;
import com.mrleonardos.codeutils.internal.job.JobEngine;
import com.mrleonardos.codeutils.internal.restart.RestartPlan;

public final class UtilsCommands {

    private static final String SET = "set";
    private static final String NAME = "name";
    private static final String TEXT = "text";
    private static final String WHEN = "when";
    private static final String RULE = "rule";
    private static final String MODE = "mode";
    private static final String NEVER = "-";

    private static final String CANCEL = "cancel";
    private static final String NOW = "now";

    private final Supplier<CommandRoots> roots;
    private final Supplier<ZoneId> zone;
    private final BroadcastEngine broadcasts;
    private final JobEngine jobs;
    private final CleanupEngine clean;
    private final RestartPlan restart;
    private final UtilsArguments arguments;
    private final UtilsSubjects subjects;
    private final UtilsMaintenance maintenance;
    private final LongSupplier clock;
    private final Logger log;

    public UtilsCommands(Supplier<CommandRoots> roots, Supplier<ZoneId> zone, BroadcastEngine broadcasts,
        JobEngine jobs, CleanupEngine clean, RestartPlan restart, UtilsArguments arguments, UtilsSubjects subjects,
        UtilsMaintenance maintenance, LongSupplier clock, Logger log) {
        this.roots = roots;
        this.zone = zone;
        this.broadcasts = broadcasts;
        this.jobs = jobs;
        this.clean = clean;
        this.restart = restart;
        this.arguments = arguments;
        this.subjects = subjects;
        this.maintenance = maintenance;
        this.clock = clock;
        this.log = log;
    }

    public void register(CommandService commands) {
        for (CommandNode root : roots.get()
            .chosen(allRoots(), log)) {
            commands.register(root);
        }
    }

    public List<CommandNode> allRoots() {
        List<CommandNode> all = new ArrayList<>();
        all.add(codeutils());
        if (restart != null) {
            all.add(restartNode(CommandRoots.RESTART));
        }
        if (broadcasts != null) {
            all.add(
                CommandNode.literal(CommandRoots.BROADCAST)
                    .permission(Nodes.ADMIN_BROADCAST)
                    .usage(UtilsMessages.USAGE_BROADCAST)
                    .arg(TEXT, ArgumentTypes.text())
                    .executes(this::broadcastText));
        }
        return all;
    }

    private CommandNode codeutils() {
        CommandNode root = CommandNode.literal(CommandRoots.CODEUTILS)
            .usage(UtilsMessages.USAGE_CODEUTILS)
            .child(
                CommandNode.literal("status")
                    .permission(Nodes.ADMIN_STATUS)
                    .executes(this::status))
            .child(
                CommandNode.literal("reload")
                    .permission(Nodes.ADMIN_RELOAD)
                    .executes(this::reload));
        if (broadcasts != null) {
            root.child(
                CommandNode.literal("broadcast")
                    .permission(Nodes.ADMIN_BROADCAST)
                    .arg(SET, arguments.setName())
                    .executes(this::broadcastSet));
        }
        if (jobs != null) {
            root.child(
                CommandNode.literal("jobs")
                    .permission(Nodes.ADMIN_JOBS)
                    .executes(this::listJobs)
                    .child(
                        CommandNode.literal("run")
                            .permission(Nodes.ADMIN_JOBS)
                            .arg(NAME, arguments.jobName())
                            .executes(this::runJob)));
        }
        if (clean != null) {
            root.child(
                CommandNode.literal("clean")
                    .permission(Nodes.ADMIN_CLEAN)
                    .optionalArg(RULE, arguments.ruleName())
                    .optionalArg(MODE, arguments.cleanMode())
                    .executes(this::clean));
        }
        if (restart != null) {
            root.child(restartNode(CommandRoots.RESTART));
        }
        return root;
    }

    private CommandNode restartNode(String name) {
        return CommandNode.literal(name)
            .permission(Nodes.ADMIN_RESTART)
            .usage(UtilsMessages.USAGE_RESTART)
            .arg(WHEN, arguments.restartWhen())
            .executes(this::restart);
    }

    private void status(CommandContext context) {
        context.reply(UtilsMessages.STATUS_HEADER);
        line(context, Subsystems.BROADCASTS, broadcasts == null ? null : String.valueOf(broadcasts.workingSets()));
        line(context, Subsystems.JOBS, jobs == null ? null : String.valueOf(jobs.workingJobs()));
        line(context, Subsystems.CLEANUP, clean == null ? null : String.valueOf(clean.workingRules()));
        line(context, Subsystems.RESTART, restart == null ? null : restartLine());
        if (jobs != null) {
            long soonest = soonest();
            context.reply(UtilsMessages.STATUS_NEXT, soonest == 0L ? NEVER : moment(soonest));
        }
    }

    private String restartLine() {
        long now = clock.getAsLong();
        if (restart.armed()) {
            return Durations.format(restart.secondsLeft(now));
        }
        long next = restart.nextScheduled(now);
        return next == 0L ? NEVER : moment(next);
    }

    private void reload(CommandContext context) {
        if (maintenance.reload()) {
            context.reply(UtilsMessages.RELOAD_DONE);
            log.info("Settings were re-read by {}", subjects.actorOf(context));
            return;
        }
        context.replyError(UtilsMessages.RELOAD_FAILED);
    }

    private void broadcastSet(CommandContext context) {
        String name = context.get(SET);
        Sent sent = broadcasts.sendNow(name);
        switch (sent.result()) {
            case DONE:
                context.reply(UtilsMessages.BROADCAST_SENT, Integer.valueOf(sent.recipients()));
                log.info("Broadcast set {} was sent by hand by {}", name, subjects.actorOf(context));
                return;
            case NOBODY:
                context.reply(UtilsMessages.BROADCAST_NOBODY);
                return;
            case SET_OFF:
                context.replyError(UtilsMessages.ERROR_SUBSYSTEM_OFF, name);
                return;
            default:
                context.replyError(UtilsMessages.ERROR_UNKNOWN_SET, name);
        }
    }

    private void broadcastText(CommandContext context) {
        Sent sent = broadcasts.sendPlain(context.get(TEXT));
        if (sent.done()) {
            context.reply(UtilsMessages.BROADCAST_SENT, Integer.valueOf(sent.recipients()));
            return;
        }
        context.reply(UtilsMessages.BROADCAST_NOBODY);
    }

    private void listJobs(CommandContext context) {
        List<String> names = jobs.names();
        if (names.isEmpty()) {
            context.reply(UtilsMessages.JOBS_NONE);
            return;
        }
        context.reply(UtilsMessages.JOBS_HEADER, Integer.valueOf(names.size()));
        long now = clock.getAsLong();
        for (String name : names) {
            long next = jobs.nextMomentOf(name, now);
            context.reply(UtilsMessages.JOBS_ROW, name, jobs.commandOf(name), next == 0L ? NEVER : moment(next));
        }
    }

    private void runJob(CommandContext context) {
        String name = context.get(NAME);
        if (!jobs.knows(name)) {
            context.replyError(
                jobs.written(name) ? UtilsMessages.ERROR_SUBSYSTEM_OFF : UtilsMessages.ERROR_UNKNOWN_JOB,
                name);
            return;
        }
        RunOutcome outcome = jobs.runNow(name);
        if (outcome.successful()) {
            context.reply(UtilsMessages.JOBS_RAN, name);
            log.info("Job {} was run by hand by {}", name, subjects.actorOf(context));
            return;
        }
        context.replyError(UtilsMessages.JOBS_FAILED, name, outcome.reason());
    }

    private void clean(CommandContext context) {
        String name = context.getOrDefault(RULE, "");
        boolean straight = NOW.equalsIgnoreCase(context.getOrDefault(MODE, ""));
        if (name.isEmpty()) {
            for (String rule : clean.names()) {
                counted(context, clean.count(rule));
            }
            if (clean.names()
                .isEmpty()) {
                context.reply(UtilsMessages.JOBS_NONE);
            }
            return;
        }
        if (!clean.knows(name)) {
            context.replyError(
                clean.written(name) ? UtilsMessages.ERROR_SUBSYSTEM_OFF : UtilsMessages.ERROR_UNKNOWN_RULE,
                name);
            return;
        }
        if (!straight) {
            counted(context, clean.count(name));
            return;
        }
        CleanupReport pass = clean.clean(name);
        context.reply(UtilsMessages.CLEAN_DONE, pass.rule(), Integer.valueOf(pass.removed()));
        log.info(
            "Cleanup rule {} was run by hand by {}, {} entity(ies) removed",
            name,
            subjects.actorOf(context),
            Integer.valueOf(pass.removed()));
    }

    private static void counted(CommandContext context, CleanupReport pass) {
        context.reply(UtilsMessages.CLEAN_COUNTED, pass.rule(), Integer.valueOf(pass.matched()));
    }

    private void restart(CommandContext context) {
        String written = context.get(WHEN);
        long now = clock.getAsLong();
        if (CANCEL.equalsIgnoreCase(written)) {
            cancelRestart(context);
            return;
        }
        long stopAt = NOW.equalsIgnoreCase(written) ? now : stopAt(written, now);
        if (stopAt <= 0L) {
            context.replyError(UtilsMessages.USAGE_RESTART);
            return;
        }
        RestartPlan.Answer answer = NOW.equalsIgnoreCase(written) ? restart.now(now)
            : restart.armAt(stopAt, RestartReason.COMMAND);
        if (answer == RestartPlan.Answer.TOO_LATE) {
            context.replyError(UtilsMessages.RESTART_TOO_LATE);
            return;
        }
        context.reply(UtilsMessages.RESTART_ARMED, Durations.format(restart.secondsLeft(now)));
        log.info(
            "A restart in {} was set by {}",
            Durations.format(restart.secondsLeft(now)),
            subjects.actorOf(context));
    }

    private void cancelRestart(CommandContext context) {
        switch (restart.cancel(clock.getAsLong())) {
            case CANCELLED:
                context.reply(UtilsMessages.RESTART_CANCELLED);
                log.info("The restart was called off by {}", subjects.actorOf(context));
                return;
            case NOTHING:
                context.replyError(UtilsMessages.RESTART_NOTHING);
                return;
            default:
                context.replyError(UtilsMessages.RESTART_TOO_LATE);
        }
    }

    private long stopAt(String written, long now) {
        int minute = Moments.parse(written);
        if (minute != Moments.NONE) {
            ZoneId at = zone.get();
            LocalDate day = Clocks.local(now, at)
                .toLocalDate();
            long moment = Clocks.millisOf(day, minute, at);
            return moment > now ? moment : Clocks.millisOf(day.plusDays(1), minute, at);
        }
        int seconds = Durations.toSeconds(written);
        return seconds <= 0 ? 0L : now + seconds * Clocks.MILLIS;
    }

    private void line(CommandContext context, String subsystem, String detail) {
        if (detail == null) {
            context.reply(UtilsMessages.STATUS_OFF, subsystem);
            return;
        }
        context.reply(UtilsMessages.STATUS_ON, subsystem, detail);
    }

    private long soonest() {
        long now = clock.getAsLong();
        long best = 0L;
        for (String name : jobs.names()) {
            long next = jobs.nextMomentOf(name, now);
            if (next > 0L && (best == 0L || next < best)) {
                best = next;
            }
        }
        return best;
    }

    private String moment(long millis) {
        LocalDateTime time = Clocks.local(millis, zone.get());
        return Moments.print(time.getHour() * 60 + time.getMinute());
    }
}
