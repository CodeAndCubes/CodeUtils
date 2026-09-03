package com.mrleonardos.codeutils.platform;

import net.minecraft.util.ChatComponentText;

import com.mrleonardos.codeutils.internal.UtilsTexts;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

public final class LoginGate {

    private final LoginDoor door;
    private final UtilsTexts texts;

    public LoginGate(LoginDoor door, UtilsTexts texts) {
        this.door = door;
        this.texts = texts;
    }

    @SubscribeEvent
    public void connecting(FMLNetworkEvent.ServerConnectionFromClientEvent event) {
        if (event.isLocal) {
            return;
        }
        if (door.closed()) {
            event.manager.closeChannel(new ChatComponentText(texts.format(door.reasonKey())));
        }
    }
}
