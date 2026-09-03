package com.mrleonardos.codeutils.internal.job;

import java.util.ArrayList;
import java.util.List;

import com.mrleonardos.codeutils.api.run.CommandRunner;
import com.mrleonardos.codeutils.api.run.CommandTicket;
import com.mrleonardos.codeutils.api.run.RunOutcome;

/**
 * Исполнитель, который ничего не выполняет, а записывает заявки и отдаёт заданный ответ.
 *
 * <p>
 * Отсутствующего игрока он не проверяет: разбор {@code as} и поиск игрока делает движок, и заявка на
 * игрока, которого нет онлайн, до исполнителя не доходит вовсе.
 */
public final class RecordingRunner implements CommandRunner {

    private final List<CommandTicket> tickets = new ArrayList<>();

    private int worksFrom = 1;

    /** Пусть сработает начиная с этой попытки; ноль означает «никогда». */
    public RecordingRunner worksFrom(int attempt) {
        this.worksFrom = attempt;
        return this;
    }

    @Override
    public RunOutcome run(CommandTicket ticket) {
        tickets.add(ticket);
        return worksFrom > 0 && ticket.attempt() >= worksFrom ? RunOutcome.ran(1) : RunOutcome.ran(0);
    }

    public List<CommandTicket> tickets() {
        return tickets;
    }

    public int runs() {
        return tickets.size();
    }

    public List<String> commands() {
        List<String> lines = new ArrayList<>();
        for (CommandTicket ticket : tickets) {
            lines.add(ticket.command());
        }
        return lines;
    }
}
