package com.aptoslabs.japtos.gasstation;

import com.aptoslabs.japtos.plugin.PluginSettings;

/**
 * Settings for the Gas Station plugin.
 * This provides type-safe configuration for gas-sponsored transactions.
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * GasStationSettings settings = GasStationSettings.builder()
 *     .apiKey("your-api-key")
 *     .endpoint("https://gas-station.testnet.aptoslabs.com")
 *     .build();
 * 
 * AptosConfig config = AptosConfig.builder()
 *     .network(Network.TESTNET)
 *     .plugin(settings)
 *     .build();
 * 
 * // Later retrieve settings
 * Optional<GasStationSettings> gsSettings = config.getPluginSettings("gas-station", GasStationSettings.class);
 * }</pre>
 */
public class GasStationSettings implements PluginSettings {
    
    public static final String PLUGIN_NAME = "gas-station";
    
    private final String apiKey;
    private final String endpoint;
    private final int timeoutMillis;
    private final int maxRetries;
    
    private GasStationSettings(Builder builder) {
        this.apiKey = builder.apiKey;
        this.endpoint = builder.endpoint;
        this.timeoutMillis = builder.timeoutMillis;
        this.maxRetries = builder.maxRetries;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public int getTimeoutMillis() {
        return timeoutMillis;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public static class Builder {
        private String apiKey;
        private String endpoint;
        private int timeoutMillis = 30000; // 30 seconds default
        private int maxRetries = 3; // 3 retries default
        
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }
        
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }
        
        public Builder timeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }
        
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public GasStationSettings build() {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalArgumentException("API key is required");
            }
            if (endpoint == null || endpoint.isEmpty()) {
                throw new IllegalArgumentException("Endpoint is required");
            }
            return new GasStationSettings(this);
        }
    }
}

