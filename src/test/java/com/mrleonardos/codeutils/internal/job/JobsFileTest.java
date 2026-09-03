package com.mrleonardos.codeutils.internal.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mrleonardos.codecore.api.config.ConfigFile;
import com.mrleonardos.codecore.api.config.ConfigFormat;
import com.mrleonardos.codecore.api.config.ConfigRoles;
import com.mrleonardos.codeutils.TestConfigs;
import com.mrleonardos.codeutils.internal.UtilsSettings;
import com.mrleonardos.codeutils.internal.job.JobsFile.JobBlock;

class JobsFileTest {

    @Test
    void theFileSitsBesideTheOtherSettings() {
        assertEquals(
            JobsFile.FILE,
            JobsFile.spec()
                .name());
        assertEquals(
            UtilsSettings.MODID,
            JobsFile.spec()
                .modid());
        assertEquals(
            ConfigRoles.UTILS,
            JobsFile.spec()
                .role());
        assertEquals(
            ConfigFormat.TOML,
            JobsFile.spec()
                .format());
    }

    @Test
    void theFactoryJobIsOffUntilSomebodyReadsWhatItDoes() {
        JobBlock clean = new JobsFile().jobs.get(JobsFile.NIGHT_CLEAN);

        assertFalse(clean.enabled);
        assertEquals("codeutils clean drops", clean.command);
        assertEquals(0, clean.everySeconds);
        assertEquals(Arrays.asList("04:00"), clean.at);
        assertEquals("console", clean.as);
        assertEquals(JobsFile.SERVER_RUNNER, clean.runner);
        assertEquals(JobsFile.ON_FAILURE_LOG, clean.onFailure);
        assertEquals(JobsFile.DEFAULT_RETRY_SECONDS, clean.retrySeconds);
        assertEquals(0, clean.retries);
        assertEquals(0, clean.catchUpSeconds);
    }

    @Test
    void theFactoryFileIsReadBackIntoTheSameValues(@TempDir Path root) {
        ConfigFile<JobsFile> opened = TestConfigs.of(root.resolve("config"))
            .open(JobsFile.spec());
        JobBlock written = opened.get().jobs.get(JobsFile.NIGHT_CLEAN);

        opened.reload();
        JobBlock again = opened.get().jobs.get(JobsFile.NIGHT_CLEAN);

        assertEquals(written.enabled, again.enabled);
        assertEquals(written.command, again.command);
        assertEquals(written.everySeconds, again.everySeconds);
        assertEquals(written.at, again.at);
        assertEquals(written.as, again.as);
        assertEquals(written.runner, again.runner);
        assertEquals(written.onFailure, again.onFailure);
        assertEquals(written.retrySeconds, again.retrySeconds);
        assertEquals(written.retries, again.retries);
        assertEquals(written.catchUpSeconds, again.catchUpSeconds);
        assertEquals(written.when.minPlayers, again.when.minPlayers);
    }

    @Test
    void theWrittenFileLooksLikeTheDesignSaysItShould(@TempDir Path root) {
        Path config = root.resolve("config");
        TestConfigs.of(config)
            .open(JobsFile.spec());

        String text = TestConfigs.read(
            TestConfigs.utils(config)
                .resolve(JobsFile.FILE_NAME));

        assertTrue(text.contains("schemaVersion = 1"), text);
        assertTrue(text.contains("[jobs.night-clean]"), text);
        assertTrue(text.contains("[jobs.night-clean.when]"), text);
        assertTrue(text.contains("at = [\"04:00\"]"), text);
        assertTrue(text.contains("runner = \"server\""), text);
        assertTrue(text.contains("Команда пишется без косой черты впереди"), () -> "шапка файла на месте:\n" + text);
    }

    @Test
    void aHandWrittenJobIsReadWithAllItsKeys(@TempDir Path root) {
        Path config = root.resolve("config");
        TestConfigs.write(
            TestConfigs.utils(config)
                .resolve(JobsFile.FILE_NAME),
            "schemaVersion = 1",
            "[jobs.save]",
            "enabled = true",
            "command = \"save-all\"",
            "everySeconds = 3600",
            "at = [\"04:00\", \"16:00\"]",
            "as = \"commandblock\"",
            "runner = \"webhook\"",
            "onFailure = \"retry\"",
            "retrySeconds = 15",
            "retries = 2",
            "catchUpSeconds = 120",
            "[jobs.save.when]",
            "maxTickMillis = 60",
            "custom = [\"event\"]");

        JobBlock save = TestConfigs.of(config)
            .open(JobsFile.spec())
            .get().jobs.get("save");

        assertTrue(save.enabled);
        assertEquals("save-all", save.command);
        assertEquals(3600, save.everySeconds);
        assertEquals(Arrays.asList("04:00", "16:00"), save.at);
        assertEquals("commandblock", save.as);
        assertEquals("webhook", save.runner);
        assertEquals(JobsFile.ON_FAILURE_RETRY, save.onFailure);
        assertEquals(15, save.retrySeconds);
        assertEquals(2, save.retries);
        assertEquals(120, save.catchUpSeconds);
        assertEquals(60, save.when.maxTickMillis);
        assertEquals(Arrays.asList("event"), save.when.custom);
    }
}
