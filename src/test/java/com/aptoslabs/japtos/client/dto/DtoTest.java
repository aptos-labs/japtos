package com.aptoslabs.japtos.client.dto;

import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the response DTOs, exercising both the Gson deserialization paths
 * (using the {@code @SerializedName} mappings) and the string-to-number accessors.
 */
class DtoTest {

    private final Gson gson = new Gson();

    @Test
    @DisplayName("LedgerInfo parses snake_case JSON and converts numeric strings")
    void ledgerInfo() {
        String json = "{\"chain_id\":4,\"ledger_version\":\"100\",\"ledger_timestamp\":\"200\"," +
                "\"epoch\":\"3\",\"oldest_block_height\":\"1\",\"oldest_block_timestamp\":\"50\"," +
                "\"node_role\":\"full_node\",\"oldest_ledger_timestamp\":\"60\",\"block_height\":\"42\"}";
        LedgerInfo info = gson.fromJson(json, LedgerInfo.class);
        assertEquals(4, info.getChainId());
        assertEquals(100L, info.getLedgerVersion());
        assertEquals(200L, info.getLedgerTimestamp());
        assertEquals(3L, info.getEpoch());
        assertEquals(1L, info.getOldestBlockHeight());
        assertEquals(50L, info.getOldestBlockTimestamp());
        assertEquals("full_node", info.getNodeRole());
        assertEquals(60L, info.getOldestLedgerTimestamp());
        assertEquals(42L, info.getBlockHeight());
    }

    @Test
    @DisplayName("AccountInfo parses sequence number and authentication key")
    void accountInfo() {
        AccountInfo info = gson.fromJson(
                "{\"sequence_number\":\"15\",\"authentication_key\":\"0xabc\"}", AccountInfo.class);
        assertEquals(15L, info.getSequenceNumber());
        assertEquals("0xabc", info.getAuthenticationKey());

        AccountInfo direct = new AccountInfo("7", "0xdef");
        assertEquals(7L, direct.getSequenceNumber());
        assertEquals("0xdef", direct.getAuthenticationKey());
    }

    @Test
    @DisplayName("AccountBalance exposes all fields and a descriptive toString")
    void accountBalance() {
        String json = "{\"amount\":\"500\",\"asset_type\":\"0x1::aptos_coin::AptosCoin\"," +
                "\"is_frozen\":false,\"is_primary\":true,\"last_transaction_timestamp\":\"t\"," +
                "\"last_transaction_version\":\"v\",\"owner_address\":\"0x1\"," +
                "\"storage_id\":\"s\",\"token_standard\":\"v2\"}";
        AccountBalance balance = gson.fromJson(json, AccountBalance.class);
        assertEquals(500L, balance.getAmount());
        assertEquals("0x1::aptos_coin::AptosCoin", balance.getAssetType());
        assertFalse(balance.isFrozen());
        assertTrue(balance.isPrimary());
        assertEquals("t", balance.getLastTransactionTimestamp());
        assertEquals("v", balance.getLastTransactionVersion());
        assertEquals("0x1", balance.getOwnerAddress());
        assertEquals("s", balance.getStorageId());
        assertEquals("v2", balance.getTokenStandard());
        assertTrue(balance.toString().contains("AccountBalance"));
    }

    @Test
    @DisplayName("AccountBalancesResponse wraps a list of balances")
    void accountBalancesResponse() {
        AccountBalance b = new AccountBalance("1", "a", false, true, "t", "v", "o", "s", "v2");
        AccountBalancesResponse response = new AccountBalancesResponse(List.of(b));
        assertEquals(1, response.getBalances().size());
        assertEquals(1L, response.getBalances().get(0).getAmount());
    }

    @Test
    @DisplayName("PendingTransaction supports both the hash-only and full constructors")
    void pendingTransaction() {
        PendingTransaction hashOnly = new PendingTransaction("0xhash");
        assertEquals("0xhash", hashOnly.getHash());

        String json = "{\"hash\":\"0xh\",\"sender\":\"0x1\",\"sequence_number\":\"9\"," +
                "\"max_gas_amount\":\"1000\",\"gas_unit_price\":\"100\"," +
                "\"expiration_timestamp_secs\":\"123\",\"payload\":{},\"signature\":{}}";
        PendingTransaction full = gson.fromJson(json, PendingTransaction.class);
        assertEquals("0xh", full.getHash());
        assertEquals("0x1", full.getSender());
        assertEquals(9L, full.getSequenceNumber());
        assertEquals(1000L, full.getMaxGasAmount());
        assertEquals(100L, full.getGasUnitPrice());
        assertEquals(123L, full.getExpirationTimestampSecs());
        assertNotNull(full.getPayload());
        assertNotNull(full.getSignature());
    }

