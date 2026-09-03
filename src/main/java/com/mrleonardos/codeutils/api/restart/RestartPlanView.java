package com.mrleonardos.codeutils.api.restart;

/**
 * Поставленный перезапуск глазами чужого шага: откуда он взялся, когда остановка и сколько осталось.
 *
 * <p>
 * Это снимок на момент вызова шага, а не живая ссылка на план: шаг работает в главном потоке и досчитать
 * секунды заново ему всё равно неоткуда.
 */
public final class RestartPlanView {

    private final RestartReason reason;
    private final long stopAt;
    private final int secondsLeft;

    private RestartPlanView(RestartReason reason, long stopAt, int secondsLeft) {
        this.reason = reason;
        this.stopAt = stopAt;
        this.secondsLeft = secondsLeft;
    }

    /**
     * Снимок плана.
     *
     * @param reason      откуда взялся перезапуск
     * @param stopAt      время остановки в миллисекундах эпохи
     * @param secondsLeft сколько секунд до неё осталось; ноль означает, что остановка уже идёт
     */
    public static RestartPlanView of(RestartReason reason, long stopAt, int secondsLeft) {
        return new RestartPlanView(reason == null ? RestartReason.COMMAND : reason, stopAt, Math.max(0, secondsLeft));
    }

    public RestartReason reason() {
        return reason;
    }

    /** Время остановки в миллисекундах эпохи. */
    public long stopAt() {
        return stopAt;
    }

    /** Сколько секунд осталось до остановки. */
    public int secondsLeft() {
        return secondsLeft;
    }

    @Override
    public String toString() {
        return "restart[" + reason + ", in " + secondsLeft + "s]";
    }
}
