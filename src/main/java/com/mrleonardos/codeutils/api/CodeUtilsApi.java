package com.mrleonardos.codeutils.api;

import java.util.Objects;

/**
 * Точка входа для чужих модов.
 *
 * <pre>
 *
 * CodeUtilsApi.registry()
 *     .addSink("discord", (recipients, message) -&gt; bridge.post(message.flat()));
 * CodeUtilsApi.registry()
 *     .addCondition("event-running", snapshot -&gt; events.anyLive());
 * </pre>
 *
 * <p>
 * Заявки подаются в фазе init своего мода: в конце постинициализации CodeUtils замораживает реестр и
 * разрешает имена, написанные в файлах настроек. Обращение до того, как CodeUtils поднялся, даёт
 * понятную ошибку, а не падение с пустой ссылкой.
 */
public final class CodeUtilsApi {

    private static volatile UtilsRuntime runtime;

    private CodeUtilsApi() {}

    /** Реестры швов. */
    public static UtilsRegistry registry() {
        return runtime().registry();
    }

    /** Работает ли подсистема; имена перечислены в {@link Subsystems}. */
    public static boolean enabled(String subsystem) {
        return runtime().enabled(Objects.requireNonNull(subsystem, "subsystem"));
    }

    /** Поднялся ли мод. Спрашивают те, кому CodeUtils нужен по желанию, а не обязательно. */
    public static boolean ready() {
        return runtime != null;
    }

    /**
     * Подключает рантайм. Вызывается самим CodeUtils, чужим модам этот метод не нужен.
     *
     * @throws IllegalStateException если рантайм уже подключён
     */
    public static void install(UtilsRuntime installed) {
        if (runtime != null) {
            throw new IllegalStateException("CodeUtils runtime is already installed");
        }
        runtime = Objects.requireNonNull(installed, "installed");
    }

    private static UtilsRuntime runtime() {
        UtilsRuntime installed = runtime;
        if (installed == null) {
            throw new IllegalStateException(
                "CodeUtils is not ready yet, request it no earlier than your mod's init phase");
        }
        return installed;
    }
}
