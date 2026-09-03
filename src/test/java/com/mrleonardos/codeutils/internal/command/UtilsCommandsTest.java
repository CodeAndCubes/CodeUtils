package com.mrleonardos.codeutils.internal.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.mrleonardos.codecore.api.command.CommandNode;
import com.mrleonardos.codecore.api.command.CommandSender;
import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.internal.Clocks;
import com.mrleonardos.codeutils.internal.Conditions;
import com.mrleonardos.codeutils.internal.FakeFacts;
import com.mrleonardos.codeutils.internal.WhenBlock;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastEngine;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile.MessageBlock;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile.SetBlock;
import com.mrleonardos.codeutils.internal.broadcast.RecordingSink;
import com.mrleonardos.codeutils.internal.job.JobEngine;
import com.mrleonardos.codeutils.internal.job.JobsFile;
import com.mrleonardos.codeutils.internal.job.JobsFile.JobBlock;
import com.mrleonardos.codeutils.internal.job.RecordingRunner;

class UtilsCommandsTest {

    private static final Logger LOG = LogManager.getLogger("codeutils-test");

    private static final ZoneId ZONE = ZoneId.of("UTC");

    private final UtilsRegistry registry = new UtilsRegistry();
    private final RecordingSink sink = new RecordingSink();
    private final RecordingRunner runner = new RecordingRunner();
    private final FakeFacts facts = new FakeFacts();
    private final BroadcastsFile broadcastsFile = new BroadcastsFile();
    private final JobsFile jobsFile = new JobsFile();
    private final CommandRoots roots = new CommandRoots();

    private boolean reloaded;

    UtilsCommandsTest() {
        registry.addSink(BroadcastsFile.SINK_CHAT, sink);
        registry.addRunner(JobEngine.SERVER_RUNNER, runner);
        broadcastsFile.sets = new LinkedHashMap<>();
        broadcastsFile.sets.put("tips", set("первое", "второе"));
        jobsFile.jobs = new LinkedHashMap<>();
        jobsFile.jobs.put("save", job("save-all"));
    }

    @Test
    void bothRootsAreRegisteredWhenBothSubsystemsWork() {
        TestCommandService commands = register(true, true);

        assertEquals(Arrays.asList(CommandRoots.CODEUTILS, CommandRoots.BROADCAST), commands.names());
        assertEquals(
            Arrays.asList("status", "reload", "broadcast", "jobs"),
            TestCommandService.childNames(commands.root(CommandRoots.CODEUTILS)));
    }

    @Test
    void aSubsystemThatIsOffRegistersNoBranchOfItsOwn() {
        TestCommandService commands = register(false, true);

        assertEquals(Collections.singletonList(CommandRoots.CODEUTILS), commands.names());
        assertEquals(
            Arrays.asList("status", "reload", "jobs"),
            TestCommandService.childNames(commands.root(CommandRoots.CODEUTILS)));

        TestCommandService without = register(true, false);

        assertEquals(
            Arrays.asList("status", "reload", "broadcast"),
            TestCommandService.childNames(without.root(CommandRoots.CODEUTILS)));
    }

    @Test
    void everyBranchSitsUnderItsOwnNodeAndTheRootCarriesNone() {
        TestCommandService commands = register(true, true);
        CommandNode root = commands.root(CommandRoots.CODEUTILS);

        assertNull(root.permissionNode(), "пустой вызов показывает доступные ветки, узла у корня нет");
        assertEquals(
            Nodes.ADMIN_STATUS,
            TestCommandService.child(root, "status")
                .permissionNode());
        assertEquals(
            Nodes.ADMIN_RELOAD,
            TestCommandService.child(root, "reload")
                .permissionNode());
        assertEquals(
            Nodes.ADMIN_BROADCAST,
            TestCommandService.child(root, "broadcast")
                .permissionNode());
        assertEquals(
            Nodes.ADMIN_JOBS,
            TestCommandService.child(root, "jobs")
                .permissionNode());
        assertEquals(
            Nodes.ADMIN_BROADCAST,
            commands.root(CommandRoots.BROADCAST)
                .permissionNode());
    }

