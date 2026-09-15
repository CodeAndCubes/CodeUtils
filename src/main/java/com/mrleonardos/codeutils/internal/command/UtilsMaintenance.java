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

    public String reload() {
        int done = 0;
        for (ConfigFile<?> file : files) {
            try {
                file.reload();
                done++;
            } catch (RuntimeException failure) {
                log.warn(
                    "{} was not re-read, {} of {} file(s) before it were, the rest are not touched "
                        + "and nothing is rearmed: {}",
                    name(file),
                    Integer.valueOf(done),
                    Integer.valueOf(files.size()),
                    failure.toString(),
                    failure);
                return name(file) + ": " + said(failure);
            }
        }
        rearm.run();
        return null;
    }

    private static String said(RuntimeException failure) {
        return failure.getMessage() == null ? failure.toString() : failure.getMessage();
    }

    private static String name(ConfigFile<?> file) {
        return file.path() == null ? "?"
            : file.path()
                .getFileName()
                .toString();
    }
}
