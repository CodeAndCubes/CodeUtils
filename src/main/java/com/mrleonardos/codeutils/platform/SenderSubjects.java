package com.mrleonardos.codeutils.platform;

import com.mrleonardos.codecore.api.command.CommandContext;
import com.mrleonardos.codecore.api.command.CommandSender;
import com.mrleonardos.codecore.api.command.SenderKind;
import com.mrleonardos.codecore.api.command.SenderPosition;
import com.mrleonardos.codeutils.internal.command.UtilsSubjects;

public final class SenderSubjects implements UtilsSubjects {

    static final String CONSOLE = "console";
    static final String RCON = "rcon";
    static final String COMMAND_BLOCK = "commandblock";

    @Override
    public String actorOf(CommandContext context) {
        return actorOf(context.caller());
    }

    static String actorOf(CommandSender sender) {
        if (sender.kind() == SenderKind.PLAYER) {
            return sender.name();
        }
        if (sender.kind() == SenderKind.COMMAND_BLOCK) {
            return sender.position()
                .map(SenderSubjects::block)
                .orElse(COMMAND_BLOCK);
        }
        return sender.kind() == SenderKind.RCON ? RCON : CONSOLE;
    }

    private static String block(SenderPosition at) {
        return COMMAND_BLOCK + "@" + at.x() + "," + at.y() + "," + at.z();
    }
}