    @Test
    void aRootTurnedOffInTheFileIsNotRegistered() {
        roots.commands.get(CommandRoots.BROADCAST).enabled = false;

        TestCommandService commands = register(true, true);

        assertEquals(Collections.singletonList(CommandRoots.CODEUTILS), commands.names());
        assertNotNull(
            TestCommandService.child(commands.root(CommandRoots.CODEUTILS), "broadcast"),
            "рассылка остаётся доступной через /codeutils broadcast");
    }

    @Test
    void statusNamesEverySubsystemAndTheNearestJobMoment() {
        TestCommandService commands = register(true, true);
        TestCommandContext context = new TestCommandContext();

        run(commands, "status", context);

        assertEquals(
            Arrays.asList(
                UtilsMessages.STATUS_HEADER,
                UtilsMessages.STATUS_ON,
                UtilsMessages.STATUS_ON,
                UtilsMessages.STATUS_NEXT),
            context.keys());
        assertEquals("04:00", context.last().arguments.get(0));
    }

    @Test
    void statusSaysOffForTheSubsystemThatIsNotWorking() {
        TestCommandService commands = register(false, true);
        TestCommandContext context = new TestCommandContext();

        run(commands, "status", context);

        assertEquals(
            Arrays.asList(
                UtilsMessages.STATUS_HEADER,
                UtilsMessages.STATUS_OFF,
                UtilsMessages.STATUS_ON,
                UtilsMessages.STATUS_NEXT),
            context.keys());
    }

    @Test
    void broadcastByHandSendsTheNextMessageAndSaysHowManyHeardIt() {
        facts.join("Steve", 0);
        TestCommandService commands = register(true, true);
        TestCommandContext context = new TestCommandContext().set("set", "tips");

        run(commands, "broadcast", context);

        assertEquals(UtilsMessages.BROADCAST_SENT, context.last().key);
        assertEquals(Integer.valueOf(1), context.last().arguments.get(0));
        assertEquals(Arrays.asList("первое"), sink.texts());
    }

    @Test
    void aTypoInTheSetNameIsAnsweredWithItsOwnKey() {
        facts.join("Steve", 0);
        TestCommandService commands = register(true, true);
        TestCommandContext context = new TestCommandContext().set("set", "tipz");

        run(commands, "broadcast", context);

        assertEquals(UtilsMessages.ERROR_UNKNOWN_SET, context.last().key);
        assertTrue(context.last().error);
    }

    @Test
    void withNobodyOnlineTheAnswerSaysSo() {
        TestCommandService commands = register(true, true);
        TestCommandContext context = new TestCommandContext().set("set", "tips");

        run(commands, "broadcast", context);

        assertEquals(UtilsMessages.BROADCAST_NOBODY, context.last().key);
        assertEquals(0, sink.sends());
    }

    @Test
    void aPlainLineFromTheOwnRootGoesToEverybody() {
        facts.join("Steve", 0);
        facts.join("Alex", -1);
        TestCommandService commands = register(true, true);
        TestCommandContext context = new TestCommandContext().set("text", "&aвсем привет");

        commands.root(CommandRoots.BROADCAST)
            .action()
            .run(context);

        assertEquals(UtilsMessages.BROADCAST_SENT, context.last().key);
        assertEquals(Arrays.asList("§aвсем привет"), sink.texts());
    }

    @Test
    void theJobListNamesEveryJobWithItsCommandAndMoment() {
        TestCommandService commands = register(true, true);
        TestCommandContext context = new TestCommandContext();

        run(commands, "jobs", context);

        assertEquals(Arrays.asList(UtilsMessages.JOBS_HEADER, UtilsMessages.JOBS_ROW), context.keys());
        assertEquals(Arrays.asList("save", "save-all", "04:00"), context.last().arguments);
    }

    @Test
    void aJobIsRunByHandFromItsOwnBranch() {
        TestCommandService commands = register(true, true);
        TestCommandContext context = new TestCommandContext().set("name", "save");

        runChild(commands, "jobs", "run", context);

        assertEquals(UtilsMessages.JOBS_RAN, context.last().key);
        assertEquals(Arrays.asList("save-all"), runner.commands());
    }

    @Test
    void aTypoInTheJobNameIsAnsweredWithItsOwnKey() {
        TestCommandService commands = register(true, true);
        TestCommandContext context = new TestCommandContext().set("name", "svae");

        runChild(commands, "jobs", "run", context);

        assertEquals(UtilsMessages.ERROR_UNKNOWN_JOB, context.last().key);
        assertEquals(0, runner.runs());
    }

