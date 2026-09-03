package com.mrleonardos.codeutils.internal.command;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codecore.api.config.ConfigFile;

public final class UtilsMaintenance {

    private final List<ConfigFile<?>> files = new ArrayList<>();
    private final Runnable rearm;
    private final Logger log;

    public UtilsMaintenance(Runnable rearm, Logger log) {
        this.rearm = rearm;
        this.log = log;
    }

    public UtilsMaintenance watch(ConfigFile<?> file) {
        if (file != null) {
            files.add(file);
        }
        return this;
    }

    public int watched() {
        return files.size();
    }

    public boolean reload() {
        try {
            for (ConfigFile<?> file : files) {
                file.reload();
            }
            rearm.run();
            return true;
        } catch (RuntimeException failure) {
            log.warn("Settings were not re-read, the mod keeps what it had: {}", failure.toString(), failure);
            return false;
        }
    }
}
