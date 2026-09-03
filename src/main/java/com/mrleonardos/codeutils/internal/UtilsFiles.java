package com.mrleonardos.codeutils.internal;

import com.mrleonardos.codecore.api.config.ConfigFile;
import com.mrleonardos.codecore.api.config.ConfigService;
import com.mrleonardos.codeutils.api.Subsystems;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile;
import com.mrleonardos.codeutils.internal.command.CommandRoots;
import com.mrleonardos.codeutils.internal.job.JobsFile;
import com.mrleonardos.codeutils.internal.restart.RestartFile;

public final class UtilsFiles {

    private final ConfigFile<UtilsSettings> settings;
    private final ConfigFile<CommandRoots> roots;
    private final ConfigFile<BroadcastsFile> broadcasts;
    private final ConfigFile<JobsFile> jobs;
    private final ConfigFile<RestartFile> restart;

    private UtilsFiles(ConfigFile<UtilsSettings> settings, ConfigFile<CommandRoots> roots,
        ConfigFile<BroadcastsFile> broadcasts, ConfigFile<JobsFile> jobs, ConfigFile<RestartFile> restart) {
        this.settings = settings;
        this.roots = roots;
        this.broadcasts = broadcasts;
        this.jobs = jobs;
        this.restart = restart;
    }

    public static UtilsFiles open(ConfigService configs) {
        ConfigFile<UtilsSettings> settings = configs.open(UtilsSettings.spec());
        ConfigFile<CommandRoots> roots = configs.open(CommandRoots.spec());
        UtilsSettings written = settings.get();
        ConfigFile<BroadcastsFile> broadcasts = written.enabled(Subsystems.BROADCASTS)
            ? configs.open(BroadcastsFile.spec())
            : null;
        ConfigFile<JobsFile> jobs = written.enabled(Subsystems.JOBS) ? configs.open(JobsFile.spec()) : null;
        ConfigFile<RestartFile> restart = written.enabled(Subsystems.RESTART) ? configs.open(RestartFile.spec()) : null;
        return new UtilsFiles(settings, roots, broadcasts, jobs, restart);
    }

    public ConfigFile<UtilsSettings> settings() {
        return settings;
    }

    public ConfigFile<CommandRoots> roots() {
        return roots;
    }

    public ConfigFile<BroadcastsFile> broadcasts() {
        return broadcasts;
    }

    public ConfigFile<JobsFile> jobs() {
        return jobs;
    }

    public ConfigFile<RestartFile> restart() {
        return restart;
    }
}
