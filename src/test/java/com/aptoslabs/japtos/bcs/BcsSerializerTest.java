package com.aptoslabs.japtos.bcs;

import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.core.crypto.Ed25519PrivateKey;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.core.crypto.Signature;
import com.aptoslabs.japtos.utils.HexUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the BCS {@link Serializer} and {@link Deserializer}.
 *
 * <p>These tests assert the exact byte layout mandated by the BCS specification
 * (little-endian integers, ULEB128 lengths) and verify round-trip behaviour.</p>
 */
class BcsSerializerTest {

    @Test
    @DisplayName("Booleans encode to a single 0x00 / 0x01 byte")
    void serializeBool() throws IOException {
        Serializer t = new Serializer();
        t.serializeBool(true);
        Serializer f = new Serializer();
        f.serializeBool(false);

        assertArrayEquals(new byte[]{0x01}, t.toByteArray());
        assertArrayEquals(new byte[]{0x00}, f.toByteArray());
    }

    @Test
    @DisplayName("Multi-byte integers are little-endian")
    void integersAreLittleEndian() throws IOException {
        Serializer s = new Serializer();
        s.serializeU16((short) 0x0102);
        s.serializeU32(0x01020304);
        s.serializeU64(0x0102030405060708L);

        byte[] out = s.toByteArray();
        assertArrayEquals(new byte[]{0x02, 0x01}, Arrays.copyOfRange(out, 0, 2));
        assertArrayEquals(new byte[]{0x04, 0x03, 0x02, 0x01}, Arrays.copyOfRange(out, 2, 6));
        assertArrayEquals(new byte[]{0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01},
                Arrays.copyOfRange(out, 6, 14));
    }

    @Test
    @DisplayName("u8 masks to the low byte")
    void serializeU8() throws IOException {
        Serializer s = new Serializer();
        s.serializeU8((byte) 0xFF);
        assertArrayEquals(new byte[]{(byte) 0xFF}, s.toByteArray());
    }

    @Test
    @DisplayName("ULEB128 encodes multi-byte continuation correctly")
    void uleb128Encoding() throws IOException {
        // 0 -> 0x00, 127 -> 0x7f, 128 -> 0x80 0x01, 300 -> 0xac 0x02
        assertArrayEquals(new byte[]{0x00}, ulebBytes(0));
        assertArrayEquals(new byte[]{0x7f}, ulebBytes(127));
        assertArrayEquals(new byte[]{(byte) 0x80, 0x01}, ulebBytes(128));
        assertArrayEquals(new byte[]{(byte) 0xac, 0x02}, ulebBytes(300));
    }

    private byte[] ulebBytes(int value) throws IOException {
        Serializer s = new Serializer();
        s.serializeU32AsUleb128(value);
        return s.toByteArray();
    }

    @Test
    @DisplayName("serializeBytes prepends a ULEB128 length")
    void serializeBytesPrependsLength() throws IOException {
        Serializer s = new Serializer();
        s.serializeBytes(new byte[]{1, 2, 3});
        assertArrayEquals(new byte[]{0x03, 1, 2, 3}, s.toByteArray());
    }

    @Test
    @DisplayName("serializeFixedBytes writes raw bytes with no length")
    void serializeFixedBytes() throws IOException {
        Serializer s = new Serializer();
        s.serializeFixedBytes(new byte[]{9, 8, 7});
        assertArrayEquals(new byte[]{9, 8, 7}, s.toByteArray());
    }

    @Test
    @DisplayName("serializeFixedBytesExact validates length")
    void serializeFixedBytesExact() throws IOException {
        Serializer s = new Serializer();
        s.serializeFixedBytesExact(new byte[]{1, 2}, 2);
        assertArrayEquals(new byte[]{1, 2}, s.toByteArray());

        assertThrows(IllegalArgumentException.class,
                () -> new Serializer().serializeFixedBytesExact(new byte[]{1, 2}, 3));
    }

    @Test
    @DisplayName("Strings serialize as UTF-8 length-prefixed bytes")
    void serializeString() throws IOException {
        Serializer s = new Serializer();
        s.serializeString("abc");
        assertArrayEquals(new byte[]{0x03, 'a', 'b', 'c'}, s.toByteArray());
    }

    @Test
    @DisplayName("u128 / u256 require exact byte lengths")
    void serializeU128AndU256() throws IOException {
        Serializer ok = new Serializer();
        ok.serializeU128(new byte[16]);
        ok.serializeU256(new byte[32]);
        assertEquals(48, ok.size());

        assertThrows(IllegalArgumentException.class, () -> new Serializer().serializeU128(new byte[15]));
        assertThrows(IllegalArgumentException.class, () -> new Serializer().serializeU256(new byte[31]));
    }

    @Test
    @DisplayName("Account address serializes as 32 raw bytes")
    void serializeAccountAddress() throws IOException {
        AccountAddress addr = AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001");
        Serializer s = new Serializer();
        s.serializeAccountAddress(addr);
        assertArrayEquals(addr.toBytes(), s.toByteArray());
        assertEquals(32, s.size());
    }

