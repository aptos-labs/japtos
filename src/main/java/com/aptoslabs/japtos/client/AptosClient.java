package com.aptoslabs.japtos.client;

import com.aptoslabs.japtos.api.AptosConfig;
import com.aptoslabs.japtos.client.dto.*;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.transaction.SignedTransaction;
import com.aptoslabs.japtos.transaction.TransactionSubmitter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Main client for interacting with the Aptos blockchain.
 *
 * <p>This class provides a comprehensive interface for interacting with Aptos nodes,
 * including transaction submission, account information retrieval, and blockchain
 * state queries. It handles HTTP communication with Aptos fullnodes and indexers.</p>
 *
 * <p>The client supports both synchronous and asynchronous operations, with proper
 * error handling and response parsing. It uses BCS (Binary Canonical Serialization)
 * for transaction submission and JSON for most other operations.</p>
 *
 * <p>Designed by Aptos Labs.</p>
 *
 * @author @rrigoni
 * @since 1.0.0
 */
public class AptosClient {
    private final AptosConfig config;
    private final Gson gson;
    private final TransactionSubmitter transactionSubmitter;

    /**
     * Constructs a new AptosClient with the specified configuration.
     *
     * @param config the configuration containing network settings and HTTP client
     */
    public AptosClient(AptosConfig config) {
        this.config = config;
        this.gson = new GsonBuilder().create();
        this.transactionSubmitter = config.getTransactionSubmitter();
    }

    /**
     * Constructs a new AptosClient with the specified network.
     *
     * @param network the network to connect to (e.g., MAINNET, DEVNET, TESTNET, LOCALNET)
     */
    public AptosClient(AptosConfig.Network network) {
        this(AptosConfig.builder().network(network).build());
    }

    /**
     * Constructs a new AptosClient with a custom fullnode URL.
     *
     * @param fullnodeUrl the custom fullnode endpoint URL
     */
    public AptosClient(String fullnodeUrl) {
        this(AptosConfig.builder().fullnode(fullnodeUrl).build());
    }

    /**
     * Retrieves account information for the specified address.
     *
     * @param address the account address to query
     * @return the account information including sequence number and authentication key
     * @throws Exception if the account lookup fails or the address is invalid
     */
    public AccountInfo getAccount(AccountAddress address) throws Exception {
        String url = config.getFullnode() + "/v1/accounts/" + address.toString();
        Map<String, String> headers = getDefaultHeaders();

        HttpResponse response = config.getClient().get(url, headers);

        if (!response.isSuccessful()) {
            throw new AptosClientException("Failed to get account: " + response.getStatusCode() + " " + response.getBody());
        }

        return gson.fromJson(response.getBody(), AccountInfo.class);
    }

    /**
     * Retrieves current ledger information from the blockchain.
     *
     * @return the ledger information including chain ID, ledger version, and timestamp
     * @throws Exception if the ledger info request fails
     */
    public LedgerInfo getLedgerInfo() throws Exception {
        String url = config.getFullnode() + "/v1";
        Map<String, String> headers = getDefaultHeaders();

        HttpResponse response = config.getClient().get(url, headers);

        if (!response.isSuccessful()) {
            throw new AptosClientException("Failed to get ledger info: " + response.getStatusCode() + " " + response.getBody());
        }

        return gson.fromJson(response.getBody(), LedgerInfo.class);
    }

    /**
     * Submits a signed transaction to the Aptos blockchain.
     * If a pluggable TransactionSubmitter is configured, it will be used.
     * Otherwise, the transaction is submitted to the default fullnode endpoint.
     *
     * @param signedTransaction the complete signed transaction to submit
     * @return a pending transaction object containing the transaction hash
     * @throws Exception if the transaction submission fails
     */
    public PendingTransaction submitTransaction(SignedTransaction signedTransaction) throws Exception {
        // If a custom transaction submitter is configured (e.g., gas station), use it
        if (transactionSubmitter != null) {
            return transactionSubmitter.submitTransaction(signedTransaction);
        }

        // Otherwise, use the default submission to the fullnode
        String url = config.getFullnode() + "/v1/transactions";
        Map<String, String> headers = getDefaultHeaders();
        headers.put("Content-Type", "application/x.aptos.signed_transaction+bcs");

        byte[] transactionBytes = signedTransaction.bcsToBytes();

        HttpResponse response = config.getClient().post(url, headers, transactionBytes);

        if (!response.isSuccessful()) {
            throw new AptosClientException("Failed to submit transaction: " + response.getStatusCode() + " " + response.getBody());
        }

        return gson.fromJson(response.getBody(), PendingTransaction.class);
    }


    /**
     * Waits for a transaction to be committed to the blockchain.
     *
     * @param hash the transaction hash to wait for
     * @return the committed transaction details
     * @throws Exception if the transaction fails or times out
     */
    public Transaction waitForTransaction(String hash) throws Exception {
        return waitForTransaction(hash, 0);
    }

