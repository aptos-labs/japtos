package com.aptoslabs.japtos.api;

import com.aptoslabs.japtos.client.HttpClient;
import com.aptoslabs.japtos.client.HttpClientImpl;
import com.aptoslabs.japtos.client.dto.HttpResponse;
import com.aptoslabs.japtos.transaction.TransactionSubmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the Aptos SDK.
 */
public class AptosConfig {
    private final String fullnode;
    private final String indexer;
    private final String faucet;
    private final HttpClient client;
    private final Network network;
    private final Map<String, Object> pluginSettings;
    private final TransactionSubmitter transactionSubmitter;

    public AptosConfig(AptosConfigBuilder builder) {
        this.fullnode = builder.fullnode;
        this.indexer = builder.indexer;
        this.faucet = builder.faucet;
        this.client = builder.client != null ? builder.client : new HttpClientImpl();
        this.network = builder.network;
        this.pluginSettings = builder.pluginSettings != null ? new HashMap<>(builder.pluginSettings) : new HashMap<>();
        this.transactionSubmitter = builder.transactionSubmitter;
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

    public Map<String, Object> getPluginSettings() {
        return pluginSettings;
    }

    public TransactionSubmitter getTransactionSubmitter() {
        return transactionSubmitter;
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
                    System.err.println("Warning: Could not fetch chain ID from " + fullnode + ", using default: " + e.getMessage());
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
        private Map<String, Object> pluginSettings;
        private TransactionSubmitter transactionSubmitter;

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

        public AptosConfigBuilder pluginSettings(Map<String, Object> pluginSettings) {
            this.pluginSettings = pluginSettings;
            return this;
        }

        public AptosConfigBuilder transactionSubmitter(TransactionSubmitter transactionSubmitter) {
            this.transactionSubmitter = transactionSubmitter;
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
