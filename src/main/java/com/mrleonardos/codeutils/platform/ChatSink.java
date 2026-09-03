package com.mrleonardos.codeutils.platform;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IChatComponent;

import com.mrleonardos.codecore.api.actor.PlayerRef;
import com.mrleonardos.codecore.platform.PlayerRefs;
import com.mrleonardos.codeutils.api.message.BroadcastSink;
import com.mrleonardos.codeutils.api.message.Message;

public final class ChatSink implements BroadcastSink {

    @Override
    public void send(List<PlayerRef> recipients, Message message) {
        IChatComponent component = Components.of(message);
        for (EntityPlayerMP player : PlayerRefs.online(recipients)) {
            player.addChatMessage(component);
        }
    }
}
