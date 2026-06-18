package com.aptoslabs.japtos.api;

import com.aptoslabs.japtos.client.HttpClient;
import com.aptoslabs.japtos.client.HttpClientImpl;
import com.aptoslabs.japtos.client.dto.HttpResponse;
import com.aptoslabs.japtos.plugin.PluginSettings;
import com.aptoslabs.japtos.transaction.TransactionSubmitter;
import com.aptoslabs.japtos.utils.LogLevel;
import com.aptoslabs.japtos.utils.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration for the Aptos SDK.
 */
public class AptosConfig {
    private final String fullnode;
    private final String indexer;
    private final String faucet;
    private final HttpClient client;
    private final Network network;
    private final Map<String, PluginSettings> pluginSettings;
    private final TransactionSubmitter transactionSubmitter;
    private final LogLevel logLevel;

    public AptosConfig(AptosConfigBuilder builder) {
        this.fullnode = builder.fullnode;
        this.indexer = builder.indexer;
        this.faucet = builder.faucet;
        this.client = builder.client != null ? builder.client : new HttpClientImpl();
        this.network = builder.network;
        this.pluginSettings = builder.pluginSettings != null ? new HashMap<>(builder.pluginSettings) : new HashMap<>();
        this.transactionSubmitter = builder.transactionSubmitter;
        this.logLevel = builder.logLevel != null ? builder.logLevel : LogLevel.DEBUG;
        Logger.setLogLevel(this.logLevel);
    }

    public static AptosConfigBuilder builder() {
        return new AptosConfigBuilder();
    }

    public String getFullnode() {
        return fullnode;
    }

    public String getIndexer() {
        return indexer;
    }

    public String getFaucet() {
        return faucet;
    }

    public HttpClient getClient() {
        return client;
    }

    public Network getNetwork() {
        return network;
    }

    /**
     * Returns all registered plugin settings.
     *
     * @return immutable map of plugin name to settings
     */
    public Map<String, PluginSettings> getPluginSettings() {
        return pluginSettings;
    }
    
    /**
     * Gets the plugin settings for a specific plugin by name.
     *
     * @param pluginName the name of the plugin
     * @return Optional containing the settings if found, empty otherwise
     */
    public Optional<PluginSettings> getPluginSettings(String pluginName) {
        return Optional.ofNullable(pluginSettings.get(pluginName));
    }
    
    /**
     * Gets the plugin settings for a specific plugin by name with type casting.
     *
     * @param pluginName the name of the plugin
     * @param settingsClass the expected class of the settings
     * @param <T> the type of settings
     * @return Optional containing the settings if found and of correct type, empty otherwise
     */
    public <T extends PluginSettings> Optional<T> getPluginSettings(String pluginName, Class<T> settingsClass) {
        PluginSettings settings = pluginSettings.get(pluginName);
        if (settings != null && settingsClass.isInstance(settings)) {
            return Optional.of(settingsClass.cast(settings));
        }
        return Optional.empty();
    }

