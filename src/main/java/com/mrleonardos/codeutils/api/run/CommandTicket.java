package com.mrleonardos.codeutils.api.run;

import java.util.Objects;
import java.util.Optional;

import com.mrleonardos.codecore.api.actor.PlayerRef;

/**
 * Заявка на одно выполнение команды: что выполнить, от чьего имени и по чьему поручению.
 *
 * <p>
 * Имя задания идёт сюда ради лога и ответа: строка «команда не сработала» без имени задания заставляет
 * искать её по всему файлу.
 *
 * <p>
 * Игрока из {@code player:<ник>} движок находит сам и кладёт сюда готовой ссылкой. Исполнителю не
 * приходится искать его заново, а заявки на игрока, которого нет онлайн, сюда не попадают вовсе:
 * политика отказа срабатывает раньше.
 */
public final class CommandTicket {

    /** Имя ручного запуска: у команды, пущенной админом, задания нет. */
    public static final String BY_HAND = "";

    private final String job;
    private final String command;
    private final SenderChoice sender;
    private final PlayerRef player;
    private final int attempt;

    private CommandTicket(String job, String command, SenderChoice sender, PlayerRef player, int attempt) {
        this.job = job;
        this.command = command;
        this.sender = sender;
        this.player = player;
        this.attempt = attempt;
    }

    /** Первая попытка задания от консоли или командного блока. */
    public static CommandTicket of(String job, String command, SenderChoice sender) {
        return of(job, command, sender, null);
    }

    /**
     * Первая попытка задания с готовой ссылкой на игрока.
     *
     * @param player игрок из {@code player:<ник>}, найденный онлайн; {@code null} у остальных отправителей
     */
    public static CommandTicket of(String job, String command, SenderChoice sender, PlayerRef player) {
        return new CommandTicket(
            Objects.requireNonNull(job, "job"),
            Objects.requireNonNull(command, "command"),
            Objects.requireNonNull(sender, "sender"),
            player,
            1);
    }

    /** Та же заявка следующей попыткой. */
    public CommandTicket next() {
        return new CommandTicket(job, command, sender, player, attempt + 1);
    }

    /** Имя задания или {@link #BY_HAND} у ручного запуска. */
    public String job() {
        return job;
    }

    /** Строка команды без косой черты впереди. */
    public String command() {
        return command;
    }

    public SenderChoice sender() {
        return sender;
    }

    /** Игрок из {@code player:<ник>}, найденный онлайн; пусто у консоли и командного блока. */
    public Optional<PlayerRef> player() {
        return Optional.ofNullable(player);
    }

    /** Номер попытки, считая с единицы. */
    public int attempt() {
        return attempt;
    }

    @Override
    public String toString() {
        return "ticket[" + (job.isEmpty() ? "by hand" : job)
            + ", "
            + command
            + ", "
            + sender
            + ", attempt "
            + attempt
            + "]";
    }
}
