package com.aptoslabs.japtos.client.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the all-args constructors of the DTOs directly.
 *
 * <p>Gson instantiates these classes via {@code Unsafe}, which bypasses the constructor body.
 * These tests invoke the constructors explicitly so the field-assignment logic is verified.</p>
 */
class DtoConstructorTest {

    @Test
    @DisplayName("LedgerInfo all-args constructor populates every accessor")
    void ledgerInfoConstructor() {
        LedgerInfo info = new LedgerInfo(4, "100", "200", "3", "1", "50",
                "full_node", "60", "42");
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
    @DisplayName("Resource all-args constructor populates type and data")
    void resourceConstructor() {
        Object data = java.util.Map.of("coin", 100);
        Resource resource = new Resource("0x1::coin::CoinStore", data);
        assertEquals("0x1::coin::CoinStore", resource.getType());
        assertSame(data, resource.getData());
    }

    @Test
    @DisplayName("Transaction all-args constructor exposes every committed-transaction field")
    void transactionConstructor() {
        Object[] changes = new Object[]{"c"};
        Object[] events = new Object[]{"e"};
        Object payload = "p";
        Object signature = "s";
        Transaction txn = new Transaction("1", "0xh", "sc", "er", "scp", "7", true,
                "Executed successfully", "ar", changes, "0x1", "2", "5000", "100",
                "99", payload, signature, events, "123", "user_transaction");
        assertSame(changes, txn.getChanges());
        assertSame(events, txn.getEvents());
        assertSame(payload, txn.getPayload());
        assertSame(signature, txn.getSignature());
        assertEquals(7L, txn.getGasUsed());
    }
}
