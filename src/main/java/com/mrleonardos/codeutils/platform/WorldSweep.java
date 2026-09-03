package com.mrleonardos.codeutils.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import com.mrleonardos.codeutils.api.clean.EntityView;
import com.mrleonardos.codeutils.internal.clean.EntitySweep;

public final class WorldSweep implements EntitySweep {

    @Override
    public List<EntityView> loaded(Set<Integer> dimensions) {
        List<EntityView> found = new ArrayList<>();
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.worldServers == null) {
            return found;
        }
        for (WorldServer world : server.worldServers) {
            if (world == null || world.provider == null) {
                continue;
            }
            int dimension = world.provider.dimensionId;
            if (!dimensions.isEmpty() && !dimensions.contains(Integer.valueOf(dimension))) {
                continue;
            }
            for (Entity entity : world.loadedEntityList) {
                if (entity != null && !entity.isDead) {
                    found.add(new EntityLook(entity, dimension));
                }
            }
        }
        return found;
    }

    @Override
    public int remove(List<EntityView> victims) {
        int removed = 0;
        for (EntityView victim : victims) {
            if (!(victim instanceof EntityLook)) {
                continue;
            }
            Entity entity = ((EntityLook) victim).entity();
            if (!entity.isDead) {
                entity.setDead();
                removed++;
            }
        }
        return removed;
    }
}
