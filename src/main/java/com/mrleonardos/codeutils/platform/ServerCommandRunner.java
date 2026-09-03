package com.mrleonardos.codeutils.platform;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import com.mrleonardos.codecore.platform.PlayerRefs;
import com.mrleonardos.codeutils.api.run.CommandRunner;
import com.mrleonardos.codeutils.api.run.CommandTicket;
import com.mrleonardos.codeutils.api.run.RunOutcome;
import com.mrleonardos.codeutils.api.run.SenderChoice;

public final class ServerCommandRunner implements CommandRunner {

    static final String NO_SERVER = "no-server";

    private final SpawnCommandBlock block = new SpawnCommandBlock();

    @Override
    public RunOutcome run(CommandTicket ticket) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getCommandManager() == null) {
            return RunOutcome.refused(NO_SERVER);
        }
        ICommandSender sender = senderOf(ticket);
        if (sender == null) {
            return RunOutcome.refused(RunOutcome.NO_PLAYER);
        }
        return RunOutcome.ran(
            server.getCommandManager()
                .executeCommand(sender, ticket.command()));
    }

    private ICommandSender senderOf(CommandTicket ticket) {
        SenderChoice choice = ticket.sender();
        if (choice.kind() == SenderChoice.Kind.COMMAND_BLOCK) {
            return block;
        }
        if (choice.kind() == SenderChoice.Kind.PLAYER) {
            return ticket.player()
                .map(PlayerRefs::online)
                .orElse(null);
        }
        return MinecraftServer.getServer();
    }
}
