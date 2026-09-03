package com.mrleonardos.codeutils.internal.broadcast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mrleonardos.codecore.api.config.ConfigFile;
import com.mrleonardos.codecore.api.config.ConfigFormat;
import com.mrleonardos.codecore.api.config.ConfigRoles;
import com.mrleonardos.codecore.api.config.ConfigService;
import com.mrleonardos.codeutils.TestConfigs;
import com.mrleonardos.codeutils.internal.UtilsSettings;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile.MessageBlock;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile.SetBlock;

class BroadcastsFileTest {

    @Test
    void theFileSitsBesideTheOtherSettings() {
        assertEquals(
            BroadcastsFile.FILE,
            BroadcastsFile.spec()
                .name());
        assertEquals(
            UtilsSettings.MODID,
            BroadcastsFile.spec()
                .modid());
        assertEquals(
            ConfigRoles.UTILS,
            BroadcastsFile.spec()
                .role());
        assertEquals(
            ConfigFormat.TOML,
            BroadcastsFile.spec()
                .format());
    }

    @Test
    void theFactoryFileHoldsTwoSetsWithTheirOwnIntervals() {
        BroadcastsFile file = new BroadcastsFile();

        assertEquals(
            Arrays.asList(BroadcastsFile.TIPS, BroadcastsFile.LINKS),
            new java.util.ArrayList<>(file.sets.keySet()));

        SetBlock tips = file.sets.get(BroadcastsFile.TIPS);
        SetBlock links = file.sets.get(BroadcastsFile.LINKS);

        assertEquals(600, tips.intervalSeconds);
        assertEquals(120, tips.firstDelaySeconds);
        assertEquals(1800, links.intervalSeconds);
        assertEquals(600, links.firstDelaySeconds);
        assertEquals(3, tips.messages.size());
        assertEquals(2, links.messages.size());
        assertEquals(1, tips.when.minPlayers, "на пустом сервере подсказки никому не нужны");
    }

    @Test
    void aFactoryMessageWithoutHoverAndClickWritesNeitherKey() {
        MessageBlock plain = new BroadcastsFile().sets.get(BroadcastsFile.TIPS).messages.get(0);

        assertNull(plain.hover, "пустой ключ в файле выглядел бы настройкой, которой нет");
        assertNull(plain.click);
        assertNull(plain.parts);
    }

    @Test
    void theFactoryFileIsReadBackIntoTheSameValues(@TempDir Path root) {
        ConfigService configs = TestConfigs.of(root.resolve("config"));
        ConfigFile<BroadcastsFile> opened = configs.open(BroadcastsFile.spec());
        BroadcastsFile written = opened.get();

        opened.reload();
        BroadcastsFile again = opened.get();

        assertEquals(written.sets.keySet(), again.sets.keySet());
        for (String name : written.sets.keySet()) {
            SetBlock before = written.sets.get(name);
            SetBlock after = again.sets.get(name);
            assertEquals(before.enabled, after.enabled, name);
            assertEquals(before.intervalSeconds, after.intervalSeconds, name);
            assertEquals(before.firstDelaySeconds, after.firstDelaySeconds, name);
            assertEquals(before.order, after.order, name);
            assertEquals(before.whenEmpty, after.whenEmpty, name);
            assertEquals(before.sink, after.sink, name);
            assertEquals(before.prefix, after.prefix, name);
            assertEquals(before.permission, after.permission, name);
            assertEquals(before.dimensions, after.dimensions, name);
            assertEquals(before.messages.size(), after.messages.size(), name);
            assertEquals(before.when.minPlayers, after.when.minPlayers, name);
            for (int index = 0; index < before.messages.size(); index++) {
                assertEquals(before.messages.get(index).text, after.messages.get(index).text, name + " " + index);
                assertEquals(before.messages.get(index).hover, after.messages.get(index).hover, name + " " + index);
                assertEquals(before.messages.get(index).click, after.messages.get(index).click, name + " " + index);
            }
        }
    }

    @Test
    void theWrittenFileLooksLikeTheDesignSaysItShould(@TempDir Path root) {
        Path config = root.resolve("config");
        TestConfigs.of(config)
            .open(BroadcastsFile.spec());

        String text = TestConfigs.read(
            TestConfigs.utils(config)
                .resolve(BroadcastsFile.FILE_NAME));

        assertTrue(text.contains("schemaVersion = 1"), text);
        assertTrue(text.contains("[sets.tips]"), text);
        assertTrue(text.contains("[[sets.tips.messages]]"), () -> "сообщения набора это массив таблиц:\n" + text);
        assertTrue(text.contains("[sets.tips.when]"), text);
        assertTrue(text.contains("[sets.links]"), text);
        assertTrue(text.contains("url:https://example.com/rules"), text);
        assertFalse(text.contains("hover = \"\""), () -> "пустых ключей в файле быть не должно:\n" + text);
    }

    @Test
    void aHandWrittenSetIsReadWithAllItsKeys(@TempDir Path root) {
        Path config = root.resolve("config");
        TestConfigs.write(
            TestConfigs.utils(config)
                .resolve(BroadcastsFile.FILE_NAME),
            "schemaVersion = 1",
            "[sets.mine]",
            "enabled = false",
            "intervalSeconds = 30",
            "firstDelaySeconds = 5",
            "order = \"shuffle\"",
            "whenEmpty = \"skip\"",
            "sink = \"log\"",
            "prefix = \"[!] \"",
            "permission = \"codeutils.vip\"",
            "dimensions = [0, -1]",
            "[[sets.mine.messages]]",
            "text = \"первое\"",
            "hover = \"подсказка\"",
            "click = \"run:spawn\"",
            "[[sets.mine.messages.parts]]",
            "text = \" и второе\"",
            "click = \"suggest:/pay \"",
            "[sets.mine.when]",
            "minPlayers = 2",
            "between = \"23:00-02:00\"",
            "days = [\"fri\", \"sat\"]");

        BroadcastsFile file = TestConfigs.of(config)
            .open(BroadcastsFile.spec())
            .get();
        SetBlock mine = file.sets.get("mine");

        assertFalse(mine.enabled);
        assertEquals(30, mine.intervalSeconds);
        assertEquals(5, mine.firstDelaySeconds);
        assertEquals("shuffle", mine.order);
        assertEquals("skip", mine.whenEmpty);
        assertEquals("log", mine.sink);
        assertEquals("[!] ", mine.prefix);
        assertEquals("codeutils.vip", mine.permission);
        assertEquals(Arrays.asList(Integer.valueOf(0), Integer.valueOf(-1)), mine.dimensions);
        assertEquals(1, mine.messages.size());
        assertEquals("первое", mine.messages.get(0).text);
        assertEquals("подсказка", mine.messages.get(0).hover);
        assertEquals("run:spawn", mine.messages.get(0).click);
        List<BroadcastsFile.PartBlock> parts = mine.messages.get(0).parts;
        assertEquals(1, parts.size());
        assertEquals(" и второе", parts.get(0).text);
        assertEquals("suggest:/pay ", parts.get(0).click);
        assertEquals(2, mine.when.minPlayers);
        assertEquals("23:00-02:00", mine.when.between);
        assertEquals(Arrays.asList("fri", "sat"), mine.when.days);
    }
}