    @Test
    @DisplayName("Transaction parses commit metadata and tolerates missing gas_used")
    void transaction() {
        String json = "{\"version\":\"10\",\"hash\":\"0xh\",\"state_change_hash\":\"sc\"," +
                "\"event_root_hash\":\"er\",\"state_checkpoint_hash\":\"scp\",\"gas_used\":\"7\"," +
                "\"success\":true,\"vm_status\":\"Executed successfully\",\"accumulator_root_hash\":\"ar\"," +
                "\"sender\":\"0x1\",\"sequence_number\":\"2\",\"max_gas_amount\":\"5000\"," +
                "\"gas_unit_price\":\"100\",\"expiration_timestamp_secs\":\"99\",\"timestamp\":\"123\"," +
                "\"type\":\"user_transaction\"}";
        Transaction txn = gson.fromJson(json, Transaction.class);
        assertEquals(10L, txn.getVersion());
        assertEquals("0xh", txn.getHash());
        assertEquals("sc", txn.getStateChangeHash());
        assertEquals("er", txn.getEventRootHash());
        assertEquals("scp", txn.getStateCheckpointHash());
        assertEquals(7L, txn.getGasUsed());
        assertTrue(txn.isSuccess());
        assertEquals("Executed successfully", txn.getVmStatus());
        assertEquals("ar", txn.getAccumulatorRootHash());
        assertEquals("0x1", txn.getSender());
        assertEquals(2L, txn.getSequenceNumber());
        assertEquals(5000L, txn.getMaxGasAmount());
        assertEquals(100L, txn.getGasUnitPrice());
        assertEquals(99L, txn.getExpirationTimestampSecs());
        assertEquals(123L, txn.getTimestamp());
        assertEquals("user_transaction", txn.getType());
        assertNull(txn.getChanges());
        assertNull(txn.getEvents());
        assertNull(txn.getPayload());
        assertNull(txn.getSignature());
    }

    @Test
    @DisplayName("Transaction.getGasUsed returns 0 for null, empty or malformed values")
    void transactionGasUsedFallback() {
        Transaction nullGas = new Transaction(null, null, null, null, null, null, false, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
        assertEquals(0L, nullGas.getGasUsed());

        Transaction emptyGas = new Transaction(null, null, null, null, null, "", false, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
        assertEquals(0L, emptyGas.getGasUsed());

        Transaction badGas = new Transaction(null, null, null, null, null, "not-a-number", false, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
        assertEquals(0L, badGas.getGasUsed());
    }

    @Test
    @DisplayName("Resource exposes type and data")
    void resource() {
        Resource resource = gson.fromJson("{\"type\":\"0x1::coin::CoinStore\",\"data\":{\"x\":1}}",
                Resource.class);
        assertEquals("0x1::coin::CoinStore", resource.getType());
        assertNotNull(resource.getData());
    }

    @Test
    @DisplayName("HttpResponse computes success status and exposes body in both forms")
    void httpResponse() {
        HttpResponse ok = new HttpResponse(200, "OK", "{}", Map.of("k", "v"));
        assertEquals(200, ok.getStatusCode());
        assertEquals("OK", ok.getStatusText());
        assertEquals("{}", ok.getBody());
        assertArrayEquals("{}".getBytes(), ok.getBodyBytes());
        assertEquals("v", ok.getHeaders().get("k"));
        assertTrue(ok.isSuccessful());

        HttpResponse created = new HttpResponse(201, "Created", "body".getBytes(), Map.of());
        assertEquals("body", created.getBody());
        assertTrue(created.isSuccessful());

        HttpResponse serverError = new HttpResponse(500, "Error", (String) null, Map.of());
        assertFalse(serverError.isSuccessful());
        assertNull(serverError.getBody());
        assertNull(serverError.getBodyBytes());
    }
}
