package com.mrleonardos.codeutils.internal.clean;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.mrleonardos.codeutils.api.clean.EntityKind;
import com.mrleonardos.codeutils.api.clean.EntityView;

/** Сущность, которой нет: тип, чанк, возраст и восемь признаков, больше очистке ничего не видно. */
public final class FakeEntity implements EntityView {

    private final String type;
    private final EntityKind kind;
    private final UUID id = UUID.randomUUID();
    private final Set<String> guards = new LinkedHashSet<>();

    private int dimension;
    private int chunkX;
    private int chunkZ;
    private int age = 600;

    private FakeEntity(String type, EntityKind kind) {
        this.type = type;
        this.kind = kind;
    }

    public static FakeEntity of(String type, EntityKind kind) {
        return new FakeEntity(type, kind);
    }

    public static FakeEntity item() {
        return of("Item", EntityKind.ITEM);
    }

    public static FakeEntity zombie() {
        return of("Zombie", EntityKind.HOSTILE);
    }

    public static FakeEntity wolf() {
        return of("Wolf", EntityKind.PASSIVE);
    }

    public FakeEntity in(int value) {
        this.dimension = value;
        return this;
    }

    public FakeEntity chunk(int x, int z) {
        this.chunkX = x;
        this.chunkZ = z;
        return this;
    }

    public FakeEntity age(int seconds) {
        this.age = seconds;
        return this;
    }

    /** Поднять признак защиты по её имени из {@link Guards}. */
    public FakeEntity guarded(String guard) {
        guards.add(guard);
        return this;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public EntityKind kind() {
        return kind;
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public int chunkX() {
        return chunkX;
    }

    @Override
    public int chunkZ() {
        return chunkZ;
    }

    @Override
    public int ageSeconds() {
        return age;
    }

    @Override
    public boolean named() {
        return guards.contains(Guards.NAMED);
    }

    @Override
    public boolean tamed() {
        return guards.contains(Guards.TAMED);
    }

    @Override
    public boolean leashed() {
        return guards.contains(Guards.LEASHED);
    }

    @Override
    public boolean boss() {
        return guards.contains(Guards.BOSS);
    }

    @Override
    public boolean persistent() {
        return guards.contains(Guards.PERSISTENT);
    }

    @Override
    public boolean carrying() {
        return guards.contains(Guards.CARRYING);
    }

    @Override
    public boolean ridden() {
        return guards.contains(Guards.RIDDEN);
    }

    @Override
    public boolean hanging() {
        return guards.contains(Guards.HANGING);
    }

    @Override
    public String toString() {
        return type + "@" + chunkX + "," + chunkZ;
    }
}
