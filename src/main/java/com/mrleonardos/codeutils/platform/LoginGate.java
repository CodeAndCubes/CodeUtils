package com.mrleonardos.codeutils.platform;

import java.util.function.LongSupplier;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.ChatComponentText;

import com.mrleonardos.codeutils.internal.UtilsTexts;
import com.mrleonardos.codeutils.internal.command.UtilsMessages;
import com.mrleonardos.codeutils.internal.queue.QueueGate;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

public final class LoginGate {

    private final LoginDoor door;
    private final QueueGate queue;
    private final UtilsTexts texts;
    private final LongSupplier clock;

    public LoginGate(LoginDoor door, QueueGate queue, UtilsTexts texts, LongSupplier clock) {
        this.door = door;
        this.queue = queue;
        this.texts = texts;
        this.clock = clock;
    }

    @SubscribeEvent
    public void connecting(FMLNetworkEvent.ServerConnectionFromClientEvent event) {
        if (event.isLocal) {
            return;
        }
        if (door.closed()) {
            event.manager.closeChannel(new ChatComponentText(texts.format(door.reasonKey())));
            return;
        }
        if (queue == null) {
            return;
        }
        EntityPlayerMP player = playerOf(event);
        if (player == null) {
            return;
        }
        QueueGate.Answer answer = queue.decide(
            player.getGameProfile()
                .getId(),
            player.getCommandSenderName(),
            clock.getAsLong());
        if (answer.allowed()) {
            return;
        }
        event.manager.closeChannel(new ChatComponentText(refusal(answer)));
    }

    @SubscribeEvent
    public void joined(PlayerEvent.PlayerLoggedInEvent event) {
        if (queue == null || event.player == null) {
            return;
        }
        queue.joined(
            event.player.getGameProfile()
                .getId());
    }

    @SubscribeEvent
    public void left(PlayerEvent.PlayerLoggedOutEvent event) {
        if (queue != null) {
            queue.left(clock.getAsLong());
        }
    }

    private String refusal(QueueGate.Answer answer) {
        return answer.place() > 0 ? texts.format(
            UtilsMessages.QUEUE_REFUSED,
            Integer.valueOf(answer.place()),
            Integer.valueOf(queue.retryHintSeconds())) : texts.format(UtilsMessages.QUEUE_FULL);
    }

    private static EntityPlayerMP playerOf(FMLNetworkEvent.ServerConnectionFromClientEvent event) {
        if (!(event.handler instanceof NetHandlerPlayServer)) {
            return null;
        }
        EntityPlayerMP player = ((NetHandlerPlayServer) event.handler).playerEntity;
        return player == null || player.getGameProfile() == null
            || player.getGameProfile()
                .getId() == null ? null : player;
    }
}
