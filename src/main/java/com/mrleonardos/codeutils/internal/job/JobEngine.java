package com.mrleonardos.codeutils.internal.job;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codecore.api.actor.PlayerRef;
import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.api.run.CommandRunner;
import com.mrleonardos.codeutils.api.run.CommandTicket;
import com.mrleonardos.codeutils.api.run.RunOutcome;
import com.mrleonardos.codeutils.api.run.SenderChoice;
import com.mrleonardos.codeutils.api.when.ServerSnapshot;
import com.mrleonardos.codeutils.api.when.When;
import com.mrleonardos.codeutils.internal.Clocks;
import com.mrleonardos.codeutils.internal.Conditions;
import com.mrleonardos.codeutils.internal.ServerFacts;
import com.mrleonardos.codeutils.internal.SpiNames;
import com.mrleonardos.codeutils.internal.Subsystem;
import com.mrleonardos.codeutils.internal.WhenBlock;
import com.mrleonardos.codeutils.internal.Words;
import com.mrleonardos.codeutils.internal.job.JobsFile.JobBlock;

public final class JobEngine implements Subsystem, SpiNames.Source {

    private final Supplier<JobsFile> file;
    private final UtilsRegistry registry;
    private final Conditions conditions;
    private final ServerFacts facts;
    private final Supplier<ZoneId> zone;
    private final Supplier<Boolean> audit;
    private final Logger log;

    private final Map<String, Live> live = new LinkedHashMap<>();

    private boolean started;

    public JobEngine(Supplier<JobsFile> file, UtilsRegistry registry, Conditions conditions, ServerFacts facts,
        Supplier<ZoneId> zone, Supplier<Boolean> audit, Logger log) {
        this.file = file;
        this.registry = registry;
        this.conditions = conditions;
        this.facts = facts;
        this.zone = zone;
        this.audit = audit;
        this.log = log;
    }

    public void arm(long now) {
        Map<String, Long> carried = new LinkedHashMap<>();
        for (Map.Entry<String, Live> entry : live.entrySet()) {
            carried.put(entry.getKey(), Long.valueOf(entry.getValue().previous));
        }
        boolean fresh = !started;
        started = true;
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
            CommandRunner runner = registry.runner(Words.word(block.runner))
                .orElse(null);
            if (runner == null) {
                continue;
            }
            Live job = new Live(name, block, runner);
            if (job.schedule.idle()) {
                log.warn("Job {} has neither everySeconds nor at, it stays quiet", name);
                continue;
            }
            job.schedule.arm(now);
            Long before = carried.get(name);
            if (before != null) {
                job.previous = before.longValue();
            } else if (fresh) {
                job.previous = now - Math.max(0, block.catchUpSeconds) * Clocks.MILLIS;
            } else {
                job.previous = now;
            }
            live.put(name, job);
        }
    }

    @Override
    public Map<String, String> sinks() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> runners() {
        Map<String, String> named = new LinkedHashMap<>();
        for (Map.Entry<String, JobBlock> entry : file.get().jobs.entrySet()) {
            JobBlock block = entry.getValue();
            if (block.enabled && !block.command.trim()
                .isEmpty()) {
                named.put(where(entry.getKey()), Words.word(block.runner));
            }
        }
        return named;
    }

    @Override
    public List<String> guards() {
        return Collections.emptyList();
    }

    @Override
    public List<String> conditions() {
        List<String> named = new ArrayList<>();
        for (JobBlock block : file.get().jobs.values()) {
            if (block.enabled && block.when != null && block.when.custom != null) {
                named.addAll(block.when.custom);
            }
        }
        return named;
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
                fire(job, 1, to);
            }
        }
    }

    public RunOutcome runNow(String name) {
        Live job = live.get(name);
        if (job == null) {
            return RunOutcome.refused(RunOutcome.NO_RUNNER);
        }
        PlayerRef found = playerOf(job);
        if (job.sender.kind() == SenderChoice.Kind.PLAYER && found == null) {
            return RunOutcome.refused(RunOutcome.NO_PLAYER);
        }
        return job.runner.run(ticket(job, found, 1));
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

    private void retry(Live job, long now) {
        if (job.retryAt == 0L || now < job.retryAt) {
            return;
        }
        int attempt = job.attempt;
        job.retryAt = 0L;
        job.attempt = 0;
        if (attempt > 0) {
            fire(job, attempt, now);
        }
    }

    private void fire(Live job, int attempt, long now) {
        PlayerRef found = playerOf(job);
        CommandTicket ticket = ticket(job, found, attempt);
        if (job.sender.kind() == SenderChoice.Kind.PLAYER && found == null) {
            failed(job, ticket, RunOutcome.refused(RunOutcome.NO_PLAYER), now);
            return;
        }
        RunOutcome outcome = job.runner.run(ticket);
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

    private PlayerRef playerOf(Live job) {
        if (job.sender.kind() != SenderChoice.Kind.PLAYER) {
            return null;
        }
        for (PlayerRef player : facts.online()) {
            if (player.name()
                .equalsIgnoreCase(job.sender.nick())) {
                return player;
            }
        }
        return null;
    }

    private static CommandTicket ticket(Live job, PlayerRef player, int attempt) {
        CommandTicket made = CommandTicket.of(job.name, job.block.command, job.sender, player);
        for (int number = 1; number < attempt; number++) {
            made = made.next();
        }
        return made;
    }

    private void failed(Live job, CommandTicket ticket, RunOutcome outcome, long now) {
        String policy = Words.word(job.block.onFailure);
        if (JobsFile.ON_FAILURE_QUIET.equals(policy)) {
            return;
        }
        if (JobsFile.ON_FAILURE_RETRY.equals(policy)) {
            int retries = Math.max(0, job.block.retries);
            if (ticket.attempt() <= retries) {
                job.attempt = ticket.attempt() + 1;
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

    private final class Live {

        private final String name;
        private final JobBlock block;
        private final When when;
        private final SenderChoice sender;
        private final CommandRunner runner;
        private final JobSchedule schedule;

        private long previous;
        private long retryAt;
        private int attempt;
        private boolean policyTold;

        private Live(String name, JobBlock block, CommandRunner runner) {
            this.name = name;
            this.block = block;
            this.runner = runner;
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
