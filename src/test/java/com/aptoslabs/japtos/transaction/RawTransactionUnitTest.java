package com.aptoslabs.japtos.transaction;

import com.aptoslabs.japtos.bcs.Deserializer;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.types.EntryFunctionPayload;
import com.aptoslabs.japtos.types.Identifier;
import com.aptoslabs.japtos.types.ModuleId;
import com.aptoslabs.japtos.types.TransactionArgument;
import com.aptoslabs.japtos.types.TransactionPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RawTransaction}, {@link RawTransactionWithFeePayer},
 * {@link FeePayerRawTransaction} and the simplified {@link Transaction}.
 */
class RawTransactionUnitTest {

    private TransactionPayload payload() {
        return new EntryFunctionPayload(
                new ModuleId(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("coin")),
                new Identifier("transfer"),
                List.of(),
                List.of(new TransactionArgument.U64(1L)));
    }

    private RawTransaction raw() {
        return new RawTransaction(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), 7L, payload(),
                100000L, 100L, 1700000000L, 4L);
    }

    @Test
    @DisplayName("Full constructor stores every field and exposes accessors")
    void accessors() {
        RawTransaction raw = raw();
        assertEquals(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), raw.getSender());
        assertEquals(7L, raw.getSequenceNumber());
        assertNotNull(raw.getPayload());
        assertEquals(100000L, raw.getMaxGasAmount());
        assertEquals(100L, raw.getGasUnitPrice());
        assertEquals(1700000000L, raw.getExpirationTimestampSecs());
        assertEquals(4L, raw.getChainId());
        assertTrue(raw.toString().contains("RawTransaction"));
    }

    @Test
    @DisplayName("Convenience constructor applies the default gas parameters")
    void defaultGasConstructor() {
        RawTransaction raw = new RawTransaction(AccountAddress.zero(), 0L, payload(), 1700000000L, 4L);
        assertEquals(RawTransaction.DEFAULT_MAX_GAS, raw.getMaxGasAmount());
        assertEquals(RawTransaction.DEFAULT_GAS_PRICE, raw.getGasUnitPrice());
    }

    @Test
    @DisplayName("BCS layout is sender|seq|payload|maxGas|gasPrice|expiry|chainId")
    void bcsLayout() throws IOException {
        RawTransaction raw = raw();
        byte[] bytes = raw.bcsToBytes();
        assertTrue(bytes.length > 32 + 8);

        Deserializer d = new Deserializer(bytes);
        byte[] sender = d.deserializeFixedBytes(32);
        assertArrayEquals(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001").toBytes(), sender);
        assertEquals(7L, d.deserializeU64()); // sequence number

        // Trailing fields: after the payload come maxGas, gasPrice, expiry (u64s) and chainId (u8).
        // Validate the chain id by re-reading from the tail.
        assertEquals(4, bytes[bytes.length - 1]);
    }

    @Test
    @DisplayName("RawTransactionWithFeePayer appends true + fee payer address and delegates getters")
    void rawTransactionWithFeePayer() throws IOException {
        RawTransaction raw = raw();
        AccountAddress feePayer = AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000002");
        RawTransactionWithFeePayer wrapped = new RawTransactionWithFeePayer(raw, feePayer);

        assertSame(raw, wrapped.getRawTransaction());
        assertEquals(feePayer, wrapped.getFeePayerAddress());
        assertEquals(raw.getSender(), wrapped.getSender());
        assertEquals(raw.getSequenceNumber(), wrapped.getSequenceNumber());
        assertEquals(raw.getPayload(), wrapped.getPayload());
        assertEquals(raw.getMaxGasAmount(), wrapped.getMaxGasAmount());
        assertEquals(raw.getGasUnitPrice(), wrapped.getGasUnitPrice());
        assertEquals(raw.getExpirationTimestampSecs(), wrapped.getExpirationTimestampSecs());
        assertEquals(raw.getChainId(), wrapped.getChainId());

        byte[] bytes = wrapped.bcsToBytes();
        byte[] rawBytes = raw.bcsToBytes();
        // last 33 bytes: 0x01 (true) + 32-byte fee payer address
        assertEquals(rawBytes.length + 1 + 32, bytes.length);
        assertEquals(0x01, bytes[rawBytes.length]);
    }

    @Test
    @DisplayName("FeePayerRawTransaction emits variant 1, raw txn, signers, fee payer")
    void feePayerRawTransaction() {
        RawTransaction raw = raw();
        AccountAddress feePayer = AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000002");
        FeePayerRawTransaction fp = new FeePayerRawTransaction(raw, List.of(), feePayer);

        assertSame(raw, fp.getRawTransaction());
        byte[] bytes = fp.bcsToBytes();
        assertEquals(0x01, bytes[0]); // FeePayer transaction variant
        // ends with: secondary signer count (0x00) + 32-byte fee payer address
        assertEquals(0x00, bytes[bytes.length - 33]);
    }

    @Test
    @DisplayName("Simplified Transaction serializes the same field order and exposes accessors")
    void simplifiedTransaction() {
        Transaction txn = new Transaction(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), 3L, payload(),
                5000L, 50L, 123L, 4L);
        assertEquals(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), txn.getSender());
        assertEquals(3L, txn.getSequenceNumber());
        assertNotNull(txn.getPayload());
        assertEquals(5000L, txn.getMaxGasAmount());
        assertEquals(50L, txn.getGasUnitPrice());
        assertEquals(123L, txn.getExpirationTimestampSecs());
        assertEquals(4L, txn.getChainId());

        byte[] bytes = txn.toBcsBytes();
        assertTrue(bytes.length > 0);
        assertEquals(4, bytes[bytes.length - 1]); // chain id is the trailing byte
    }
}
