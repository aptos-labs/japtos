package com.aptoslabs.japtos.gasstation;

import com.aptoslabs.japtos.api.AptosConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GasStationSettings} and {@link GasStationClientOptions} builders.
 */
class GasStationConfigTest {

    @Test
    @DisplayName("GasStationSettings builder applies defaults and validates required fields")
    void gasStationSettings() {
        GasStationSettings settings = GasStationSettings.builder()
                .apiKey("api-key")
                .endpoint("https://gs.example.com")
                .build();
        assertEquals("gas-station", settings.getPluginName());
        assertEquals("api-key", settings.getApiKey());
        assertEquals("https://gs.example.com", settings.getEndpoint());
        assertEquals(30000, settings.getTimeoutMillis());
        assertEquals(3, settings.getMaxRetries());

        GasStationSettings custom = GasStationSettings.builder()
                .apiKey("k").endpoint("e").timeoutMillis(1000).maxRetries(5).build();
        assertEquals(1000, custom.getTimeoutMillis());
        assertEquals(5, custom.getMaxRetries());

        assertThrows(IllegalArgumentException.class,
                () -> GasStationSettings.builder().endpoint("e").build());
        assertThrows(IllegalArgumentException.class,
                () -> GasStationSettings.builder().apiKey("k").build());
    }

    @Test
    @DisplayName("GasStationClientOptions builder applies defaults and validates the API key")
    void gasStationClientOptions() {
        GasStationClientOptions options = new GasStationClientOptions.Builder()
                .apiKey("api-key")
                .build();
        assertEquals(AptosConfig.Network.MAINNET, options.getNetwork());
        assertEquals("api-key", options.getApiKey());
        assertEquals("prod", options.getEnv());
        assertNull(options.getBaseUrl());

        GasStationClientOptions custom = new GasStationClientOptions.Builder()
                .network(AptosConfig.Network.TESTNET)
                .apiKey("k")
                .baseUrl("https://custom")
                .env("staging")
                .build();
        assertEquals(AptosConfig.Network.TESTNET, custom.getNetwork());
        assertEquals("https://custom", custom.getBaseUrl());
        assertEquals("staging", custom.getEnv());

        assertThrows(IllegalArgumentException.class,
                () -> new GasStationClientOptions.Builder().build());
        assertThrows(IllegalArgumentException.class,
                () -> new GasStationClientOptions.Builder().apiKey("").build());
    }
}
