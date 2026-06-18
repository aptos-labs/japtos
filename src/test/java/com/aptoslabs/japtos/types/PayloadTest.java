package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AccountAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the various {@link TransactionPayload} / executable implementations.
 */
class PayloadTest {

    private EntryFunctionPayload transferPayload() {
        ModuleId moduleId = new ModuleId(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("coin"));
        return new EntryFunctionPayload(
                moduleId,
                new Identifier("transfer"),
                List.of(new TypeTag.Struct(new StructTag(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"),
                        new Identifier("aptos_coin"), new Identifier("AptosCoin"), List.of()))),
                List.of(new TransactionArgument.AccountAddress(AccountAddress.zero()),
                        new TransactionArgument.U64(1000L)));
    }

    @Test
    @DisplayName("EntryFunctionPayload starts with variant 0x02 and exposes its parts")
    void entryFunctionPayload() throws IOException {
        EntryFunctionPayload payload = transferPayload();
        byte[] bytes = payload.bcsToBytes();
        assertEquals(0x02, bytes[0]);
        assertEquals("coin", payload.getModuleId().getName().getValue());
        assertEquals("transfer", payload.getFunctionName().getValue());
        assertEquals(1, payload.getTypeArguments().size());
        assertEquals(2, payload.getArguments().size());
        assertTrue(payload.toString().contains("EntryFunctionPayload"));
    }

    @Test
    @DisplayName("EntryFunctionPayload factory and varargs factory produce identical bytes")
    void entryFunctionFactories() {
        ModuleId moduleId = new ModuleId(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("coin"));
        Identifier fn = new Identifier("transfer");
        TransactionArgument arg = new TransactionArgument.U64(5L);

        EntryFunctionPayload viaList = EntryFunctionPayload.create(moduleId, fn, List.of(), List.of(arg));
        EntryFunctionPayload viaVarargs = EntryFunctionPayload.create(moduleId, fn, List.of(), arg);
        assertArrayEquals(viaList.bcsToBytes(), viaVarargs.bcsToBytes());
    }

    @Test
    @DisplayName("ScriptPayload starts with variant 0x00 and round-trips its accessors")
    void scriptPayload() throws IOException {
        byte[] code = {(byte) 0xa1, 0x1c, (byte) 0xeb, 0x0b};
        ScriptPayload script = new ScriptPayload(code,
                List.of(new TransactionArgument.U64(1L)),
                List.of(new TypeTag.U8()));
        assertArrayEquals(code, script.getCode());
        assertEquals(1, script.getArguments().size());
        assertEquals(1, script.getTypeArguments().size());

        Serializer s = new Serializer();
        script.serialize(s);
        assertEquals(0x00, s.toByteArray()[0]);
    }

    @Test
    @DisplayName("TransactionExecutableEntryFunction (raw parts) starts with variant 0x01")
    void executableEntryFunctionRawParts() throws IOException {
        TransactionExecutableEntryFunction exec = new TransactionExecutableEntryFunction(
                new ModuleId(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("coin")),
                new Identifier("transfer"),
                List.of(),
                List.of(new TransactionArgument.U64(1L)));
        Serializer s = new Serializer();
        exec.serialize(s);
        assertEquals(0x01, s.toByteArray()[0]);
    }

    @Test
    @DisplayName("TransactionExecutableEntryFunction can reuse an EntryFunctionPayload body")
    void executableEntryFunctionFromPayload() throws IOException {
        TransactionExecutableEntryFunction fromRaw = new TransactionExecutableEntryFunction(
                transferPayload().getModuleId(),
                transferPayload().getFunctionName(),
                transferPayload().getTypeArguments(),
                transferPayload().getArguments());
        TransactionExecutableEntryFunction fromPayload =
                TransactionExecutableEntryFunction.fromEntryFunctionPayload(transferPayload());

        Serializer a = new Serializer();
        fromRaw.serialize(a);
        Serializer b = new Serializer();
        fromPayload.serialize(b);
        // Reusing the payload drops its 0x02 variant byte and re-tags with executable variant 0x01,
        // producing identical bytes to the raw-parts path.
        assertArrayEquals(a.toByteArray(), b.toByteArray());
    }

    @Test
    @DisplayName("TransactionInnerPayloadV1 emits payload variant 4 then inner variant 0")
    void innerPayloadV1() throws IOException {
        TransactionExecutableEntryFunction exec = new TransactionExecutableEntryFunction(
                new ModuleId(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("coin")),
                new Identifier("transfer"),
                List.of(),
                List.of());
        TransactionExtraConfigV1 config = new TransactionExtraConfigV1(null, null);
        TransactionInnerPayloadV1 payload = new TransactionInnerPayloadV1(exec, config);

        byte[] bytes = payload.bcsToBytes();
        assertEquals(0x04, bytes[0]); // payload variant
        assertEquals(0x00, bytes[1]); // inner payload variant V1
    }

    @Test
    @DisplayName("TransactionExtraConfigV1 encodes optional fields as BCS Options")
    void extraConfigV1() throws IOException {
        // Both empty -> variant(0) + false + false
        TransactionExtraConfigV1 empty = new TransactionExtraConfigV1(null, null);
        assertArrayEquals(new byte[]{0x00, 0x00, 0x00}, empty.bcsToBytes());

        // With multisig address and nonce -> variant + true + 32 bytes + true + 8 bytes
        TransactionExtraConfigV1 full =
                new TransactionExtraConfigV1(AccountAddress.zero(), 42L);
        byte[] bytes = full.bcsToBytes();
        assertEquals(0x00, bytes[0]);
        assertEquals(0x01, bytes[1]); // Some(address)
        // 1 (variant) + 1 (some) + 32 (addr) + 1 (some) + 8 (u64) = 43
        assertEquals(43, bytes.length);
    }
}
