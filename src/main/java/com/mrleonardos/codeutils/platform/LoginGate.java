package com.mrleonardos.codeutils.platform;

import java.util.UUID;
import java.util.function.LongSupplier;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.util.ChatComponentText;

import com.mrleonardos.codeutils.internal.UtilsTexts;
import com.mrleonardos.codeutils.internal.queue.Ghosts;
import com.mrleonardos.codeutils.internal.queue.QueueGate;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public final class LoginGate {

    private final LoginDoor door;
    private final QueueGate queue;
    private final UtilsTexts texts;
    private final LongSupplier clock;
    private final Ghosts ghosts = new Ghosts();

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
        EntityPlayerMP player = playerOf(event);
        if (door.closed()) {
            refuse(event, player, texts.format(door.reasonKey()));
            return;
        }
        if (queue == null) {
            return;
        }
        if (player == null) {
            return;
        }
        QueueGate.Answer answer = queue.decide(id(player), player.getCommandSenderName(), clock.getAsLong());
        if (answer.allowed()) {
            ghosts.admit(id(player));
            return;
        }
        refuse(event, player, queue.refusalOf(answer, texts));
    }

    @SubscribeEvent
    public void joined(PlayerEvent.PlayerLoggedInEvent event) {
        if (queue == null || event.player == null) {
            return;
        }
        UUID player = id(event.player);
        if (!ghosts.countsJoin(player)) {
            return;
        }
        queue.joined(player);
    }

    @SubscribeEvent
    public void left(PlayerEvent.PlayerLoggedOutEvent event) {
        if (queue == null) {
            return;
        }
        UUID player = event.player == null ? null : id(event.player);
        if (!ghosts.countsLeave(player)) {
            return;
        }
        queue.left(clock.getAsLong());
    }

    private static UUID id(EntityPlayer player) {
        return player.getGameProfile() == null || player.getGameProfile()
            .getId() == null ? null
                : player.getGameProfile()
                    .getId();
    }

    private void refuse(FMLNetworkEvent.ServerConnectionFromClientEvent event, EntityPlayerMP player, String text) {
        if (queue != null && player != null) {
            ghosts.refuse(id(player));
        }
        final ChatComponentText reason = new ChatComponentText(text);
        event.manager.scheduleOutboundPacket(new S40PacketDisconnect(reason), new GenericFutureListener<Future<?>>() {

            @Override
            public void operationComplete(Future<?> result) {
                event.manager.closeChannel(reason);
            }
        });
        event.manager.disableAutoRead();
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