    @Test
    @DisplayName("Public keys and signatures serialize as length-prefixed vectors")
    void serializePublicKeyAndSignature() throws IOException {
        Ed25519PrivateKey priv = Ed25519PrivateKey.generate();
        Ed25519PublicKey pub = priv.publicKey();
        Signature sig = priv.sign("hi".getBytes());

        Serializer pk = new Serializer();
        pk.serializePublicKey(pub);
        assertEquals(33, pk.toByteArray().length); // 1 length byte + 32

        Serializer ss = new Serializer();
        ss.serializeSignature(sig);
        assertEquals(65, ss.toByteArray().length); // 1 length byte + 64
    }

    @Test
    @DisplayName("serializeSequence writes length then each element")
    void serializeSequence() throws IOException {
        Serializer s = new Serializer();
        s.serializeSequence(List.of(
                new com.aptoslabs.japtos.types.Identifier("a"),
                new com.aptoslabs.japtos.types.Identifier("bb")
        ));
        // 0x02 count, then ("a")=0x01 'a', then ("bb")=0x02 'b' 'b'
        assertArrayEquals(new byte[]{0x02, 0x01, 'a', 0x02, 'b', 'b'}, s.toByteArray());
    }

    @Test
    @DisplayName("serializeSequenceBytes writes count then each length-prefixed array")
    void serializeSequenceBytes() throws IOException {
        Serializer s = new Serializer();
        s.serializeSequenceBytes(List.of(new byte[]{1}, new byte[]{2, 3}));
        assertArrayEquals(new byte[]{0x02, 0x01, 1, 0x02, 2, 3}, s.toByteArray());
    }

    @Test
    @DisplayName("writeBytesDirect bypasses length prefix; deprecated ctor still buffers")
    void writeBytesDirectAndDeprecatedConstructor() throws IOException {
        Serializer s = new Serializer(new java.io.ByteArrayOutputStream());
        s.writeBytesDirect(new byte[]{5, 6});
        assertArrayEquals(new byte[]{5, 6}, s.toByteArray());
    }

    @Test
    @DisplayName("Deserializer reverses every primitive serializer")
    void deserializerRoundTrip() throws IOException {
        Serializer s = new Serializer();
        s.serializeBool(true);
        s.serializeU8((byte) 200);
        s.serializeU16((short) 40000);
        s.serializeU32(123456789);
        s.serializeU64(9876543210L);
        s.serializeU32AsUleb128(300);
        s.serializeBytes(new byte[]{4, 5, 6});
        s.serializeString("hello");

        Deserializer d = new Deserializer(s.toByteArray());
        assertTrue(d.deserializeBool());
        assertEquals((byte) 200, d.deserializeU8());
        assertEquals((short) 40000, d.deserializeU16());
        assertEquals(123456789, d.deserializeU32());
        assertEquals(9876543210L, d.deserializeU64());
        assertEquals(300, d.deserializeUleb128AsU32());
        assertArrayEquals(new byte[]{4, 5, 6}, d.deserializeBytes());
        assertEquals("hello", d.deserializeString());
        assertEquals(0, d.remaining());
    }

    @Test
    @DisplayName("Deserializer reports remaining bytes and signals truncation")
    void deserializerRemainingAndErrors() throws IOException {
        Deserializer d = new Deserializer(new byte[]{0x01, 0x02});
        assertEquals(2, d.remaining());
        assertEquals((byte) 1, d.deserializeU8());
        assertEquals(1, d.remaining());

        // Reading more bytes than available throws
        Deserializer empty = new Deserializer(new byte[0]);
        assertThrows(IOException.class, empty::deserializeBool);

        Deserializer shortBuf = new Deserializer(new byte[]{0x05});
        assertThrows(IOException.class, () -> shortBuf.deserializeFixedBytes(4));
    }

    @Test
    @DisplayName("ULEB128 deserialization rejects oversized values")
    void uleb128Overflow() {
        // Five 0x80 continuation bytes shifts beyond 31 bits
        byte[] tooBig = {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0x01};
        Deserializer d = new Deserializer(tooBig);
        assertThrows(IOException.class, d::deserializeUleb128AsU32);
    }

    @Test
    @DisplayName("Serializable default bcsToHex/bcsToString wrap bcsToBytes")
    void serializableDefaults() {
        com.aptoslabs.japtos.types.Identifier id = new com.aptoslabs.japtos.types.Identifier("a");
        // serialize -> 0x01 (len) + 0x61 ('a')
        assertArrayEquals(new byte[]{0x01, 0x61}, id.bcsToBytes());
        assertEquals("0161", id.bcsToHex());
        assertEquals("0x0161", id.bcsToString());
    }

    @Test
    @DisplayName("bytesToHex on serialized output matches HexUtils")
    void hexConsistency() throws IOException {
        Serializer s = new Serializer();
        s.serializeU32(0xdeadbeef);
        assertEquals("efbeadde", HexUtils.bytesToHex(s.toByteArray()));
    }
}