    public TransactionSubmitter getTransactionSubmitter() {
        return transactionSubmitter;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    /**
     * Network enumeration
     */
    public enum Network {
        MAINNET("https://fullnode.mainnet.aptoslabs.com", "https://indexer.mainnet.aptoslabs.com", "https://faucet.mainnet.aptoslabs.com"),
        TESTNET("https://fullnode.testnet.aptoslabs.com", "https://indexer.testnet.aptoslabs.com", "https://faucet.testnet.aptoslabs.com"),
        DEVNET("https://fullnode.devnet.aptoslabs.com", "https://indexer.devnet.aptoslabs.com", "https://faucet.devnet.aptoslabs.com"),
        LOCALNET("http://127.0.0.1:8080", "http://127.0.0.1:8080", "http://127.0.0.1:8081");

        private final String fullnode;
        private final String indexer;
        private final String faucet;
        private Integer cachedChainId = null;

        Network(String fullnode, String indexer, String faucet) {
            this.fullnode = fullnode;
            this.indexer = indexer;
            this.faucet = faucet;
        }

        public String getFullnode() {
            return fullnode;
        }

        public String getIndexer() {
            return indexer;
        }

        public String getFaucet() {
            return faucet;
        }

        public int getChainId() {
            if (cachedChainId == null) {
                // Fetch chain ID from the fullnode
                try {
                    HttpClient client = new HttpClientImpl();
                    String url = fullnode + "/v1";
                    HttpResponse response = client.get(url, null);
                    if (response.isSuccessful()) {
                        // Parse the response to get chain ID
                        // This is a simplified approach - in practice you might want to use a JSON parser
                        String body = response.getBody();
                        // Extract chain_id from the response
                        // For now, we'll use a simple approach and fetch it via the ledger info endpoint
                        String ledgerUrl = fullnode + "/v1/ledger_info";
                        HttpResponse ledgerResponse = client.get(ledgerUrl, null);
                        if (ledgerResponse.isSuccessful()) {
                            String ledgerBody = ledgerResponse.getBody();
                            // Simple JSON parsing to extract chain_id
                            int chainIdIndex = ledgerBody.indexOf("\"chain_id\":");
                            if (chainIdIndex != -1) {
                                int startIndex = chainIdIndex + 12; // length of "chain_id":
                                int endIndex = ledgerBody.indexOf(",", startIndex);
                                if (endIndex == -1) {
                                    endIndex = ledgerBody.indexOf("}", startIndex);
                                }
                                if (endIndex != -1) {
                                    String chainIdStr = ledgerBody.substring(startIndex, endIndex).trim();
                                    cachedChainId = Integer.parseInt(chainIdStr);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // If we can't fetch the chain ID, use a default value
                    Logger.warn("Could not fetch chain ID from %s, using default: %s", fullnode, e.getMessage());
                }

                // Fallback to default values if fetching fails
                if (cachedChainId == null) {
                    switch (this) {
                        case MAINNET:
                            cachedChainId = 1;
                            break;
                        case TESTNET:
                            cachedChainId = 2;
                            break;
                        case DEVNET:
                            cachedChainId = 200;
                            break;
                        case LOCALNET:
                            cachedChainId = 4;
                            break;
                        default:
                            cachedChainId = 1;
                            break;
                    }
                }
            }
            return cachedChainId;
        }
    }

    /**
     * Builder for AptosConfig
     */
    public static class AptosConfigBuilder {
        private String fullnode;
        private String indexer;
        private String faucet;
        private HttpClient client;
        private Network network;
        private Map<String, PluginSettings> pluginSettings;
        private TransactionSubmitter transactionSubmitter;
        private LogLevel logLevel;

        public AptosConfigBuilder() {
        }

        public AptosConfigBuilder fullnode(String fullnode) {
            this.fullnode = fullnode;
            return this;
        }

        public AptosConfigBuilder indexer(String indexer) {
            this.indexer = indexer;
            return this;
        }

        public AptosConfigBuilder faucet(String faucet) {
            this.faucet = faucet;
            return this;
        }

        public AptosConfigBuilder client(HttpClient client) {
            this.client = client;
            return this;
        }

        public AptosConfigBuilder network(Network network) {
            this.network = network;
            this.fullnode = network.getFullnode();
            this.indexer = network.getIndexer();
            this.faucet = network.getFaucet();
            return this;
        }

        /**
         * Sets all plugin settings at once.
         *
         * @param pluginSettings map of plugin name to settings
         * @return this builder
         */
        public AptosConfigBuilder pluginSettings(Map<String, PluginSettings> pluginSettings) {
            this.pluginSettings = pluginSettings;
            return this;
        }
        
        /**
         * Registers settings for a specific plugin.
         *
         * @param pluginName the name of the plugin
         * @param settings the plugin settings
         * @return this builder
         */
        public AptosConfigBuilder pluginSettings(String pluginName, PluginSettings settings) {
            if (this.pluginSettings == null) {
                this.pluginSettings = new HashMap<>();
            }
            this.pluginSettings.put(pluginName, settings);
            return this;
        }
        
        /**
         * Registers settings for a plugin using the plugin's own name.
         * The plugin name is obtained from settings.getPluginName().
         *
         * @param settings the plugin settings
         * @return this builder
         */
        public AptosConfigBuilder plugin(PluginSettings settings) {
            if (this.pluginSettings == null) {
                this.pluginSettings = new HashMap<>();
            }
            this.pluginSettings.put(settings.getPluginName(), settings);
            return this;
        }

        public AptosConfigBuilder transactionSubmitter(TransactionSubmitter transactionSubmitter) {
            this.transactionSubmitter = transactionSubmitter;
            return this;
        }

        public AptosConfigBuilder logLevel(LogLevel logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public AptosConfig build() {
            if (fullnode == null) {
                throw new IllegalArgumentException("Fullnode URL is required");
            }
            return new AptosConfig(this);
        }
    }
}
