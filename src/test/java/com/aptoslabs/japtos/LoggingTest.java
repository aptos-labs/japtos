package com.aptoslabs.japtos;

import com.aptoslabs.japtos.api.AptosConfig;
import com.aptoslabs.japtos.client.AptosClient;
import com.aptoslabs.japtos.utils.LogLevel;
import com.aptoslabs.japtos.utils.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the logging functionality
 */
public class LoggingTest {

    @Test
    public void testLogLevelConfiguration() {
        // Test default log level
        assertEquals(LogLevel.DEBUG, Logger.getLogLevel());
        
        // Test setting log level via AptosConfig
        AptosConfig config = AptosConfig.builder()
                .network(AptosConfig.Network.DEVNET)
                .logLevel(LogLevel.ERROR)
                .build();
        
        assertEquals(LogLevel.ERROR, config.getLogLevel());
        assertEquals(LogLevel.ERROR, Logger.getLogLevel());
        
        // Test changing log level
        Logger.setLogLevel(LogLevel.INFO);
        assertEquals(LogLevel.INFO, Logger.getLogLevel());
        
        // Reset to DEBUG for other tests
        Logger.setLogLevel(LogLevel.DEBUG);
    }
    
    @Test
    public void testLogLevelHierarchy() {
        // Test log level filtering
        Logger.setLogLevel(LogLevel.WARN);
        
        // These should not print (but we can't easily test that)
        Logger.debug("This debug message should not appear");
        Logger.info("This info message should not appear");
        
        // These should print
        Logger.warn("This warning should appear");
        Logger.error("This error should appear");
        
        // Reset to DEBUG for other tests
        Logger.setLogLevel(LogLevel.DEBUG);
    }
    
    @Test
    public void testAptosClientInitialization() {
        // Test that initialization log is triggered
        Logger.setLogLevel(LogLevel.INFO);
        
        AptosConfig config = AptosConfig.builder()
                .network(AptosConfig.Network.DEVNET)
                .logLevel(LogLevel.INFO)
                .build();
                
        // This should log the initialization message
        AptosClient client = new AptosClient(config);
        
        // Reset to DEBUG for other tests
        Logger.setLogLevel(LogLevel.DEBUG);
    }
}
