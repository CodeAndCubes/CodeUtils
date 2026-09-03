package com.mrleonardos.codeutils.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mrleonardos.codecore.api.config.ConfigService;
import com.mrleonardos.codeutils.TestConfigs;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile;
import com.mrleonardos.codeutils.internal.clean.CleanupFile;
import com.mrleonardos.codeutils.internal.command.CommandRoots;
import com.mrleonardos.codeutils.internal.job.JobsFile;
import com.mrleonardos.codeutils.internal.queue.QueueFile;
import com.mrleonardos.codeutils.internal.restart.RestartFile;

class UtilsFilesTest {

    private static final String SETTINGS_FILE = "utils.toml";

    @Test
    void theFirstRunLeavesEveryFileOfTheLiveSubsystemsInPlace(@TempDir Path root) {
        Path config = root.resolve("config");

        UtilsFiles files = UtilsFiles.open(TestConfigs.of(config));

        Path folder = TestConfigs.utils(config);
        assertTrue(Files.exists(folder.resolve(SETTINGS_FILE)), "utils.toml");
        assertTrue(Files.exists(folder.resolve(CommandRoots.FILE_NAME)), CommandRoots.FILE_NAME);
        assertTrue(Files.exists(folder.resolve(BroadcastsFile.FILE_NAME)), BroadcastsFile.FILE_NAME);
        assertTrue(Files.exists(folder.resolve(JobsFile.FILE_NAME)), JobsFile.FILE_NAME);
        assertTrue(Files.exists(folder.resolve(RestartFile.FILE_NAME)), RestartFile.FILE_NAME);
        assertTrue(Files.exists(folder.resolve(CleanupFile.FILE_NAME)), CleanupFile.FILE_NAME);
        assertFalse(Files.exists(folder.resolve(QueueFile.FILE_NAME)), "очередь по умолчанию снята, и файла у неё нет");
        assertNotNull(files.broadcasts());
        assertNotNull(files.jobs());
        assertNotNull(files.restart());
        assertNotNull(files.cleanup());
        assertNull(files.queue());
    }

    @Test
    void aSubsystemThatIsOffCreatesNoFileOfItsOwn(@TempDir Path root) {
        Path config = root.resolve("config");
        TestConfigs.write(
            TestConfigs.utils(config)
                .resolve(SETTINGS_FILE),
            "schemaVersion = 1",
            "[on]",
            "broadcasts = false",
            "jobs = false",
            "restart = false",
            "cleanup = false");

        UtilsFiles files = UtilsFiles.open(TestConfigs.of(config));

        Path folder = TestConfigs.utils(config);
        assertFalse(Files.exists(folder.resolve(BroadcastsFile.FILE_NAME)), "выключенная рассылка файла не заводит");
        assertFalse(Files.exists(folder.resolve(JobsFile.FILE_NAME)), "выключенные задания файла не заводят");
        assertFalse(Files.exists(folder.resolve(RestartFile.FILE_NAME)), "выключенный перезапуск файла не заводит");
        assertFalse(Files.exists(folder.resolve(CleanupFile.FILE_NAME)), "выключенная очистка файла не заводит");
        assertTrue(Files.exists(folder.resolve(CommandRoots.FILE_NAME)), "корни команд создаются всегда");
        assertNull(files.broadcasts());
        assertNull(files.jobs());
        assertNull(files.restart());
        assertNull(files.cleanup());
    }

    @Test
    void theQueueGetsItsFileOnceItIsSwitchedOn(@TempDir Path root) {
        Path config = root.resolve("config");
        TestConfigs.write(
            TestConfigs.utils(config)
                .resolve(SETTINGS_FILE),
            "schemaVersion = 1",
            "[on]",
            "queue = true");

        UtilsFiles files = UtilsFiles.open(TestConfigs.of(config));

        assertTrue(
            Files.exists(
                TestConfigs.utils(config)
                    .resolve(QueueFile.FILE_NAME)));
        assertNotNull(files.queue());
    }

    @Test
    void aFileTheModWroteIsNotTouchedOnTheNextStart(@TempDir Path root) {
        Path config = root.resolve("config");
        Path settings = TestConfigs.utils(config)
            .resolve(SETTINGS_FILE);
        UtilsFiles.open(TestConfigs.of(config));
        String first = TestConfigs.read(settings);

        UtilsFiles.open(TestConfigs.of(config));

        assertEquals(first, TestConfigs.read(settings), "второй запуск не меняет в файле ни строки");
    }

