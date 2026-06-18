package com.aptoslabs.japtos.utils;

import com.aptoslabs.japtos.utils.Logger;

import com.aptoslabs.japtos.api.AptosConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;

/**
 * Utility class for network operations including faucet funding
 */
public class FundingUtils {
    private static final Gson gson = new Gson();

    /**
     * Fund an account using the Aptos faucet for a specific network
     * <p>
     * <p>
     * Only supports LOCALNET and DEVNET networks for testing purposes.
     * TESTNET and MAINNET funding is not supported.
     */
    public static String fundAccount(String address, String amount, AptosConfig.Network network) throws IOException {
        // Only allow LOCALNET and DEVNET for funding
        if (network != AptosConfig.Network.LOCALNET && network != AptosConfig.Network.DEVNET) {
            throw new UnsupportedOperationException(
                    "Funding is only supported for LOCALNET and DEVNET networks. " +
                            "Attempted to fund on: " + network.name() + ". " +
                            "For TESTNET, use the official faucet at https://aptos.dev/network/faucet. " +
                            "For MAINNET, funding is not available."
            );
        }

        try {
            // Use the correct endpoint: /fund (not /mint)
            String url = network.getFaucet() + "/fund";

            // Create JSON payload
            JsonObject payload = new JsonObject();
            payload.addProperty("address", address);
            // Both LOCALNET and DEVNET support amount parameter
            payload.addProperty("amount", Long.parseLong(amount));

            RequestBody body = RequestBody.create(
                    gson.toJson(payload),
                    MediaType.get("application/json")
            );

            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json");

            // No authentication required for LOCALNET and DEVNET
            Request request = requestBuilder.build();

            OkHttpClient client = new OkHttpClient();
            try (Response response = client.newCall(request).execute()) {

                String responseBody = response.body().string();

                // Debug: Print the response for investigation
                Logger.info("   Faucet response code: " + response.code());
                Logger.info("   Faucet response body: " + responseBody);

                if (!response.isSuccessful()) {
                    throw new IOException("Failed to fund account: " + response.code() + " - " + responseBody);
                }

                // Parse response to extract transaction hash
                JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);

                // Check for txn_hashes array
                if (responseJson.has("txn_hashes") && responseJson.get("txn_hashes").isJsonArray()) {
                    var txnHashes = responseJson.getAsJsonArray("txn_hashes");
                    if (txnHashes.size() > 0) {
                        String hash = txnHashes.get(0).getAsString();
                        Logger.info("   Extracted transaction hash from txn_hashes: " + hash);
                        return hash;
                    }
                }

                // Fallback to hash field
                if (responseJson.has("hash")) {
                    String hash = responseJson.get("hash").getAsString();
                    Logger.info("   Extracted transaction hash from hash field: " + hash);
                    return hash;
                }

                throw new IOException("Unexpected response format: " + responseBody);
            }

        } catch (Exception e) {
            throw new IOException("Failed to fund account: " + e.getMessage(), e);
        }
    }

    /**
     * Fund an account using the Aptos faucet (legacy method - DEPRECATED)
     *
     * @deprecated Use fundAccount(address, amount, network) with explicit network parameter
     */
    @Deprecated
    public static String fundAccount(String address, String amount) throws IOException {
        throw new UnsupportedOperationException(
                "Legacy fundAccount method is no longer supported. " +
                        "Use fundAccount(address, amount, network) with explicit network parameter. " +
                        "Only LOCALNET and DEVNET networks are supported for funding."
        );
    }
}
