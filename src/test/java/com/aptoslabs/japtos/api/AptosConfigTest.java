package com.aptoslabs.japtos.api;

import com.aptoslabs.japtos.client.HttpClient;
import com.aptoslabs.japtos.client.HttpClientImpl;
import com.aptoslabs.japtos.gasstation.GasStationSettings;
import com.aptoslabs.japtos.plugin.PluginSettings;
import com.aptoslabs.japtos.utils.LogLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AptosConfig} and its builder.
 */
class AptosConfigTest {

    @Test
    @DisplayName("network(...) populates fullnode/indexer/faucet from the enum")
    void networkPopulatesEndpoints() {
        AptosConfig config = AptosConfig.builder().network(AptosConfig.Network.TESTNET).build();
        assertEquals(AptosConfig.Network.TESTNET, config.getNetwork());
        assertEquals(AptosConfig.Network.TESTNET.getFullnode(), config.getFullnode());
        assertEquals(AptosConfig.Network.TESTNET.getIndexer(), config.getIndexer());
        assertEquals(AptosConfig.Network.TESTNET.getFaucet(), config.getFaucet());
        // Default HTTP client is provided automatically.
        assertNotNull(config.getClient());
        assertNull(config.getTransactionSubmitter());
        assertEquals(LogLevel.DEBUG, config.getLogLevel());
    }

    @Test
    @DisplayName("Explicit fullnode, client and log level are honoured")
    void explicitFields() {
        HttpClient client = new HttpClientImpl();
        AptosConfig config = AptosConfig.builder()
                .fullnode("http://localhost:9999")
                .indexer("http://localhost:9998")
                .faucet("http://localhost:9997")
                .client(client)
                .logLevel(LogLevel.ERROR)
                .build();
        assertEquals("http://localhost:9999", config.getFullnode());
        assertEquals("http://localhost:9998", config.getIndexer());
        assertEquals("http://localhost:9997", config.getFaucet());
        assertSame(client, config.getClient());
        assertEquals(LogLevel.ERROR, config.getLogLevel());
    }

    @Test
    @DisplayName("build() requires a fullnode URL")
    void buildRequiresFullnode() {
        assertThrows(IllegalArgumentException.class, () -> AptosConfig.builder().build());
    }

    @Test
    @DisplayName("Network enum exposes default endpoints for every network")
    void networkEndpoints() {
        assertTrue(AptosConfig.Network.MAINNET.getFullnode().contains("mainnet"));
        assertTrue(AptosConfig.Network.DEVNET.getIndexer().contains("devnet"));
        assertTrue(AptosConfig.Network.LOCALNET.getFaucet().contains("127.0.0.1"));
    }

    @Test
    @DisplayName("plugin(...) registers settings under the plugin's own name")
    void pluginRegistrationByName() {
        GasStationSettings settings = GasStationSettings.builder()
                .apiKey("k").endpoint("http://gs").build();
        AptosConfig config = AptosConfig.builder()
                .network(AptosConfig.Network.TESTNET)
                .plugin(settings)
                .build();

        Optional<PluginSettings> generic = config.getPluginSettings(GasStationSettings.PLUGIN_NAME);
        assertTrue(generic.isPresent());

        Optional<GasStationSettings> typed =
                config.getPluginSettings(GasStationSettings.PLUGIN_NAME, GasStationSettings.class);
        assertTrue(typed.isPresent());
        assertEquals("k", typed.get().getApiKey());

        // Unknown plugin name -> empty
        assertTrue(config.getPluginSettings("missing").isEmpty());
        // Wrong type cast -> empty
        assertTrue(config.getPluginSettings(GasStationSettings.PLUGIN_NAME, WrongSettings.class).isEmpty());

        assertEquals(1, config.getPluginSettings().size());
    }

    @Test
    @DisplayName("pluginSettings(name, settings) and pluginSettings(map) both register entries")
    void pluginRegistrationVariants() {
        GasStationSettings settings = GasStationSettings.builder()
                .apiKey("k").endpoint("http://gs").build();

        AptosConfig byName = AptosConfig.builder()
                .network(AptosConfig.Network.TESTNET)
                .pluginSettings("custom", settings)
                .build();
        assertTrue(byName.getPluginSettings("custom").isPresent());

        AptosConfig byMap = AptosConfig.builder()
                .network(AptosConfig.Network.TESTNET)
                .pluginSettings(Map.of("custom", settings))
                .build();
        assertTrue(byMap.getPluginSettings("custom").isPresent());
    }

    /** A second PluginSettings type used to validate type-mismatched lookups. */
    private static class WrongSettings implements PluginSettings {
        public String getPluginName() { return "wrong"; }
    }
}
