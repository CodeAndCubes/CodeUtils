package com.mrleonardos.codeutils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mrleonardos.codesides.gate.PackageGate;
import com.mrleonardos.codeutils.api.CodeUtilsApi;

class ImportGateTest {

    private static final String[] GATED = { "com/mrleonardos/codeutils/api", "com/mrleonardos/codeutils/internal" };

    private static final String[] FORBIDDEN = { "net/minecraft", "net/minecraftforge", "cpw/mods", "io/netty",
        "org/lwjgl", "com/mojang", "com/mrleonardos/codecore/platform" };

    private static final String[] API_ONLY = { "com/mrleonardos/codeutils/internal" };

    @Test
    void apiAndInternalHoldNoPlatformTypes() throws IOException {
        List<String> violations = gate(FORBIDDEN).violations(GATED);

        assertTrue(
            violations.isEmpty(),
            () -> "типы платформы и хелперы ядра живут только в platform, чужие ссылки:\n"
                + String.join("\n", violations));
    }

    @Test
    void theApiDoesNotReachIntoInternal() throws IOException {
        List<String> violations = gate(API_ONLY).violations("com/mrleonardos/codeutils/api");

        assertTrue(
            violations.isEmpty(),
            () -> "против api компилируются чужие моды, internal им не виден:\n" + String.join("\n", violations));
    }

    @Test
    void eventListenersArePublic() throws IOException {
        List<String> hidden = gate(FORBIDDEN).hiddenListeners();

        assertTrue(hidden.isEmpty(), () -> "классы с @SubscribeEvent обязаны быть public: " + hidden);
    }

    @Test
    void gateNoticesAForbiddenReference() throws IOException {
        List<String> found = gate(FORBIDDEN).scan(PackageGate.foreignSample());

        assertFalse(found.isEmpty(), "гейт обязан ловить ссылку на тип Minecraft");
    }

    private static PackageGate gate(String[] forbidden) throws IOException {
        return PackageGate.of(CodeUtilsApi.class, forbidden);
    }
}
