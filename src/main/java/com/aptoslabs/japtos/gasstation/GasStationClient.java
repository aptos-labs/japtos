package com.aptoslabs.japtos.gasstation;

import com.aptoslabs.japtos.api.AptosConfig;
import com.aptoslabs.japtos.client.HttpClient;
import com.aptoslabs.japtos.client.HttpClientImpl;
import com.aptoslabs.japtos.client.dto.HttpResponse;
import com.aptoslabs.japtos.transaction.SignedTransaction;
import com.aptoslabs.japtos.transaction.authenticator.AccountAuthenticator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Client for interacting with the Aptos Gas Station API.
 * This client submits transactions to be sponsored by a gas station fee payer.
 */
public class GasStationClient {
    private final GasStationClientOptions options;
    private final HttpClient httpClient;
    private final Gson gson;

    public GasStationClient(GasStationClientOptions options) {
        this(options, new HttpClientImpl());
    }

    public GasStationClient(GasStationClientOptions options, HttpClient httpClient) {
        this.options = options;
        this.httpClient = httpClient;
        this.gson = new GsonBuilder().create();
    }

    /**
     * Signs and submits a transaction to the gas station.
     *
     * @param transaction the raw transaction to submit (can be RawTransaction or RawTransactionWithFeePayer)
     * @param senderAuthenticator the authenticator for the transaction sender
     * @param secondaryAuthenticators optional authenticators for secondary signers
     * @param recaptchaToken optional recaptcha token
     * @return object containing the transaction hash
     * @throws Exception if the submission fails
     */
    public GasStationResponse signAndSubmitTransaction(
            Object transaction,
            AccountAuthenticator senderAuthenticator,
            List<AccountAuthenticator> secondaryAuthenticators,
            String recaptchaToken) throws Exception {

        String url = getBaseUrl() + "/api/transaction/signAndSubmit";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + options.getApiKey());

        // Convert transaction bytes to array of integers
        List<Integer> transactionBytes = bytesToIntegerList(getTransactionBytes(transaction));
        List<Integer> senderAuth = bytesToIntegerList(senderAuthenticator.bcsToBytes());

        List<List<Integer>> additionalSignersAuth = null;
        if (secondaryAuthenticators != null && !secondaryAuthenticators.isEmpty()) {
            additionalSignersAuth = secondaryAuthenticators.stream()
                    .map(auth -> {
                        try {
                            return bytesToIntegerList(auth.bcsToBytes());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
        }

        // Build request body
        JsonObject requestBody = new JsonObject();
        requestBody.add("transactionBytes", gson.toJsonTree(transactionBytes));
        requestBody.add("senderAuth", gson.toJsonTree(senderAuth));
        if (additionalSignersAuth != null) {
            requestBody.add("additionalSignersAuth", gson.toJsonTree(additionalSignersAuth));
        }
        if (recaptchaToken != null && !recaptchaToken.isEmpty()) {
            requestBody.addProperty("recaptchaToken", recaptchaToken);
        }

        byte[] bodyBytes = requestBody.toString().getBytes();

        // Make HTTP POST request
        HttpResponse response = httpClient.post(url, headers, bodyBytes);

        if (!response.isSuccessful()) {
            String errorMessage = "Failed to submit transaction to gas station: " +
                    response.getStatusCode() + " " + response.getBody();
            System.err.println("Gas Station Error Details:");
            System.err.println("URL: " + url);
            System.err.println("Status Code: " + response.getStatusCode());
            System.err.println("Response Body: " + response.getBody());
            System.err.println("Request Body: " + requestBody.toString());
            throw new GasStationClientException(errorMessage);
        }

        // Parse response
        GasStationResponse gasStationResponse = gson.fromJson(response.getBody(), GasStationResponse.class);
        if (gasStationResponse == null || gasStationResponse.getTransactionHash() == null) {
            throw new GasStationClientException("Invalid response from gas station: " + response.getBody());
        }

        return gasStationResponse;
    }

    /**
     * Gets the base URL for the gas station API.
     *
     * @return the base URL
     */
    private String getBaseUrl() {
        if (options.getBaseUrl() != null && !options.getBaseUrl().isEmpty()) {
            return options.getBaseUrl();
        }

        String envSegment = "prod".equals(options.getEnv()) ? "" : "." + options.getEnv();
        return String.format("https://api.%s%s.aptoslabs.com/gs/v1", 
                options.getNetwork().toString().toLowerCase(), envSegment);
    }

    /**
     * Extracts bytes from a transaction object.
     * The transaction should have a bcsToBytes() method.
     *
     * @param transaction the transaction object (RawTransaction, RawTransactionWithFeePayer, or SignedTransaction)
     * @return byte array
     * @throws Exception if extraction fails
     */
    private byte[] getTransactionBytes(Object transaction) throws Exception {
        if (transaction instanceof SignedTransaction) {
            return ((SignedTransaction) transaction).bcsToBytes();
        }
        // Use reflection to call bcsToBytes() on any transaction object
        try {
            return (byte[]) transaction.getClass().getMethod("bcsToBytes").invoke(transaction);
        } catch (Exception e) {
            throw new GasStationClientException("Failed to extract bytes from transaction: " + e.getMessage());
        }
    }

    /**
     * Converts a byte array to a list of integers.
     *
     * @param bytes the byte array
     * @return list of integers (unsigned byte values)
     */
    private List<Integer> bytesToIntegerList(byte[] bytes) {
        List<Integer> result = new ArrayList<>(bytes.length);
        for (byte b : bytes) {
            result.add(b & 0xFF);  // Convert signed byte to unsigned int
        }
        return result;
    }

    /**
     * Response object from gas station transaction submission.
     */
    public static class GasStationResponse {
        private String transactionHash;

        public String getTransactionHash() {
            return transactionHash;
        }

        public void setTransactionHash(String transactionHash) {
            this.transactionHash = transactionHash;
        }
    }

    /**
     * Exception thrown by GasStationClient operations.
     */
    public static class GasStationClientException extends RuntimeException {
        public GasStationClientException(String message) {
            super(message);
        }

        public GasStationClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
