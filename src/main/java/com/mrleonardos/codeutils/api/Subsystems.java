package com.mrleonardos.codeutils.api;

/**
 * Имена подсистем: они же ключи секции {@code [on]} файла {@code utils.toml}.
 *
 * <p>
 * Выключенная подсистема не создаёт своего файла, не подписывается на события и не занимает ветку такта.
 * Мод, который заявляет свой шов, спрашивает {@link UtilsRuntime#enabled(String)} и не тратит работу на
 * то, что всё равно никто не спросит.
 */
public final class Subsystems {

    /** Рассылка сообщений по расписанию. */
    public static final String BROADCASTS = "broadcasts";

    /** Запуск команд по расписанию. */
    public static final String JOBS = "jobs";

    /** Перезапуск сервера. */
    public static final String RESTART = "restart";

    /** Очистка сущностей. */
    public static final String CLEANUP = "cleanup";

    /** Мягкие потолки слотов и очередь на вход. */
    public static final String QUEUE = "queue";

    private Subsystems() {}
}
