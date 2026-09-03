package com.mrleonardos.codeutils.api.restart;

/**
 * Откуда взялся перезапуск.
 *
 * <p>
 * Шаг чужого мода вправе вести себя по-разному: ночная остановка по расписанию это обычное дело, а
 * остановка руками посреди дня обычно значит, что на сервере что-то чинят.
 */
public enum RestartReason {

    /** Расписание из {@code utils-restart.toml}. */
    SCHEDULE,

    /** Команда {@code /restart}. */
    COMMAND
}
