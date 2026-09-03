package com.mrleonardos.codeutils.internal.job;

import java.util.ArrayList;
import java.util.List;

import com.mrleonardos.codeutils.api.run.CommandRunner;
import com.mrleonardos.codeutils.api.run.CommandTicket;
import com.mrleonardos.codeutils.api.run.RunOutcome;
import com.mrleonardos.codeutils.api.run.SenderChoice;

/** Исполнитель, который ничего не выполняет, а записывает заявки и отдаёт заданный ответ. */
public final class RecordingRunner implements CommandRunner {

    private final List<CommandTicket> tickets = new ArrayList<>();
    private final List<String> online = new ArrayList<>();

    private int worksFrom = 1;

    /** Пусть сработает начиная с этой попытки; ноль означает «никогда». */
    public RecordingRunner worksFrom(int attempt) {
        this.worksFrom = attempt;
        return this;
    }

    public RecordingRunner online(String nick) {
        online.add(nick);
        return this;
    }

    @Override
    public RunOutcome run(CommandTicket ticket) {
        tickets.add(ticket);
        if (ticket.sender()
            .kind() == SenderChoice.Kind.PLAYER
            && !online.contains(
                ticket.sender()
                    .nick())) {
            return RunOutcome.refused(RunOutcome.NO_PLAYER);
        }
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
