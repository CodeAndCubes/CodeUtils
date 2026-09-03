package com.mrleonardos.codeutils.platform;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mrleonardos.codecore.api.CodeApi;
import com.mrleonardos.codecore.api.config.ConfigFile;
import com.mrleonardos.codecore.api.config.ConfigRoles;
import com.mrleonardos.codecore.api.config.ConfigService;
import com.mrleonardos.codeutils.Tags;
import com.mrleonardos.codeutils.api.CodeUtilsApi;
import com.mrleonardos.codeutils.api.Subsystems;
import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.internal.Announcer;
import com.mrleonardos.codeutils.internal.Conditions;
import com.mrleonardos.codeutils.internal.ServerFacts;
import com.mrleonardos.codeutils.internal.SpiNames;
import com.mrleonardos.codeutils.internal.Ticker;
import com.mrleonardos.codeutils.internal.UtilsBeat;
import com.mrleonardos.codeutils.internal.UtilsFiles;
import com.mrleonardos.codeutils.internal.UtilsSettings;
import com.mrleonardos.codeutils.internal.UtilsTexts;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastEngine;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile;
import com.mrleonardos.codeutils.internal.broadcast.LogSink;
import com.mrleonardos.codeutils.internal.clean.CleanupEngine;
import com.mrleonardos.codeutils.internal.clean.CleanupFile;
import com.mrleonardos.codeutils.internal.command.CommandRoots;
import com.mrleonardos.codeutils.internal.command.UtilsArguments;
import com.mrleonardos.codeutils.internal.command.UtilsCommands;
import com.mrleonardos.codeutils.internal.command.UtilsMaintenance;
import com.mrleonardos.codeutils.internal.command.UtilsMessages;
import com.mrleonardos.codeutils.internal.job.JobEngine;
import com.mrleonardos.codeutils.internal.job.JobsFile;
import com.mrleonardos.codeutils.internal.restart.RestartFile;
import com.mrleonardos.codeutils.internal.restart.RestartPlan;
import com.mrleonardos.codeutils.internal.restart.RestartRunner;
import com.mrleonardos.codeutils.internal.restart.Shutdown;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;

@Mod(
    modid = "codeutils",
    name = "CodeUtils",
    version = Tags.VERSION,
    dependencies = "required-after:codecore",
    acceptableRemoteVersions = "*")
public final class CodeUtilsMod {

    public static final Logger LOG = LogManager.getLogger("CodeUtils");

    private final UtilsRegistry registry = new UtilsRegistry();
    private final LongSupplier clock = System::currentTimeMillis;

    private final LoginDoor door = new LoginDoor();
    private final UtilsTexts texts = new GameTexts();

    private ConfigService configs;
    private ConfigFile<UtilsSettings> settings;
    private ConfigFile<CommandRoots> roots;
    private ConfigFile<BroadcastsFile> broadcastsFile;
    private ConfigFile<JobsFile> jobsFile;
    private ConfigFile<RestartFile> restartFile;
    private ConfigFile<CleanupFile> cleanupFile;

    private BroadcastEngine broadcasts;
    private JobEngine jobs;
    private CleanupEngine clean;
    private RestartPlan restart;
    private SpiNames names;
    private Ticker ticker;
    private LoginGate loginGate;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOG.info("CodeUtils {} is starting up", Tags.VERSION);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        configs = CodeApi.configs();
        UtilsFiles files = UtilsFiles.open(configs);
        settings = files.settings();
        roots = files.roots();
        broadcastsFile = files.broadcasts();
        jobsFile = files.jobs();
        restartFile = files.restart();
        cleanupFile = files.cleanup();

        registry.addSink(BroadcastsFile.SINK_CHAT, new ChatSink());
        registry.addSink(BroadcastsFile.SINK_LOG, new LogSink(LOG));
        registry.addRunner(JobsFile.SERVER_RUNNER, new ServerCommandRunner());
        CodeUtilsApi.install(new UtilsRuntimeImpl(registry, settings::get));

        ServerFacts facts = new ServerFactsImpl();
        Supplier<ZoneId> zone = this::zone;
        Conditions conditions = new Conditions(registry);
        UtilsBeat beat = new UtilsBeat(facts, zone);
        names = new SpiNames(registry, LOG);

