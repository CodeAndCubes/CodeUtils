package com.mrleonardos.codeutils.platform;

import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.mrleonardos.codeutils.api.clean.EntityKind;
import com.mrleonardos.codeutils.api.clean.EntityView;

public final class EntityLook implements EntityView {

    private static final int TICKS_IN_SECOND = 20;

    private final Entity entity;
    private final int dimension;

    public EntityLook(Entity entity, int dimension) {
        this.entity = entity;
        this.dimension = dimension;
    }

    Entity entity() {
        return entity;
    }

    @Override
    public String type() {
        String name = EntityList.getEntityString(entity);
        return name == null || name.isEmpty() ? entity.getClass()
            .getSimpleName() : name;
    }

    @Override
    public EntityKind kind() {
        if (entity instanceof EntityPlayer) {
            return EntityKind.PLAYER;
        }
        if (entity instanceof EntityItem) {
            return EntityKind.ITEM;
        }
        if (entity instanceof EntityXPOrb) {
            return EntityKind.XP;
        }
        if (entity instanceof EntityMinecart || entity instanceof EntityBoat) {
            return EntityKind.VEHICLE;
        }
        if (entity instanceof IProjectile) {
            return EntityKind.PROJECTILE;
        }
        if (entity instanceof IMob) {
            return EntityKind.HOSTILE;
        }
        if (entity instanceof IAnimals) {
            return EntityKind.PASSIVE;
        }
        return EntityKind.OTHER;
    }

    @Override
    public UUID id() {
        return entity.getUniqueID();
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public int chunkX() {
        return entity.chunkCoordX;
    }

    @Override
    public int chunkZ() {
        return entity.chunkCoordZ;
    }

    @Override
    public int ageSeconds() {
        if (entity instanceof EntityItem) {
            return ((EntityItem) entity).age / TICKS_IN_SECOND;
        }
        if (entity instanceof EntityXPOrb) {
            return ((EntityXPOrb) entity).xpOrbAge / TICKS_IN_SECOND;
        }
        return entity.ticksExisted / TICKS_IN_SECOND;
    }

    @Override
    public boolean named() {
        return entity instanceof EntityLiving && ((EntityLiving) entity).hasCustomNameTag();
    }

    @Override
    public boolean tamed() {
        if (entity instanceof EntityTameable && ((EntityTameable) entity).isTamed()) {
            return true;
        }
        if (!(entity instanceof IEntityOwnable)) {
            return false;
        }
        IEntityOwnable owned = (IEntityOwnable) entity;
        String owner = owned.func_152113_b();
        return owned.getOwner() != null || owner != null && !owner.isEmpty();
    }

    @Override
    public boolean leashed() {
        return entity instanceof EntityLiving && ((EntityLiving) entity).getLeashed();
    }

    @Override
    public boolean boss() {
        return entity instanceof IBossDisplayData;
    }

    @Override
    public boolean persistent() {
        return entity instanceof EntityLiving && ((EntityLiving) entity).isNoDespawnRequired();
    }

    @Override
    public boolean carrying() {
        if (entity instanceof EntityHorse && ((EntityHorse) entity).isChested()) {
            return true;
        }
        if (!(entity instanceof IInventory)) {
            return false;
        }
        IInventory held = (IInventory) entity;
        for (int slot = 0; slot < held.getSizeInventory(); slot++) {
            ItemStack stack = held.getStackInSlot(slot);
            if (stack != null && stack.stackSize > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean ridden() {
        return entity.riddenByEntity != null;
    }

    @Override
    public boolean hanging() {
        return entity instanceof EntityHanging;
    }
}
