package com.mrleonardos.codeutils.internal.command;

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
import com.mrleonardos.codeutils.api.Subsystems;
import com.mrleonardos.codeutils.api.run.RunOutcome;
import com.mrleonardos.codeutils.internal.Clocks;
import com.mrleonardos.codeutils.internal.Moments;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastEngine;
import com.mrleonardos.codeutils.internal.broadcast.Sent;
import com.mrleonardos.codeutils.internal.job.JobEngine;

public final class UtilsCommands {

    private static final String SET = "set";
    private static final String NAME = "name";
    private static final String TEXT = "text";
    private static final String NEVER = "-";

    private final Supplier<CommandRoots> roots;
    private final Supplier<ZoneId> zone;
    private final BroadcastEngine broadcasts;
    private final JobEngine jobs;
    private final UtilsArguments arguments;
    private final UtilsSubjects subjects;
    private final UtilsMaintenance maintenance;
    private final LongSupplier clock;
    private final Logger log;

    public UtilsCommands(Supplier<CommandRoots> roots, Supplier<ZoneId> zone, BroadcastEngine broadcasts,
        JobEngine jobs, UtilsArguments arguments, UtilsSubjects subjects, UtilsMaintenance maintenance,
        LongSupplier clock, Logger log) {
        this.roots = roots;
        this.zone = zone;
        this.broadcasts = broadcasts;
        this.jobs = jobs;
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
        return root;
    }

    private void status(CommandContext context) {
        context.reply(UtilsMessages.STATUS_HEADER);
        line(context, Subsystems.BROADCASTS, broadcasts == null ? null : String.valueOf(broadcasts.workingSets()));
        line(context, Subsystems.JOBS, jobs == null ? null : String.valueOf(jobs.workingJobs()));
        if (jobs != null) {
            long soonest = soonest();
            context.reply(UtilsMessages.STATUS_NEXT, soonest == 0L ? NEVER : moment(soonest));
        }
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
