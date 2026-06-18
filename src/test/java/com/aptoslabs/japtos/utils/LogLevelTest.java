package com.aptoslabs.japtos.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LogLevel} ordering and the {@link Logger} formatting paths.
 */
class LogLevelTest {

    @Test
    @DisplayName("Numeric ordering reflects verbosity")
    void numericOrdering() {
        assertEquals(0, LogLevel.DEBUG.getLevel());
        assertEquals(1, LogLevel.INFO.getLevel());
        assertEquals(2, LogLevel.WARN.getLevel());
        assertEquals(3, LogLevel.ERROR.getLevel());
    }

    @Test
    @DisplayName("isEnabled only allows levels at or above the configured threshold")
    void isEnabled() {
        // With current level INFO: DEBUG disabled, INFO/WARN/ERROR enabled.
        assertFalse(LogLevel.DEBUG.isEnabled(LogLevel.INFO));
        assertTrue(LogLevel.INFO.isEnabled(LogLevel.INFO));
        assertTrue(LogLevel.WARN.isEnabled(LogLevel.INFO));
        assertTrue(LogLevel.ERROR.isEnabled(LogLevel.INFO));

        // With current level ERROR: only ERROR enabled.
        assertFalse(LogLevel.WARN.isEnabled(LogLevel.ERROR));
        assertTrue(LogLevel.ERROR.isEnabled(LogLevel.ERROR));
    }

    @Test
    @DisplayName("Logger honours the configured level and exercises every emit path")
    void loggerEmitPaths() {
        LogLevel previous = Logger.getLogLevel();
        try {
            Logger.setLogLevel(LogLevel.DEBUG);
            assertEquals(LogLevel.DEBUG, Logger.getLogLevel());

            // Plain and formatted variants for each level must not throw.
            Logger.debug("debug plain");
            Logger.debug("debug %s %d", "fmt", 1);
            Logger.info("info plain");
            Logger.info("info %s", "fmt");
            Logger.warn("warn plain");
            Logger.warn("warn %s", "fmt");
            Logger.warn("warn with throwable", new RuntimeException("boom"));
            Logger.error("error plain");
            Logger.error("error %s", "fmt");
            Logger.error("error with throwable", new RuntimeException("boom"));

            // Raising the level suppresses lower-priority logging without error.
            Logger.setLogLevel(LogLevel.ERROR);
            Logger.debug("should be filtered");
            Logger.info("should be filtered");
            assertEquals(LogLevel.ERROR, Logger.getLogLevel());
        } finally {
            Logger.setLogLevel(previous);
        }
    }
}
