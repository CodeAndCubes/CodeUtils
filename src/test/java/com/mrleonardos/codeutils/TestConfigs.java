package com.mrleonardos.codeutils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mrleonardos.codecore.api.config.ConfigService;

/**
 * Настоящий ConfigService ядра поверх временной папки.
 *
 * <p>
 * Своей заглушки у CodeUtils нет: раскладка путей, toml, комментарии и порядок ключей это поведение
 * ядра, и проверять их подделкой значит проверять подделку. Реализация лежит в dev-джаре ядра, который и
 * так стоит на тестовом classpath, поэтому она достаётся по имени класса.
 */
public final class TestConfigs {

    public static final String LINEUP = "code";
    public static final String UTILS = "utils";
    public static final String MAIN_FILE = "config.toml";

    private static final Logger LOG = LogManager.getLogger(TestConfigs.class);

    private static final String SERVICE = "com.mrleonardos.codecore.internal.config.ConfigServiceImpl";
    private static final String PATHS = "com.mrleonardos.codecore.internal.config.ConfigPaths";

    private TestConfigs() {}

    public static ConfigService of(Path configDirectory) {
        try {
            Class<?> pathsType = Class.forName(PATHS);
            Constructor<?> pathsConstructor = pathsType.getConstructor(Path.class);
            Object paths = pathsConstructor.newInstance(configDirectory);
            Class<?> serviceType = Class.forName(SERVICE);
            Constructor<?> serviceConstructor = serviceType.getConstructor(pathsType, Logger.class);
            return (ConfigService) serviceConstructor.newInstance(paths, LOG);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(
                "ConfigService ядра не собрался, проверьте dev-джар CodeCore на тестовом classpath",
                failure);
        }
    }

    /** Конец постинициализации: главный файл пишется со всеми объявленными секциями. */
    public static void seal(ConfigService configs, String... roles) {
        try {
            Method method = configs.getClass()
                .getMethod("seal", List.class);
            method.invoke(configs, Arrays.asList(roles));
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Метод seal ядра не вызвался", failure);
        }
    }

    /** Папка линейки внутри папки конфигов игры. */
    public static Path lineup(Path configDirectory) {
        return configDirectory.resolve(LINEUP);
    }

    /** Папка служебных мелочей, в которой лежат все файлы мода. */
    public static Path utils(Path configDirectory) {
        return lineup(configDirectory).resolve(UTILS);
    }

    public static Path mainFile(Path configDirectory) {
        return lineup(configDirectory).resolve(MAIN_FILE);
    }

    public static void write(Path file, String... lines) {
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, Arrays.asList(lines), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("Файл " + file + " не записан", failure);
        }
    }

    public static String read(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("Файл " + file + " не прочитан", failure);
        }
    }
}
