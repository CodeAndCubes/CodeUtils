package com.mrleonardos.codeutils.platform;

import java.util.function.Supplier;

import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.api.UtilsRuntime;
import com.mrleonardos.codeutils.internal.UtilsSettings;

public final class UtilsRuntimeImpl implements UtilsRuntime {

    private final UtilsRegistry registry;
    private final Supplier<UtilsSettings> settings;

    public UtilsRuntimeImpl(UtilsRegistry registry, Supplier<UtilsSettings> settings) {
        this.registry = registry;
        this.settings = settings;
    }

    @Override
    public UtilsRegistry registry() {
        return registry;
    }

    @Override
    public boolean enabled(String subsystem) {
        return settings.get()
            .enabled(subsystem);
    }
}