    @Test
    void aValueChangedByHandSurvivesTheReading(@TempDir Path root) {
        Path config = root.resolve("config");
        Path settings = TestConfigs.utils(config)
            .resolve(SETTINGS_FILE);
        UtilsFiles.open(TestConfigs.of(config));
        TestConfigs.write(
            settings,
            TestConfigs.read(settings)
                .replace("timezone = \"system\"", "timezone = \"Europe/Moscow\"")
                .split("\n"));

        UtilsFiles files = UtilsFiles.open(TestConfigs.of(config));

        assertEquals(
            ZoneId.of("Europe/Moscow"),
            files.settings()
                .get()
                .zone());
        assertTrue(
            TestConfigs.read(settings)
                .contains("Europe/Moscow"),
            "значение человека остаётся в файле");
    }

    @Test
    void aLineOverAFieldOfTheModYieldsToItsDescriptionAndOverHisOwnKeyStays(@TempDir Path root) {
        Path config = root.resolve("config");
        Path settings = TestConfigs.utils(config)
            .resolve(SETTINGS_FILE);
        TestConfigs.write(
            settings,
            "schemaVersion = 1",
            "# моя строка",
            "timezone = \"Europe/Moscow\"",
            "# моя над чужим",
            "myOwn = 1");

        UtilsFiles files = UtilsFiles.open(TestConfigs.of(config));
        String text = TestConfigs.read(settings);

        assertEquals(
            ZoneId.of("Europe/Moscow"),
            files.settings()
                .get()
                .zone(),
            "значение человека на месте");
        assertTrue(text.contains("Часовой пояс всего"), () -> "описание поля мода на месте:\n" + text);
        assertFalse(text.contains("# моя строка"), () -> "строка над полем мода уступила описанию:\n" + text);
        assertTrue(text.contains("myOwn = 1"), () -> "чужой ключ остаётся:\n" + text);
        assertTrue(text.contains("# моя над чужим"), () -> "строка над своим ключом остаётся:\n" + text);
    }

    @Test
    void anIncompleteSettingsFileGetsTheMissingKeysAndKeepsTheValuesOfTheMan(@TempDir Path root) {
        Path config = root.resolve("config");
        Path settings = TestConfigs.utils(config)
            .resolve(SETTINGS_FILE);
        TestConfigs.write(
            settings,
            "schemaVersion = 1",
            "timezone = \"Europe/Moscow\"",
            "[on]",
            "broadcasts = true",
            "jobs = false");

        UtilsFiles files = UtilsFiles.open(TestConfigs.of(config));
        UtilsSettings written = files.settings()
            .get();

        assertEquals(ZoneId.of("Europe/Moscow"), written.zone(), "пояс человека на месте");
        assertFalse(written.on.jobs, "выключенные им задания на месте");
        assertTrue(written.on.cleanup, "недостающий ключ пришёл заводским");

        String text = TestConfigs.read(settings);

        assertTrue(text.contains("cleanup = true"), text);
        assertTrue(text.contains("[log]"), () -> "секция, которой не было, дописана целиком:\n" + text);
    }

    @Test
    void aFactoryFileIsReadBackIntoTheSameValues(@TempDir Path root) {
        Path config = root.resolve("config");
        ConfigService configs = TestConfigs.of(config);

        UtilsFiles files = UtilsFiles.open(configs);
        UtilsSettings written = files.settings()
            .get();

        assertEquals(UtilsSettings.SYSTEM_ZONE, written.timezone);
        assertTrue(written.on.broadcasts);
        assertTrue(written.on.jobs);
        assertTrue(written.on.restart);
        assertTrue(written.on.cleanup);
        assertFalse(written.on.queue, "очередь просит правки server.properties, поэтому по умолчанию снята");
        assertTrue(written.startupSummary());
        assertEquals(UtilsSettings.DEFAULT_SLOW_PASS_MILLIS, written.slowPassMillis());

        files.settings()
            .reload();
        UtilsSettings again = files.settings()
            .get();

        assertEquals(written.timezone, again.timezone);
        assertEquals(written.on.broadcasts, again.on.broadcasts);
        assertEquals(written.on.queue, again.on.queue);
        assertEquals(written.slowPassMillis(), again.slowPassMillis());
    }

    @Test
    void theFileNamesFollowTheRuleOfTheLineup(@TempDir Path root) {
        Path config = root.resolve("config");

        UtilsFiles.open(TestConfigs.of(config));

        Path folder = TestConfigs.utils(config);
        assertEquals(
            "utils",
            folder.getFileName()
                .toString(),
            "владелец в именах это utils, приставка code отрезана");
        assertTrue(Files.exists(folder.resolve(SETTINGS_FILE)));
        assertFalse(Files.exists(TestConfigs.mainFile(config)), "своих секций в главном файле у мода нет");
    }
}
