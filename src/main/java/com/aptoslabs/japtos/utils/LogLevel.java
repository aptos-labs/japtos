package com.aptoslabs.japtos.utils;

/**
 * Logging levels for the Japtos SDK
 */
public enum LogLevel {
    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3);

    private final int level;

    LogLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public boolean isEnabled(LogLevel currentLevel) {
        return this.level >= currentLevel.level;
    }
}
