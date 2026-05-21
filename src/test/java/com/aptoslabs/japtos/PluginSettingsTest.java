package com.aptoslabs.japtos;

import com.aptoslabs.japtos.api.AptosConfig;
import com.aptoslabs.japtos.gasstation.GasStationSettings;
import com.aptoslabs.japtos.plugin.PluginSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the strongly-typed PluginSettings system.
 */
public class PluginSettingsTest {
    
    @Test
    @DisplayName("Test GasStationSettings can be registered and retrieved")
    void testGasStationSettingsRegistration() {
        // Create settings
        GasStationSettings settings = GasStationSettings.builder()
                .apiKey("test-key")
                .endpoint("https://gas-station.testnet.aptoslabs.com")
                .timeoutMillis(5000)
                .maxRetries(2)
                .build();
        
        // Register with AptosConfig using the plugin() method
        AptosConfig config = AptosConfig.builder()
                .network(AptosConfig.Network.TESTNET)
                .plugin(settings)
                .build();
        
        // Retrieve settings by name
        Optional<PluginSettings> retrieved = config.getPluginSettings("gas-station");
        assertTrue(retrieved.isPresent(), "Settings should be present");
        assertEquals("gas-station", retrieved.get().getPluginName());
        
        // Retrieve with type casting
        Optional<GasStationSettings> typedSettings = 
                config.getPluginSettings("gas-station", GasStationSettings.class);
        assertTrue(typedSettings.isPresent(), "Typed settings should be present");
        
        GasStationSettings gs = typedSettings.get();
        assertEquals("test-key", gs.getApiKey());
        assertEquals("https://gas-station.testnet.aptoslabs.com", gs.getEndpoint());
        assertEquals(5000, gs.getTimeoutMillis());
        assertEquals(2, gs.getMaxRetries());
    }
    
    @Test
    @DisplayName("Test manual registration with pluginSettings(name, settings)")
    void testManualRegistration() {
        GasStationSettings settings = GasStationSettings.builder()
                .apiKey("manual-key")
                .endpoint("https://custom-endpoint.com")
                .build();
        
        AptosConfig config = AptosConfig.builder()
                .network(AptosConfig.Network.TESTNET)
                .pluginSettings("gas-station", settings)
                .build();
        
        Optional<GasStationSettings> retrieved = 
                config.getPluginSettings("gas-station", GasStationSettings.class);
        assertTrue(retrieved.isPresent());
        assertEquals("manual-key", retrieved.get().getApiKey());
    }
    
    @Test
    @DisplayName("Test retrieving non-existent plugin returns empty")
    void testNonExistentPlugin() {
        AptosConfig config = AptosConfig.builder()
                .network(AptosConfig.Network.TESTNET)
                .build();
        
        Optional<PluginSettings> retrieved = config.getPluginSettings("non-existent");
        assertFalse(retrieved.isPresent(), "Non-existent plugin should return empty");
    }
    
    @Test
    @DisplayName("Test type casting to wrong type returns empty")
    void testWrongTypeCasting() {
        GasStationSettings settings = GasStationSettings.builder()
                .apiKey("test-key")
                .endpoint("https://test.com")
                .build();
        
        AptosConfig config = AptosConfig.builder()
                .network(AptosConfig.Network.TESTNET)
                .plugin(settings)
                .build();
        
        // Try to cast to a custom class that doesn't match
        Optional<CustomPluginSettings> wrongType = 
                config.getPluginSettings("gas-station", CustomPluginSettings.class);
        assertFalse(wrongType.isPresent(), "Wrong type cast should return empty");
    }
    
    @Test
    @DisplayName("Test GasStationSettings validation")
    void testGasStationSettingsValidation() {
        // Missing API key
        assertThrows(IllegalArgumentException.class, () -> 
                GasStationSettings.builder()
                        .endpoint("https://test.com")
                        .build()
        );
        
        // Missing endpoint
        assertThrows(IllegalArgumentException.class, () -> 
                GasStationSettings.builder()
                        .apiKey("test-key")
                        .build()
        );
    }
    
    // Custom plugin settings for testing type casting
    static class CustomPluginSettings implements PluginSettings {
        @Override
        public String getPluginName() {
            return "custom";
        }
    }
}

