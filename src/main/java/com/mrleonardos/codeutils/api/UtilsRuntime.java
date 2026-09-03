package com.mrleonardos.codeutils.api;

/**
 * То, что CodeUtils отдаёт чужим модам: реестр швов и список работающих подсистем.
 */
public interface UtilsRuntime {

    /** Реестры приёмников, условий и исполнителей. */
    UtilsRegistry registry();

    /**
     * Работает ли подсистема.
     *
     * @param subsystem одно из имён {@link Subsystems}
     */
    boolean enabled(String subsystem);
}
