package com.aptoslabs.japtos.utils;

/**
 * Enumeration of logging levels for the Japtos SDK.
 *
 * <p>Log levels are ordered from most verbose (DEBUG) to least verbose (ERROR).
 * When a log level is set, all messages at that level and above will be displayed.</p>
 *
 * <p>Level hierarchy:</p>
 * <ul>
 *   <li><strong>DEBUG</strong> (0) - Most verbose, includes detailed debugging information</li>
 *   <li><strong>INFO</strong> (1) - General informational messages about SDK operations</li>
 *   <li><strong>WARN</strong> (2) - Warning messages for potential issues or deprecations</li>
 *   <li><strong>ERROR</strong> (3) - Error messages for failures and exceptions</li>
 * </ul>
 *
 * <p>Example:</p>
 * <pre>{@code
 * // Set to INFO level - will show INFO, WARN, and ERROR messages
 * Logger.setLogLevel(LogLevel.INFO);
 *
 * Logger.debug("This won't be shown");  // Filtered out
 * Logger.info("This will be shown");     // Displayed
 * Logger.error("This will be shown");   // Displayed
 * }</pre>
 *
 * @see Logger
 * @since 1.1.6
 */
public enum LogLevel {
    /** Detailed debugging information (most verbose) */
    DEBUG(0),
    /** General informational messages */
    INFO(1),
    /** Warning messages for potential issues */
    WARN(2),
    /** Error messages for failures (least verbose) */
    ERROR(3);

    private final int level;

    /**
     * Creates a log level with the specified numeric value.
     *
     * @param level the numeric level (0 = most verbose, 3 = least verbose)
     */
    LogLevel(int level) {
        this.level = level;
    }

    /**
     * Gets the numeric value of this log level.
     *
     * @return the numeric level value
     */
    public int getLevel() {
        return level;
    }

    /**
     * Checks if this log level is enabled given the current log level setting.
     *
     * <p>A log level is enabled if its numeric value is greater than or equal
     * to the current log level. For example, if the current level is INFO (1),
     * then INFO, WARN, and ERROR messages will be enabled, but DEBUG messages
     * will be disabled.</p>
     *
     * @param currentLevel the currently configured log level
     * @return true if this level should be logged, false otherwise
     */
    public boolean isEnabled(LogLevel currentLevel) {
        return this.level >= currentLevel.level;
    }
}
