package com.aptoslabs.japtos.plugin;

/**
 * Base interface for plugin settings that can be registered with AptosConfig.
 * Each plugin should implement this interface to provide type-safe configuration.
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * public class GasStationSettings implements PluginSettings {
 *     private final String apiKey;
 *     private final String endpoint;
 *     
 *     // ... constructor, getters, builder ...
 *     
 *     @Override
 *     public String getPluginName() {
 *         return "gas-station";
 *     }
 * }
 * 
 * // Register in AptosConfig
 * AptosConfig config = AptosConfig.builder()
 *     .network(Network.TESTNET)
 *     .pluginSettings("gas-station", gasStationSettings)
 *     .build();
 * }</pre>
 */
public interface PluginSettings {
    /**
     * Returns the unique name/identifier for this plugin.
     * This is used as the key when registering settings with AptosConfig.
     *
     * @return the plugin identifier (e.g., "gas-station", "indexer", etc.)
     */
    String getPluginName();
}