    /**
     * Waits for a transaction to be committed with retry logic.
     *
     * @param hash     the transaction hash to wait for
     * @param attempts the current attempt number (used for timeout)
     * @return the committed transaction details
     * @throws Exception if the transaction fails or maximum attempts exceeded
     */
    private Transaction waitForTransaction(String hash, int attempts) throws Exception {
        if (attempts > 10) { // 20 seconds timeout
            throw new AptosClientException("Transaction timeout: transaction remained pending for too long. This may indicate that the LOCALNET node is not processing transactions properly.");
        }

        String url = config.getFullnode() + "/v1/transactions/by_hash/" + hash + "?wait=true";
        Map<String, String> headers = getDefaultHeaders();

        System.out.println("   Waiting for transaction: " + hash + " (attempt " + (attempts + 1) + ")");
        System.out.println("   URL: " + url);

        HttpResponse response = config.getClient().get(url, headers);

        System.out.println("   Transaction response code: " + response.getStatusCode());
        System.out.println("   Transaction response body: " + response.getBody());

        if (!response.isSuccessful()) {
            throw new AptosClientException("Failed to wait for transaction: " + response.getStatusCode() + " " + response.getBody());
        }

        Transaction transaction = gson.fromJson(response.getBody(), Transaction.class);
        System.out.println("   Transaction type: " + transaction.getType());
        System.out.println("   Transaction success: " + transaction.isSuccess());
        System.out.println("   Transaction VM status: " + transaction.getVmStatus());

        // Check if transaction is still pending
        if ("pending_transaction".equals(transaction.getType())) {
            System.out.println("   Transaction is still pending, waiting for it to be committed...");
            // Wait a bit more and try again
            Thread.sleep(2000);
            return waitForTransaction(hash, attempts + 1);
        }

        // Check if transaction failed
        if (!transaction.isSuccess()) {
            throw new AptosClientException("Transaction failed with VM status: " + transaction.getVmStatus());
        }

        System.out.println("   Transaction committed successfully");
        return transaction;
    }

    /**
     * Retrieves a transaction by its hash.
     *
     * @param hash the transaction hash to lookup
     * @return the transaction details
     * @throws Exception if the transaction lookup fails or hash is invalid
     */
    public Transaction getTransactionByHash(String hash) throws Exception {
        String url = config.getFullnode() + "/v1/transactions/by_hash/" + hash;
        Map<String, String> headers = getDefaultHeaders();

        HttpResponse response = config.getClient().get(url, headers);

        if (!response.isSuccessful()) {
            throw new AptosClientException("Failed to get transaction: " + response.getStatusCode() + " " + response.getBody());
        }

        return gson.fromJson(response.getBody(), Transaction.class);
    }

    /**
     * Creates default headers for API requests to Aptos nodes.
     *
     * @return a map containing standard headers for HTTP requests
     */
    private Map<String, String> getDefaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("User-Agent", "aptos-java-japtos/1.0.0");
        return headers;
    }

    /**
     * Gets the current Aptos configuration.
     *
     * @return the configuration object containing network settings
     */
    public AptosConfig getConfig() {
        return config;
    }

    /**
     * Retrieves the APT coin balance for the specified account.
     *
     * @param accountAddress the account address to query for APT balance
     * @return the APT balance in octas (smallest unit of APT)
     * @throws Exception if the balance lookup fails
     */
    public long getAccountCoinAmount(AccountAddress accountAddress) throws Exception {
        return getAccountCoinAmount(accountAddress, "0x1::aptos_coin::AptosCoin");
    }

    /**
     * Retrieves the balance of a specific token for the specified account.
     *
     * @param accountAddress the account address to query
     * @param assetType      the asset type identifier (e.g., "0x1::aptos_coin::AptosCoin")
     * @return the token balance in the smallest unit (octas for APT)
     * @throws Exception if the balance lookup fails
     */
    public long getAccountCoinAmount(AccountAddress accountAddress, String assetType) throws Exception {
        String url = config.getFullnode() + "/v1/accounts/" + accountAddress.toString() + "/balance/" + assetType;
        Map<String, String> headers = getDefaultHeaders();
        headers.put("Accept", "application/json");

        HttpResponse response = config.getClient().get(url, headers);

        if (!response.isSuccessful()) {
            throw new AptosClientException("Failed to get account balance: " + response.getStatusCode() + " " + response.getBody());
        }

        String amount = "0";
        try {
            amount = String.valueOf(response.getBody());
        } catch (Exception e) {
            throw new AptosClientException("Failed to deserialize balance response: " + e.getMessage());
        }
        return Long.parseLong(amount);
    }

    /**
     * Retrieves the next sequence number for an account.
     *
     * @param address the account address to get the sequence number for
     * @return the next sequence number for transactions
     * @throws Exception if the account lookup fails
     */
    public long getNextSequenceNumber(AccountAddress address) throws Exception {
        AccountInfo accountInfo = getAccount(address);
        return accountInfo.getSequenceNumber();
    }

    /**
     * Custom exception for AptosClient operations.
     *
     * <p>This exception is thrown when operations fail due to network issues,
     * invalid responses, or other client-related errors.</p>
     */
    public static class AptosClientException extends RuntimeException {
        /**
         * Constructs a new AptosClientException with the specified message.
         *
         * @param message the detail message explaining the error
         */
        public AptosClientException(String message) {
            super(message);
        }

        /**
         * Constructs a new AptosClientException with the specified message and cause.
         *
         * @param message the detail message explaining the error
         * @param cause   the underlying cause of the exception
         */
        public AptosClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}


