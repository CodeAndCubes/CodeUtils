package com.mrleonardos.codeutils.internal.clean;

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
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile;
import com.mrleonardos.codeutils.internal.clean.CleanupFile.RuleBlock;

class CleanupFileTest {

    @Test
    void theFileSitsBesideTheOtherSettings() {
        assertEquals(
            CleanupFile.FILE,
            CleanupFile.spec()
                .name());
        assertEquals(
            UtilsSettings.MODID,
            CleanupFile.spec()
                .modid());
        assertEquals(
            ConfigRoles.UTILS,
            CleanupFile.spec()
                .role());
        assertEquals(
            ConfigFormat.TOML,
            CleanupFile.spec()
                .format());
    }

    @Test
    void theFactoryRulesAreTheOnesOfTheDesign() {
        CleanupFile file = new CleanupFile();
        RuleBlock drops = file.rules.get(CleanupFile.DROPS);
        RuleBlock mobs = file.rules.get(CleanupFile.MOBS);

        assertTrue(drops.enabled);
        assertEquals(Arrays.asList("@item", "@xp"), drops.types);
        assertEquals(60, drops.minAgeSeconds);
        assertEquals(900, drops.intervalSeconds);
        assertEquals(0, drops.worldThreshold);
        assertEquals(0, drops.chunkThreshold);
        assertEquals(Arrays.asList(30, 10), drops.warnSeconds);
        assertEquals(BroadcastsFile.SINK_CHAT, drops.warnSink);
        assertEquals(0, drops.maxRemovals);
        assertTrue(drops.ignoreGuards.isEmpty(), "все защиты на месте, и это правильное значение почти всегда");

        assertFalse(mobs.enabled, "сколько мобов лишние, знает только владелец сервера");
        assertEquals(Arrays.asList("@hostile"), mobs.types);
        assertEquals(300, mobs.minAgeSeconds);
        assertEquals(0, mobs.intervalSeconds);
        assertEquals(600, mobs.worldThreshold);
        assertEquals(40, mobs.chunkThreshold);
    }

    @Test
    void theFactoryFileIsReadBackIntoTheSameValues(@TempDir Path root) {
        ConfigFile<CleanupFile> opened = TestConfigs.of(root.resolve("config"))
            .open(CleanupFile.spec());
        RuleBlock written = opened.get().rules.get(CleanupFile.DROPS);

        opened.reload();
        RuleBlock again = opened.get().rules.get(CleanupFile.DROPS);

        assertEquals(written.enabled, again.enabled);
        assertEquals(written.types, again.types);
        assertEquals(written.dimensions, again.dimensions);
        assertEquals(written.minAgeSeconds, again.minAgeSeconds);
        assertEquals(written.intervalSeconds, again.intervalSeconds);
        assertEquals(written.worldThreshold, again.worldThreshold);
        assertEquals(written.chunkThreshold, again.chunkThreshold);
        assertEquals(written.warnSeconds, again.warnSeconds);
        assertEquals(written.warnSink, again.warnSink);
        assertEquals(written.maxRemovals, again.maxRemovals);
        assertEquals(written.ignoreGuards, again.ignoreGuards);
        assertEquals(written.when.minPlayers, again.when.minPlayers);
    }

    @Test
    void theWrittenFileLooksLikeTheDesignSaysItShould(@TempDir Path root) {
        Path config = root.resolve("config");
        TestConfigs.of(config)
            .open(CleanupFile.spec());

        String text = TestConfigs.read(
            TestConfigs.utils(config)
                .resolve(CleanupFile.FILE_NAME));

        assertTrue(text.contains("schemaVersion = 1"), text);
        assertTrue(text.contains("[rules.drops]"), text);
        assertTrue(text.contains("[rules.drops.when]"), text);
        assertTrue(text.contains("[rules.mobs]"), text);
        assertTrue(text.contains("types = [\"@item\", \"@xp\"]"), text);
        assertTrue(text.contains("ignoreGuards = []"), text);
        assertTrue(text.contains("Имя идёт в /codeutils clean <правило>"), () -> "шапка файла на месте:\n" + text);
    }

    @Test
    void aHandWrittenRuleIsReadWithAllItsKeys(@TempDir Path root) {
        Path config = root.resolve("config");
        TestConfigs.write(
            TestConfigs.utils(config)
                .resolve(CleanupFile.FILE_NAME),
            "schemaVersion = 1",
            "[rules.nether]",
            "enabled = true",
            "types = [\"@hostile\", \"-Wither\"]",
            "dimensions = [-1]",
            "minAgeSeconds = 120",
            "intervalSeconds = 300",
            "worldThreshold = 400",
            "chunkThreshold = 30",
            "warnSeconds = [15]",
            "warnSink = \"log\"",
            "maxRemovals = 200",
            "ignoreGuards = [\"named\"]",
            "[rules.nether.when]",
            "maxTickMillis = 55");

        RuleBlock rule = TestConfigs.of(config)
            .open(CleanupFile.spec())
            .get().rules.get("nether");

        assertTrue(rule.enabled);
        assertEquals(Arrays.asList("@hostile", "-Wither"), rule.types);
        assertEquals(Arrays.asList(-1), rule.dimensions);
        assertEquals(120, rule.minAgeSeconds);
        assertEquals(300, rule.intervalSeconds);
        assertEquals(400, rule.worldThreshold);
        assertEquals(30, rule.chunkThreshold);
        assertEquals(Arrays.asList(15), rule.warnSeconds);
        assertEquals("log", rule.warnSink);
        assertEquals(200, rule.maxRemovals);
        assertEquals(Arrays.asList("named"), rule.ignoreGuards);
        assertEquals(55, rule.when.maxTickMillis);
    }
}
