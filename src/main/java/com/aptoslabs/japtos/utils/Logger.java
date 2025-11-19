package com.aptoslabs.japtos.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger for Japtos SDK
 */
public class Logger {
    private static LogLevel currentLevel = LogLevel.DEBUG; // Default to DEBUG
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static void setLogLevel(LogLevel level) {
        currentLevel = level;
    }

    public static LogLevel getLogLevel() {
        return currentLevel;
    }

    public static void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }

    public static void debug(String message, Object... args) {
        log(LogLevel.DEBUG, String.format(message, args), null);
    }

    public static void info(String message) {
        log(LogLevel.INFO, message, null);
    }

    public static void info(String message, Object... args) {
        log(LogLevel.INFO, String.format(message, args), null);
    }

    public static void warn(String message) {
        log(LogLevel.WARN, message, null);
    }

    public static void warn(String message, Object... args) {
        log(LogLevel.WARN, String.format(message, args), null);
    }

    public static void warn(String message, Throwable throwable) {
        log(LogLevel.WARN, message, throwable);
    }

    public static void error(String message) {
        log(LogLevel.ERROR, message, null);
    }

    public static void error(String message, Object... args) {
        log(LogLevel.ERROR, String.format(message, args), null);
    }

    public static void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message, throwable);
    }

    private static void log(LogLevel level, String message, Throwable throwable) {
        if (!level.isEnabled(currentLevel)) {
            return;
        }

        String timestamp = LocalDateTime.now().format(formatter);
        String logMessage = String.format("[%s] [%s] %s", timestamp, level, message);
        
        if (level == LogLevel.ERROR) {
            System.err.println(logMessage);
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        } else {
            System.out.println(logMessage);
            if (throwable != null) {
                throwable.printStackTrace(System.out);
            }
        }
    }
}