        if (restartFile != null) {
            Shutdown shutdown = new ShutdownImpl(door, texts, LOG);
            RestartRunner stopping = new RestartRunner(
                restartFile::get,
                registry,
                shutdown,
                CodeApi.scheduler(),
                this::stopTicker,
                LOG);
            restart = new RestartPlan(
                restartFile::get,
                registry,
                conditions,
                new Announcer(facts, texts),
                shutdown,
                stopping,
                zone,
                LOG);
            beat.add(restart)
                .pause(stopping::stopping);
            names.add(restart);
        }
        if (broadcastsFile != null) {
            broadcasts = new BroadcastEngine(broadcastsFile::get, registry, conditions, facts, new Random(), LOG);
            beat.add(broadcasts);
            names.add(broadcasts);
        }
        if (jobsFile != null) {
            jobs = new JobEngine(jobsFile::get, registry, conditions, facts, zone, this::audit, LOG);
            beat.add(jobs);
            names.add(jobs);
        }
        if (cleanupFile != null) {
            clean = new CleanupEngine(
                cleanupFile::get,
                registry,
                conditions,
                new WorldSweep(),
                new Announcer(facts, texts),
                this::slowPassMillis,
                this::audit,
                LOG);
            beat.add(clean);
            names.add(clean);
        }

        ticker = new Ticker(CodeApi.scheduler(), clock, beat);
        commands();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        registry.freeze();
        names.resolve();
        if (restart != null) {
            loginGate = new LoginGate(door, texts);
            FMLCommonHandler.instance()
                .bus()
                .register(loginGate);
        }
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        settings.get()
            .zone(LOG);
        rearm();
        ticker.start();
        summary();
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        stopTicker();
        door.close(restartFile == null ? UtilsMessages.RESTART_DOOR : restartFile.get().messages.doorKey);
        if (loginGate != null) {
            FMLCommonHandler.instance()
                .bus()
                .unregister(loginGate);
            loginGate = null;
        }
    }

    private void stopTicker() {
        if (ticker != null) {
            ticker.stop();
        }
    }

    private void commands() {
        UtilsArguments arguments = new UtilsArguments(this::setNames, this::jobNames, this::ruleNames);
        UtilsMaintenance maintenance = new UtilsMaintenance(this::rearm, LOG).watch(settings)
            .watch(broadcastsFile)
            .watch(jobsFile)
            .watch(restartFile)
            .watch(cleanupFile);
        new UtilsCommands(
            roots::get,
            this::zone,
            broadcasts,
            jobs,
            clean,
            restart,
            arguments,
            new SenderSubjects(),
            maintenance,
            clock,
            LOG).register(CodeApi.commands());
        completeRoots();
    }

    private void rearm() {
        long now = clock.getAsLong();
        names.resolve();
        if (broadcasts != null) {
            broadcasts.arm(now);
        }
        if (jobs != null) {
            jobs.arm(now);
        }
        if (clean != null) {
            clean.arm(now);
        }
        if (restart != null) {
            restart.arm(now);
        }
    }

    private void completeRoots() {
        if (!roots.get()
            .filledIn()) {
            return;
        }
        try {
            roots.save();
            LOG.info("Records of the new command root(s) are written to {}", CommandRoots.FILE_NAME);
        } catch (RuntimeException failure) {
            LOG.warn(
                "{} was not written, the new command root(s) stay only in memory: {}",
                CommandRoots.FILE_NAME,
                failure.toString(),
                failure);
        }
    }

    private void summary() {
        if (!settings.get()
            .startupSummary()) {
            return;
        }
        List<String> live = new ArrayList<>();
        if (broadcasts != null) {
            live.add(Subsystems.BROADCASTS + " (" + broadcasts.workingSets() + ")");
        }
        if (jobs != null) {
            live.add(Subsystems.JOBS + " (" + jobs.workingJobs() + ")");
        }
        if (clean != null) {
            live.add(Subsystems.CLEANUP + " (" + clean.workingRules() + ")");
        }
        if (restart != null) {
            live.add(Subsystems.RESTART);
        }
        LOG.info(
            "CodeUtils is up on server {} in zone {}, working subsystems: {}",
            configs.serverId(),
            zone().getId(),
            live.isEmpty() ? "none" : String.join(", ", live));
    }

    private List<String> setNames() {
        return broadcasts == null ? new ArrayList<>() : broadcasts.names();
    }

    private List<String> jobNames() {
        return jobs == null ? new ArrayList<>() : jobs.names();
    }

    private List<String> ruleNames() {
        return clean == null ? new ArrayList<>() : clean.names();
    }

    private int slowPassMillis() {
        return settings.get()
            .slowPassMillis();
    }

    private ZoneId zone() {
        return settings.get()
            .zone();
    }

    private Boolean audit() {
        return Boolean.valueOf(
            configs.audit(ConfigRoles.UTILS)
                .logChanges());
    }
}
