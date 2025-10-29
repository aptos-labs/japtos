package com.aptoslabs.japtos.gasstation;

import com.aptoslabs.japtos.api.AptosConfig;

/**
 * Configuration options for the GasStationClient.
 */
public class GasStationClientOptions {
    private final AptosConfig.Network network;
    private final String apiKey;
    private final String baseUrl;
    private final String env;

    public GasStationClientOptions(Builder builder) {
        this.network = builder.network != null ? builder.network : AptosConfig.Network.MAINNET;
        this.apiKey = builder.apiKey;
        this.baseUrl = builder.baseUrl;
        this.env = builder.env != null ? builder.env : "prod";
    }

    public AptosConfig.Network getNetwork() {
        return network;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getEnv() {
        return env;
    }

    /**
     * Builder for GasStationClientOptions
     */
    public static class Builder {
        private AptosConfig.Network network;
        private String apiKey;
        private String baseUrl;
        private String env;

        public Builder network(AptosConfig.Network network) {
            this.network = network;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder env(String env) {
            this.env = env;
            return this;
        }

        public GasStationClientOptions build() {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalArgumentException("API key is required");
            }
            return new GasStationClientOptions(this);
        }
    }
}
