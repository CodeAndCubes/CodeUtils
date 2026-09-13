package com.mrleonardos.codeutils.api.run;

/**
 * Чем кончилось выполнение команды.
 *
 * <p>
 * Игра отвечает числом удачных исполнений, а не признаком успеха, и законная команда тоже умеет вернуть
 * ноль. Поэтому «сработало» здесь означает «число больше нуля», а не «исключения не было». Ложная
 * тревога возможна, и заводская политика отказа выбрана так, чтобы она стоила одну строку в логе.
 */
public final class RunOutcome {

    /** Отказ, когда игрока из {@code player:<ник>} нет на сервере. */
    public static final String NO_PLAYER = "no-player";

    /** Отказ, когда исполнитель не нашёл, чем выполнить команду. */
    public static final String NO_RUNNER = "no-runner";

    /** Команда выполнилась, но ни одного удачного исполнения не насчитала. */
    public static final String NOTHING = "nothing";

    private final int count;
    private final String reason;

    private RunOutcome(int count, String reason) {
        this.count = count;
        this.reason = reason;
    }

    /**
     * Команда выполнена столько-то раз.
     *
     * @param count число удачных исполнений; ноль и меньше считается отказом
     */
    public static RunOutcome ran(int count) {
        return count > 0 ? new RunOutcome(count, "") : new RunOutcome(0, NOTHING);
    }

    /** Выполнить не удалось по названной причине. */
    public static RunOutcome refused(String reason) {
        return new RunOutcome(0, reason == null || reason.isEmpty() ? NOTHING : reason);
    }

    /** Сработала ли команда. */
    public boolean successful() {
        return count > 0;
    }

    /** Число удачных исполнений. */
    public int count() {
        return count;
    }

    /** Почему не сработало; пустая строка у успеха. */
    public String reason() {
        return reason;
    }

    @Override
    public String toString() {
        return successful() ? "ran x" + count : "refused: " + reason;
    }
}
