package com.aptoslabs.japtos;

import com.aptoslabs.japtos.api.AptosConfig;
import com.aptoslabs.japtos.client.AptosClient;
import com.aptoslabs.japtos.utils.LogLevel;
import org.junit.jupiter.api.Test;

/**
 * Test logging with custom network configuration
 */
public class LoggingCustomNetworkTest {

    @Test
    public void testCustomNetworkLogging() {
        // Test with custom fullnode URL (no predefined network)
        AptosConfig config = AptosConfig.builder()
                .fullnode("https://custom.fullnode.example.com")
                .logLevel(LogLevel.INFO)
                .build();
                
        // This should log "CUSTOM" as the network
        AptosClient client = new AptosClient(config);
    }
}
