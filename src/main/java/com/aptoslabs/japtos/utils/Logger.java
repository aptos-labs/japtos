package com.aptoslabs.japtos.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralized logging utility for the Japtos SDK.
 *
 * <p>This logger provides a simple, lightweight logging system that outputs
 * formatted log messages to standard output (stdout) or standard error (stderr).
 * It supports multiple log levels and can be configured at runtime.</p>
 *
 * <p>Log levels (from most verbose to least):</p>
 * <ul>
 *   <li><strong>DEBUG</strong> - Detailed information for debugging (default)</li>
 *   <li><strong>INFO</strong> - General informational messages</li>
 *   <li><strong>WARN</strong> - Warning messages for potential issues</li>
 *   <li><strong>ERROR</strong> - Error messages for failures</li>
 * </ul>
 *
 * <p>Log messages are formatted with timestamps and log levels:</p>
 * <pre>{@code
 * [2025-01-15 10:30:45.123] [INFO] Japtos SDK v1.1.8 initialized [DEVNET]
 * }</pre>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Set log level
 * Logger.setLogLevel(LogLevel.INFO);
 *
 * // Log messages
 * Logger.debug("Debug information");
 * Logger.info("SDK initialized");
 * Logger.warn("Deprecated API used");
 * Logger.error("Transaction failed", exception);
 *
 * // Formatted messages
 * Logger.info("Account %s has balance %d", address, balance);
 * }</pre>
 *
 * <p>Note: This logger is designed for SDK internal use and development.
 * For production applications, consider integrating with SLF4J or other
 * logging frameworks.</p>
 *
 * @see LogLevel
 * @since 1.1.6
 */
public class Logger {
    private static LogLevel currentLevel = LogLevel.DEBUG; // Default to DEBUG
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Sets the current log level for the SDK.
     *
     * <p>Only messages at or above the specified level will be logged.
     * For example, setting the level to INFO will log INFO, WARN, and ERROR
     * messages, but not DEBUG messages.</p>
     *
     * @param level the minimum log level to display
     */
    public static void setLogLevel(LogLevel level) {
        currentLevel = level;
    }

    /**
     * Gets the current log level.
     *
     * @return the current log level
     */
    public static LogLevel getLogLevel() {
        return currentLevel;
    }

    /**
     * Logs a DEBUG level message.
     *
     * @param message the message to log
     */
    public static void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }

    /**
     * Logs a formatted DEBUG level message.
     *
     * @param message the message format string (see {@link String#format(String, Object...)})
     * @param args the arguments for the format string
     */
    public static void debug(String message, Object... args) {
        log(LogLevel.DEBUG, String.format(message, args), null);
    }

    /**
     * Logs an INFO level message.
     *
     * @param message the message to log
     */
    public static void info(String message) {
        log(LogLevel.INFO, message, null);
    }

    /**
     * Logs a formatted INFO level message.
     *
     * @param message the message format string (see {@link String#format(String, Object...)})
     * @param args the arguments for the format string
     */
    public static void info(String message, Object... args) {
        log(LogLevel.INFO, String.format(message, args), null);
    }

    /**
     * Logs a WARN level message.
     *
     * @param message the warning message to log
     */
    public static void warn(String message) {
        log(LogLevel.WARN, message, null);
    }

    /**
     * Logs a formatted WARN level message.
     *
     * @param message the message format string (see {@link String#format(String, Object...)})
     * @param args the arguments for the format string
     */
    public static void warn(String message, Object... args) {
        log(LogLevel.WARN, String.format(message, args), null);
    }

    /**
     * Logs a WARN level message with an associated exception.
     *
     * @param message the warning message to log
     * @param throwable the exception associated with the warning
     */
    public static void warn(String message, Throwable throwable) {
        log(LogLevel.WARN, message, throwable);
    }

    /**
     * Logs an ERROR level message.
     *
     * @param message the error message to log
     */
    public static void error(String message) {
        log(LogLevel.ERROR, message, null);
    }

    /**
     * Logs a formatted ERROR level message.
     *
     * @param message the message format string (see {@link String#format(String, Object...)})
     * @param args the arguments for the format string
     */
    public static void error(String message, Object... args) {
        log(LogLevel.ERROR, String.format(message, args), null);
    }

    /**
     * Logs an ERROR level message with an associated exception.
     *
     * @param message the error message to log
     * @param throwable the exception that caused the error
     */
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
