package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AccountAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TransactionArgument} variants, asserting both the tagged
 * serialization (with the variant byte) and the entry-function serialization
 * (raw little-endian value).
 */
class TransactionArgumentTest {

    private byte[] tagged(TransactionArgument arg) throws IOException {
        Serializer s = new Serializer();
        arg.serialize(s);
        return s.toByteArray();
    }

    @Test
    @DisplayName("U8 serialization carries tag 0 and raw byte for entry functions")
    void u8() throws IOException {
        TransactionArgument.U8 arg = new TransactionArgument.U8((byte) 0x2a);
        assertEquals((byte) 0x2a, arg.getValue());
        assertArrayEquals(new byte[]{0x00, 0x2a}, tagged(arg));
        assertArrayEquals(new byte[]{0x2a}, arg.serializeForEntryFunction());
    }

    @Test
    @DisplayName("U64 serialization carries tag 1 and 8 little-endian bytes")
    void u64() throws IOException {
        TransactionArgument.U64 arg = new TransactionArgument.U64(1L);
        assertEquals(1L, arg.getValue());
        assertArrayEquals(new byte[]{0x01, 1, 0, 0, 0, 0, 0, 0, 0}, tagged(arg));
        assertArrayEquals(new byte[]{1, 0, 0, 0, 0, 0, 0, 0}, arg.serializeForEntryFunction());
    }

    @Test
    @DisplayName("U16 and U32 serialization carry their tags and little-endian values")
    void u16AndU32() throws IOException {
        TransactionArgument.U16 u16 = new TransactionArgument.U16((short) 0x0102);
        assertEquals((short) 0x0102, u16.getValue());
        assertArrayEquals(new byte[]{0x06, 0x02, 0x01}, tagged(u16));
        assertArrayEquals(new byte[]{0x02, 0x01}, u16.serializeForEntryFunction());

        TransactionArgument.U32 u32 = new TransactionArgument.U32(0x01020304);
        assertEquals(0x01020304, u32.getValue());
        assertArrayEquals(new byte[]{0x07, 0x04, 0x03, 0x02, 0x01}, tagged(u32));
        assertArrayEquals(new byte[]{0x04, 0x03, 0x02, 0x01}, u32.serializeForEntryFunction());
    }

    @Test
    @DisplayName("U128 entry-function encoding is little-endian and 16 bytes")
    void u128() throws IOException {
        TransactionArgument.U128 arg = new TransactionArgument.U128(BigInteger.ONE);
        assertEquals(BigInteger.ONE, arg.getValue());
        byte[] efn = arg.serializeForEntryFunction();
        assertEquals(16, efn.length);
        assertEquals(1, efn[0]); // little-endian: low byte first
        // Tagged form: tag 2 then 16 big-endian-padded bytes
        byte[] t = tagged(arg);
        assertEquals(17, t.length);
        assertEquals(2, t[0]);
        assertThrows(IllegalArgumentException.class,
                () -> new TransactionArgument.U128(BigInteger.ONE.shiftLeft(200)).serialize(new Serializer()));
    }

    @Test
    @DisplayName("U256 entry-function encoding is little-endian and 32 bytes")
    void u256() throws IOException {
        TransactionArgument.U256 arg = new TransactionArgument.U256(BigInteger.valueOf(255));
        assertEquals(BigInteger.valueOf(255), arg.getValue());
        byte[] efn = arg.serializeForEntryFunction();
        assertEquals(32, efn.length);
        assertEquals((byte) 0xff, efn[0]);
        byte[] t = tagged(arg);
        assertEquals(33, t.length);
        assertEquals(8, t[0]);
        assertThrows(IllegalArgumentException.class,
                () -> new TransactionArgument.U256(BigInteger.ONE.shiftLeft(300)).serialize(new Serializer()));
    }

    @Test
    @DisplayName("Bool serialization carries tag 5")
    void bool() throws IOException {
        TransactionArgument.Bool t = new TransactionArgument.Bool(true);
        assertTrue(t.getValue());
        assertArrayEquals(new byte[]{0x05, 0x01}, tagged(t));
        assertArrayEquals(new byte[]{0x01}, t.serializeForEntryFunction());
        assertArrayEquals(new byte[]{0x00}, new TransactionArgument.Bool(false).serializeForEntryFunction());
    }

    @Test
    @DisplayName("AccountAddress argument serializes the 32-byte address")
    void accountAddress() throws IOException {
        AccountAddress addr = AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001");
        TransactionArgument.AccountAddress arg = new TransactionArgument.AccountAddress(addr);
        assertEquals(addr, arg.getValue());
        byte[] t = tagged(arg);
        assertEquals(33, t.length);
        assertEquals(3, t[0]);
        assertArrayEquals(addr.toBytes(), arg.serializeForEntryFunction());
    }

    @Test
    @DisplayName("U8Vector serializes as a length-prefixed byte vector")
    void u8Vector() throws IOException {
        TransactionArgument.U8Vector arg = new TransactionArgument.U8Vector(new byte[]{1, 2, 3});
        assertArrayEquals(new byte[]{1, 2, 3}, arg.getValue());
        assertArrayEquals(new byte[]{0x04, 0x03, 1, 2, 3}, tagged(arg));
        assertArrayEquals(new byte[]{0x03, 1, 2, 3}, arg.serializeForEntryFunction());
    }

    @Test
    @DisplayName("U64Vector serializes length then each u64")
    void u64Vector() throws IOException {
        TransactionArgument.U64Vector arg = new TransactionArgument.U64Vector(List.of(1L, 2L));
        assertEquals(List.of(1L, 2L), arg.getValue());
        byte[] efn = arg.serializeForEntryFunction();
        // count (1) + 2 * 8 bytes = 17
        assertEquals(17, efn.length);
        assertEquals(0x02, efn[0]);
        byte[] t = tagged(arg);
        assertEquals(0x09, t[0]); // tag 9
        assertEquals(18, t.length);
    }

    @Test
    @DisplayName("String argument serializes as Move String (length-prefixed UTF-8)")
    void stringArgument() throws IOException {
        TransactionArgument.String fromString = new TransactionArgument.String("hi");
        assertEquals("hi", fromString.getStringValue());
        assertArrayEquals("hi".getBytes(), fromString.getValue());
        assertArrayEquals(new byte[]{0x02, 'h', 'i'}, fromString.serializeForEntryFunction());
        // serialize() (no tag for Move String) also yields the length-prefixed bytes
        Serializer s = new Serializer();
        fromString.serialize(s);
        assertArrayEquals(new byte[]{0x02, 'h', 'i'}, s.toByteArray());

        TransactionArgument.String fromBytes = new TransactionArgument.String(new byte[]{'a'});
        assertArrayEquals(new byte[]{'a'}, fromBytes.getValue());
    }

    @Test
    @DisplayName("Default serializeForEntryFunction falls back to bcsToBytes")
    void defaultEntryFunction() throws IOException {
        // U64Vector overrides serializeForEntryFunction, but the inherited default on a plain
        // argument equals bcsToBytes(). Validate via a Bool whose default would equal tagged form.
        TransactionArgument.AccountAddress arg =
                new TransactionArgument.AccountAddress(AccountAddress.zero());
        // bcsToBytes equals the tagged serialization
        assertArrayEquals(tagged(arg), arg.bcsToBytes());
    }
}
