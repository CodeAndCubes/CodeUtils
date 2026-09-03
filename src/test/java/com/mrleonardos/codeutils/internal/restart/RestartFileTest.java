package com.mrleonardos.codeutils.internal.restart;

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
import com.mrleonardos.codeutils.internal.command.UtilsMessages;

class RestartFileTest {

    @Test
    void theFileSitsBesideTheOtherSettings() {
        assertEquals(
            RestartFile.FILE,
            RestartFile.spec()
                .name());
        assertEquals(
            UtilsSettings.MODID,
            RestartFile.spec()
                .modid());
        assertEquals(
            ConfigRoles.UTILS,
            RestartFile.spec()
                .role());
        assertEquals(
            ConfigFormat.TOML,
            RestartFile.spec()
                .format());
    }

    @Test
    void theFactoryScheduleIsOffAndTheStepsHoldTheNumbersOfTheDesign() {
        RestartFile file = new RestartFile();

        assertFalse(file.schedule.enabled, "сервер не начинает останавливать себя сам, пока об этом не попросят");
        assertEquals(Arrays.asList("06:00"), file.schedule.at);
        assertTrue(file.schedule.days.isEmpty());
        assertEquals(Arrays.asList(600, 300, 60, 30, 10, 5, 4, 3, 2, 1), file.warnings.seconds);
        assertEquals(RestartFile.DEFAULT_PREFIX, file.warnings.prefix);
        assertEquals(RestartFile.DEFAULT_CLOSE_DOOR_SECONDS, file.steps.closeDoorSeconds);
        assertEquals(RestartFile.DEFAULT_KICK_SECONDS, file.steps.kickSeconds);
        assertEquals(RestartFile.DEFAULT_SETTLE_TICKS, file.steps.settleTicks);
        assertTrue(file.steps.savePlayers);
        assertTrue(file.steps.saveWorlds);
        assertEquals(UtilsMessages.RESTART_KICK, file.messages.kickKey);
        assertEquals(UtilsMessages.RESTART_DOOR, file.messages.doorKey);
    }

    @Test
    void theFactoryFileIsReadBackIntoTheSameValues(@TempDir Path root) {
        ConfigFile<RestartFile> opened = TestConfigs.of(root.resolve("config"))
            .open(RestartFile.spec());
        RestartFile written = opened.get();

        opened.reload();
        RestartFile again = opened.get();

        assertEquals(written.schedule.enabled, again.schedule.enabled);
        assertEquals(written.schedule.at, again.schedule.at);
        assertEquals(written.schedule.days, again.schedule.days);
        assertEquals(written.schedule.when.minPlayers, again.schedule.when.minPlayers);
        assertEquals(written.warnings.seconds, again.warnings.seconds);
        assertEquals(written.warnings.sink, again.warnings.sink);
        assertEquals(written.warnings.prefix, again.warnings.prefix);
        assertEquals(written.steps.closeDoorSeconds, again.steps.closeDoorSeconds);
        assertEquals(written.steps.kickSeconds, again.steps.kickSeconds);
        assertEquals(written.steps.settleTicks, again.steps.settleTicks);
        assertEquals(written.steps.savePlayers, again.steps.savePlayers);
        assertEquals(written.steps.saveWorlds, again.steps.saveWorlds);
        assertEquals(written.messages.kickKey, again.messages.kickKey);
        assertEquals(written.messages.doorKey, again.messages.doorKey);
    }

    @Test
    void theWrittenFileLooksLikeTheDesignSaysItShould(@TempDir Path root) {
        Path config = root.resolve("config");
        TestConfigs.of(config)
            .open(RestartFile.spec());

        String text = TestConfigs.read(
            TestConfigs.utils(config)
                .resolve(RestartFile.FILE_NAME));

        assertTrue(text.contains("schemaVersion = 1"), text);
        assertTrue(text.contains("[schedule]"), text);
        assertTrue(text.contains("[schedule.when]"), text);
        assertTrue(text.contains("[warnings]"), text);
        assertTrue(text.contains("[steps]"), text);
        assertTrue(text.contains("[messages]"), text);
        assertTrue(text.contains("closeDoorSeconds = 30"), text);
        assertTrue(text.contains("kickSeconds = 5"), text);
        assertTrue(text.contains("settleTicks = 20"), text);
        assertTrue(text.contains("поднимает его обратно обёртка процесса"), () -> "шапка файла на месте:\n" + text);
    }

    @Test
    void aHandWrittenFileIsReadWithAllItsKeys(@TempDir Path root) {
        Path config = root.resolve("config");
        TestConfigs.write(
            TestConfigs.utils(config)
                .resolve(RestartFile.FILE_NAME),
            "schemaVersion = 1",
            "[schedule]",
            "enabled = true",
            "at = [\"04:00\", \"16:00\"]",
            "days = [\"mon\", \"fri\"]",
            "[schedule.when]",
            "maxPlayers = 10",
            "[warnings]",
            "seconds = [120, 60]",
            "sink = \"log\"",
            "prefix = \"\"",
            "[steps]",
            "closeDoorSeconds = 45",
            "kickSeconds = 10",
            "settleTicks = 40",
            "savePlayers = false",
            "saveWorlds = false",
            "[messages]",
            "kickKey = \"my.kick\"",
            "doorKey = \"my.door\"");

        RestartFile file = TestConfigs.of(config)
            .open(RestartFile.spec())
            .get();

        assertTrue(file.schedule.enabled);
        assertEquals(Arrays.asList("04:00", "16:00"), file.schedule.at);
        assertEquals(Arrays.asList("mon", "fri"), file.schedule.days);
        assertEquals(10, file.schedule.when.maxPlayers);
        assertEquals(Arrays.asList(120, 60), file.warnings.seconds);
        assertEquals("log", file.warnings.sink);
        assertEquals("", file.warnings.prefix);
        assertEquals(45, file.steps.closeDoorSeconds);
        assertEquals(10, file.steps.kickSeconds);
        assertEquals(40, file.steps.settleTicks);
        assertFalse(file.steps.savePlayers);
        assertFalse(file.steps.saveWorlds);
        assertEquals("my.kick", file.messages.kickKey);
        assertEquals("my.door", file.messages.doorKey);
    }

    @Test
    void anEmptyKeyOfAMessageFallsBackToTheKeyOfTheMod(@TempDir Path root) {
        Path config = root.resolve("config");
        TestConfigs.write(
            TestConfigs.utils(config)
                .resolve(RestartFile.FILE_NAME),
            "schemaVersion = 1",
            "[messages]",
            "kickKey = \"\"",
            "doorKey = \"\"");

        RestartFile file = TestConfigs.of(config)
            .open(RestartFile.spec())
            .get();

        assertEquals(UtilsMessages.RESTART_KICK, file.messages.kickKey);
        assertEquals(UtilsMessages.RESTART_DOOR, file.messages.doorKey);
    }
}