    @Test
    void aJobThatIsOffAnswersThatItIsOffAndNotThatItIsMissing() {
        jobsFile.jobs.get("save").enabled = false;
        TestCommandService commands = register(true, true);
        TestCommandContext context = new TestCommandContext().set("name", "save");

        runChild(commands, "jobs", "run", context);

        assertEquals(UtilsMessages.ERROR_SUBSYSTEM_OFF, context.last().key);
    }

    @Test
    void reloadRereadsTheFilesAndSaysSo() {
        TestCommandService commands = register(true, true);
        TestCommandContext context = new TestCommandContext();

        run(commands, "reload", context);

        assertEquals(UtilsMessages.RELOAD_DONE, context.last().key);
        assertTrue(reloaded, "перечитывание переставляет дедлайны");
        assertFalse(context.last().error);
    }

    @Test
    void nameCompletionOffersTheSetsAndTheJobs() {
        BroadcastEngine broadcasts = broadcasts();
        JobEngine jobs = jobs();
        broadcasts.arm(0L);
        jobs.arm(0L);
        UtilsArguments arguments = new UtilsArguments(broadcasts::names, jobs::names);

        CommandSender nobody = null;
        assertEquals(
            Arrays.asList("tips"),
            arguments.setName()
                .suggestions(nobody, "ti"));
        assertTrue(
            arguments.setName()
                .suggestions(nobody, "z")
                .isEmpty());
        assertEquals(
            Arrays.asList("save"),
            arguments.jobName()
                .suggestions(nobody, "sa"));
    }

    private TestCommandService register(boolean withBroadcasts, boolean withJobs) {
        BroadcastEngine broadcasts = withBroadcasts ? broadcasts() : null;
        JobEngine jobs = withJobs ? jobs() : null;
        long now = at("2026-09-03T03:00:00");
        if (broadcasts != null) {
            broadcasts.arm(now);
        }
        if (jobs != null) {
            jobs.arm(now);
        }
        UtilsArguments arguments = new UtilsArguments(
            () -> broadcasts == null ? new ArrayList<>() : broadcasts.names(),
            () -> jobs == null ? new ArrayList<>() : jobs.names());
        UtilsMaintenance maintenance = new UtilsMaintenance(() -> reloaded = true, LOG);
        TestCommandService commands = new TestCommandService();
        new UtilsCommands(
            () -> roots,
            () -> ZONE,
            broadcasts,
            jobs,
            arguments,
            context -> "console",
            maintenance,
            () -> now,
            LOG).register(commands);
        return commands;
    }

    private BroadcastEngine broadcasts() {
        return new BroadcastEngine(
            () -> broadcastsFile,
            registry,
            new Conditions(registry),
            facts,
            new Random(1L),
            LOG);
    }

    private JobEngine jobs() {
        return new JobEngine(() -> jobsFile, registry, new Conditions(registry), () -> ZONE, () -> Boolean.FALSE, LOG);
    }

    private static void run(TestCommandService commands, String branch, TestCommandContext context) {
        TestCommandService.child(commands.root(CommandRoots.CODEUTILS), branch)
            .action()
            .run(context);
    }

    private static void runChild(TestCommandService commands, String branch, String leaf, TestCommandContext context) {
        CommandNode node = TestCommandService.child(commands.root(CommandRoots.CODEUTILS), branch);
        TestCommandService.child(node, leaf)
            .action()
            .run(context);
    }

    private static SetBlock set(String... messages) {
        SetBlock block = new SetBlock();
        block.intervalSeconds = 600;
        block.firstDelaySeconds = 0;
        block.prefix = "";
        block.when = new WhenBlock();
        block.messages = new ArrayList<>();
        for (String text : messages) {
            block.messages.add(MessageBlock.of(text));
        }
        return block;
    }

    private static JobBlock job(String command) {
        JobBlock block = new JobBlock();
        block.command = command;
        block.at = new ArrayList<>(Collections.singletonList("04:00"));
        block.when = new WhenBlock();
        return block;
    }

    private static long at(String time) {
        return Clocks.millisOf(LocalDateTime.parse(time), ZONE);
    }
}
