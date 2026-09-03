package com.mrleonardos.codeutils.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.mrleonardos.codeutils.api.Subsystems;

/**
 * Настройка без читателя это дефект. Здесь читатель каждого ключа {@code utils.toml} дёргается за руку и
 * проверяется по ответу, а не по обещанию.
 */
class SettingsReadersTest {

    @Test
    void theZoneKeyIsReadAndAnUnknownZoneFallsBackToTheMachine() {
        UtilsSettings settings = new UtilsSettings();
        settings.timezone = "Europe/Moscow";

        assertEquals(ZoneId.of("Europe/Moscow"), settings.zone());

        settings.timezone = "Middle/Earth";

        assertEquals(ZoneId.systemDefault(), settings.zone());

        settings.timezone = UtilsSettings.SYSTEM_ZONE;

        assertEquals(ZoneId.systemDefault(), settings.zone());
    }

    @Test
    void everySwitchOfTheOnSectionAnswersForItsOwnSubsystem() {
        UtilsSettings settings = new UtilsSettings();
        settings.on.broadcasts = false;
        settings.on.jobs = false;
        settings.on.restart = false;
        settings.on.cleanup = false;
        settings.on.queue = true;

        assertFalse(settings.enabled(Subsystems.BROADCASTS));
        assertFalse(settings.enabled(Subsystems.JOBS));
        assertFalse(settings.enabled(Subsystems.RESTART));
        assertFalse(settings.enabled(Subsystems.CLEANUP));
        assertTrue(settings.enabled(Subsystems.QUEUE));
    }

    @Test
    void aNameThatIsNotASubsystemIsNotEnabled() {
        assertFalse(new UtilsSettings().enabled("broadcast"));
        assertFalse(new UtilsSettings().enabled(null));
    }

    @Test
    void theStartupSummaryKeyIsRead() {
        UtilsSettings settings = new UtilsSettings();

        assertTrue(settings.startupSummary());

        settings.log.startupSummary = false;

        assertFalse(settings.startupSummary());
    }

    @Test
    void theSlowPassKeyIsReadAndNeverGoesBelowZero() {
        UtilsSettings settings = new UtilsSettings();
        settings.log.slowPassMillis = -5;

        assertEquals(0, settings.slowPassMillis());

        settings.log.slowPassMillis = 40;

        assertEquals(40, settings.slowPassMillis());
    }

}
