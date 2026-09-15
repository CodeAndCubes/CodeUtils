package com.mrleonardos.codeutils.internal.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mrleonardos.codecore.api.config.ConfigFile;

class UtilsMaintenanceTest {

    @TempDir
    Path folder;

    private final AtomicInteger rearms = new AtomicInteger();

    @Test
    void aFaultInTheMiddleStopsTheRunAndNamesTheFile() {
        CountingFile broadcasts = new CountingFile("utils-broadcasts.toml");
        CountingFile jobs = new CountingFile("utils-jobs.toml", true);
        CountingFile cleanup = new CountingFile("utils-cleanup.toml");
        UtilsMaintenance maintenance = new UtilsMaintenance(this::rearm, LogManager.getLogger("codeutils-test"))
            .watch(broadcasts)
            .watch(jobs)
            .watch(cleanup);

        String fault = maintenance.reload();

        assertEquals("utils-jobs.toml: broken on purpose", fault);
        assertEquals(1, broadcasts.reloads);
        assertEquals(1, jobs.reloads);
        assertEquals(0, cleanup.reloads, "файлы после сбойного не трогаются вовсе");
        assertEquals(0, rearms.get(), "перевооружения при сбое нет");
    }

    @Test
    void aCleanRunRearmsExactlyOnce() {
        CountingFile settings = new CountingFile("utils.toml");
        CountingFile jobs = new CountingFile("utils-jobs.toml");
        UtilsMaintenance maintenance = new UtilsMaintenance(this::rearm, LogManager.getLogger("codeutils-test"))
            .watch(settings)
            .watch(jobs);

        assertNull(maintenance.reload());

        assertEquals(1, settings.reloads);
        assertEquals(1, jobs.reloads);
        assertEquals(1, rearms.get());
        assertEquals(2, maintenance.watched());
    }

    private void rearm() {
        rearms.incrementAndGet();
    }

    private final class CountingFile implements ConfigFile<Object> {

        private final String name;
        private final boolean broken;

        private int reloads;

        private CountingFile(String name) {
            this(name, false);
        }

        private CountingFile(String name, boolean broken) {
            this.name = name;
            this.broken = broken;
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public boolean loaded() {
            return true;
        }

        @Override
        public void save() {}

        @Override
        public void reload() {
            reloads++;
            if (broken) {
                throw new IllegalStateException("broken on purpose");
            }
        }

        @Override
        public Path path() {
            return folder.resolve(name);
        }
    }
}
