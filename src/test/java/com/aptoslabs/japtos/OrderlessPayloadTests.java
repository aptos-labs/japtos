package com.aptoslabs.japtos;

import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.types.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for orderless TransactionInnerPayloadV1 serialization.
 */
public class OrderlessPayloadTests {

    @Test
    @DisplayName("Orderless payload encodes correct variant tags and extra config")
    void testOrderlessPayloadSerialization() throws Exception {
        // Build a trivial entry function payload (no types/args) for serialization testing only
        ModuleId moduleId = new ModuleId(
                AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"),
                new Identifier("coin")
        );
        Identifier fn = new Identifier("noop");
        EntryFunctionPayload entry = new EntryFunctionPayload(
                moduleId,
                fn,
                java.util.List.of(),
                java.util.List.of()
        );

        long nonce = 12345L;
        TransactionPayload orderless = new TransactionInnerPayloadV1(
                TransactionExecutableEntryFunction.fromEntryFunctionPayload(entry),
                new TransactionExtraConfigV1(null, nonce)
        );

        byte[] bytes = orderless.bcsToBytes();
        assertNotNull(bytes);
        assertTrue(bytes.length > 5);

        // Expect prefix: TransactionPayloadVariants.Payload = 4, TransactionInnerPayloadVariants.V1 = 0
        assertEquals((byte) 0x04, bytes[0], "Expected top-level payload variant 4");
        assertEquals((byte) 0x00, bytes[1], "Expected inner payload variant V1 (=0)");

        // Expect executable variant next: TransactionExecutableVariants.EntryFunction = 1
        assertEquals((byte) 0x01, bytes[2], "Expected executable variant EntryFunction (=1)");

        // Check trailing extra config bytes: variant=0, multisig None (0), replay Some (1), u64 little-endian
        int n = bytes.length;
        assertTrue(n >= 12);
        byte replayPresent = bytes[n - 9];
        byte multisigPresent = bytes[n - 10];
        byte extraCfgVariant = bytes[n - 11];
        assertEquals((byte) 0x01, replayPresent, "replay nonce should be present");
        assertEquals((byte) 0x00, multisigPresent, "multisig should be none");
        assertEquals((byte) 0x00, extraCfgVariant, "extra config V1 variant should be 0");

        // Validate the u64 little endian encoding of nonce
        byte[] le = new byte[8];
        System.arraycopy(bytes, n - 8, le, 0, 8);
        ByteBuffer bb = ByteBuffer.wrap(le).order(ByteOrder.LITTLE_ENDIAN);
        long decoded = bb.getLong();
        assertEquals(nonce, decoded, "replay nonce should round-trip in little-endian");
    }
}
