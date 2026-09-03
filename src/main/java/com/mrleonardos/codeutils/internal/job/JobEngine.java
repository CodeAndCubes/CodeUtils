package com.mrleonardos.codeutils.internal.job;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.api.run.CommandRunner;
import com.mrleonardos.codeutils.api.run.CommandTicket;
import com.mrleonardos.codeutils.api.run.RunOutcome;
import com.mrleonardos.codeutils.api.run.SenderChoice;
import com.mrleonardos.codeutils.api.when.ServerSnapshot;
import com.mrleonardos.codeutils.api.when.When;
import com.mrleonardos.codeutils.internal.Clocks;
import com.mrleonardos.codeutils.internal.Conditions;
import com.mrleonardos.codeutils.internal.Subsystem;
import com.mrleonardos.codeutils.internal.WhenBlock;
import com.mrleonardos.codeutils.internal.job.JobsFile.JobBlock;

public final class JobEngine implements Subsystem {

    /** Имя встроенного исполнителя: он отдаёт строку менеджеру команд сервера. */
    public static final String SERVER_RUNNER = "server";

    private final Supplier<JobsFile> file;
    private final UtilsRegistry registry;
    private final Conditions conditions;
    private final Supplier<ZoneId> zone;
    private final Supplier<Boolean> audit;
    private final Logger log;

    private final Map<String, Live> live = new LinkedHashMap<>();

    public JobEngine(Supplier<JobsFile> file, UtilsRegistry registry, Conditions conditions, Supplier<ZoneId> zone,
        Supplier<Boolean> audit, Logger log) {
        this.file = file;
        this.registry = registry;
        this.conditions = conditions;
        this.zone = zone;
        this.audit = audit;
        this.log = log;
    }

    public void arm(long now) {
        live.clear();
        for (Map.Entry<String, JobBlock> entry : file.get().jobs.entrySet()) {
            String name = entry.getKey();
            JobBlock block = entry.getValue();
            if (!block.enabled) {
                continue;
            }
            if (block.command.trim()
                .isEmpty()) {
                log.warn("Job {} holds no command, it stays quiet", name);
                continue;
            }
            Live job = new Live(name, block);
            if (job.schedule.idle()) {
                log.warn("Job {} has neither everySeconds nor at, it stays quiet", name);
                continue;
            }
            job.schedule.arm(now);
            job.previous = now - Math.max(0, block.catchUpSeconds) * Clocks.MILLIS;
            live.put(name, job);
        }
        conditions.report(whens(), log);
    }

    @Override
    public void tick(long from, long to, ServerSnapshot snapshot) {
        for (Live job : live.values()) {
            retry(job, to);
            int due = job.schedule.due(job.previous, to);
            job.previous = to;
            if (due <= 0) {
                continue;
            }
            if (!conditions.allows(job.when, snapshot)) {
                continue;
            }
            for (int shot = 0; shot < due; shot++) {
                fire(job, CommandTicket.of(job.name, job.block.command, job.sender), to);
            }
        }
    }

    public RunOutcome runNow(String name) {
        Live job = live.get(name);
        if (job == null) {
            return RunOutcome.refused(RunOutcome.NO_RUNNER);
        }
        return run(CommandTicket.of(job.name, job.block.command, job.sender));
    }

    public boolean knows(String name) {
        return live.containsKey(name);
    }

    public boolean written(String name) {
        return file.get().jobs.containsKey(name);
    }

    public List<String> names() {
        return new ArrayList<>(live.keySet());
    }

    public int workingJobs() {
        return live.size();
    }

    public long nextMomentOf(String name, long now) {
        Live job = live.get(name);
        return job == null ? 0L : job.schedule.nextMoment(now);
    }

    public String commandOf(String name) {
        Live job = live.get(name);
        return job == null ? "" : job.block.command;
    }

    public List<When> whens() {
        List<When> all = new ArrayList<>();
        for (Live job : live.values()) {
            all.add(job.when);
        }
        return all;
    }

    private void retry(Live job, long now) {
        if (job.retryAt == 0L || now < job.retryAt) {
            return;
        }
        CommandTicket ticket = job.pending;
        job.retryAt = 0L;
        job.pending = null;
        if (ticket != null) {
            fire(job, ticket, now);
        }
    }

    private void fire(Live job, CommandTicket ticket, long now) {
        RunOutcome outcome = run(ticket);
        if (outcome.successful()) {
            if (audit.get()
                .booleanValue()) {
                log.info(
                    "Job {} ran {} and it worked {} time(s)",
                    job.name,
                    ticket.command(),
                    Integer.valueOf(outcome.count()));
            }
            return;
        }
        failed(job, ticket, outcome, now);
    }

    private RunOutcome run(CommandTicket ticket) {
        CommandRunner runner = registry.runner(SERVER_RUNNER)
            .orElse(null);
        return runner == null ? RunOutcome.refused(RunOutcome.NO_RUNNER) : runner.run(ticket);
    }

    private void failed(Live job, CommandTicket ticket, RunOutcome outcome, long now) {
        String policy = word(job.block.onFailure);
        if (JobsFile.ON_FAILURE_QUIET.equals(policy)) {
            return;
        }
        if (JobsFile.ON_FAILURE_RETRY.equals(policy)) {
            int retries = Math.max(0, job.block.retries);
            if (ticket.attempt() <= retries) {
                job.pending = ticket.next();
                job.retryAt = now + Math.max(1, job.block.retrySeconds) * Clocks.MILLIS;
                return;
            }
            log.warn(
                "Job {} did not work in {} attempt(s), the command was {} and the last answer was {}",
                job.name,
                Integer.valueOf(ticket.attempt()),
                ticket.command(),
                outcome.reason());
            return;
        }
        if (!JobsFile.ON_FAILURE_LOG.equals(policy) && !job.policyTold) {
            job.policyTold = true;
            log.warn(
                "{}: onFailure = {} is not one of log, quiet, retry, log is used",
                where(job.name),
                job.block.onFailure);
        }
        log.warn(
            "Job {} did not work, the command was {} and the answer was {}",
            job.name,
            ticket.command(),
            outcome.reason());
    }

    private static String where(String name) {
        return JobsFile.FILE_NAME + " job " + name;
    }

    private static String word(String value) {
        return value == null ? ""
            : value.trim()
                .toLowerCase(Locale.ROOT);
    }

    private final class Live {

        private final String name;
        private final JobBlock block;
        private final When when;
        private final SenderChoice sender;
        private final JobSchedule schedule;

        private long previous;
        private long retryAt;
        private CommandTicket pending;
        private boolean policyTold;

        private Live(String name, JobBlock block) {
            this.name = name;
            this.block = block;
            WhenBlock written = block.when == null ? new WhenBlock() : block.when;
            this.when = written.toWhen(where(name), log);
            this.sender = SenderChoice.parse(block.as);
            if (!sender.known()) {
                log.warn(
                    "{}: as = {} is none of console, commandblock, player:<nick>, console is used",
                    where(name),
                    block.as);
            }
            this.schedule = JobSchedule.of(block.at, block.everySeconds, where(name), log, zone);
        }
    }
}
