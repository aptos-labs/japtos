package com.aptoslabs.japtos.core.crypto;

import com.aptoslabs.japtos.bcs.Deserializer;
import com.aptoslabs.japtos.bcs.Serializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AnyPublicKey}, {@link KeylessPublicKey},
 * {@link MultiEd25519PublicKey} and {@link MultiEd25519Signature}.
 */
class CryptoExtraTest {

    @Test
    @DisplayName("AnyPublicKey wraps an Ed25519 key with the Ed25519 variant and delegates")
    void anyPublicKeyEd25519() throws IOException {
        Ed25519PublicKey ed = Ed25519PrivateKey.generate().publicKey();
        AnyPublicKey any = new AnyPublicKey(ed);

        assertEquals(AnyPublicKey.VARIANT_ED25519, any.getVariant());
        assertSame(ed, any.getPublicKey());
        assertArrayEquals(ed.toBytes(), any.toBytes());
        assertEquals(ed.toHexString(), any.toHexString());
        assertEquals(ed.toString(), any.toString());
        assertEquals(ed.authKey(), any.authKey());
        assertEquals(ed.accountAddress(), any.accountAddress());

        Serializer s = new Serializer();
        any.serialize(s);
        byte[] serialized = s.toByteArray();
        // variant (1 byte uleb) + length prefix (0x20) + 32 byte key = 34
        assertEquals(34, serialized.length);
        assertEquals(AnyPublicKey.VARIANT_ED25519, serialized[0]);
        assertEquals(0x20, serialized[1]); // length prefix from Ed25519PublicKey.serialize

        // Ed25519PublicKey.deserialize reads a fixed (non-prefixed) 32-byte key, so build the
        // matching fixed encoding: variant byte followed by the raw 32 key bytes.
        Serializer fixed = new Serializer();
        fixed.serializeU32AsUleb128(AnyPublicKey.VARIANT_ED25519);
        fixed.serializeFixedBytes(ed.toBytes());
        AnyPublicKey restored = AnyPublicKey.deserialize(new Deserializer(fixed.toByteArray()));
        assertEquals(AnyPublicKey.VARIANT_ED25519, restored.getVariant());
        assertArrayEquals(ed.toBytes(), restored.toBytes());
    }

    @Test
    @DisplayName("AnyPublicKey verifies signatures by delegating to its inner key")
    void anyPublicKeyVerifies() {
        Ed25519PrivateKey priv = Ed25519PrivateKey.generate();
        AnyPublicKey any = new AnyPublicKey(priv.publicKey());
        byte[] msg = "verify".getBytes();
        assertTrue(any.verifySignature(msg, priv.sign(msg)));
    }

    @Test
    @DisplayName("AnyPublicKey rejects unsupported key types and unknown variants")
    void anyPublicKeyErrors() {
        // An anonymous unsupported PublicKey implementation
        PublicKey unsupported = new PublicKey() {
            public byte[] toBytes() { return new byte[0]; }
            public String toHexString() { return ""; }
            public com.aptoslabs.japtos.core.AuthenticationKey authKey() { return null; }
            public boolean verifySignature(byte[] message, Signature signature) { return false; }
        };
        assertThrows(IllegalArgumentException.class, () -> new AnyPublicKey(unsupported));

        // Unknown variant index during deserialization
        Deserializer d = new Deserializer(new byte[]{(byte) 0x09});
        assertThrows(IOException.class, () -> AnyPublicKey.deserialize(d));
    }

    @Test
    @DisplayName("KeylessPublicKey serialize/deserialize round-trips and derives an address")
    void keylessPublicKeyRoundTrip() throws Exception {
        byte[] idCommitment = new byte[KeylessPublicKey.ID_COMMITMENT_LENGTH];
        for (int i = 0; i < idCommitment.length; i++) idCommitment[i] = (byte) (i + 1);
        String iss = "https://accounts.google.com";
        KeylessPublicKey key = new KeylessPublicKey(iss, idCommitment);

        assertEquals(iss, key.getIss());
        assertArrayEquals(idCommitment, key.getIdCommitment());
        assertEquals("0x" + key.toHexString(), key.toString());
        assertNotNull(key.authKey());
        assertEquals(key.authKey().accountAddress(), key.accountAddress());

        // Round-trip via hex string
        KeylessPublicKey restored = KeylessPublicKey.fromHexString(key.toString());
        assertEquals(key.getIss(), restored.getIss());
        assertArrayEquals(key.getIdCommitment(), restored.getIdCommitment());

        // Round-trip via deserializer directly
        Serializer s = new Serializer();
        key.serialize(s);
        KeylessPublicKey restored2 = KeylessPublicKey.deserialize(new Deserializer(s.toByteArray()));
        assertEquals(key.getIss(), restored2.getIss());
    }

    @Test
    @DisplayName("KeylessPublicKey enforces the id-commitment length and rejects ZK verification")
    void keylessPublicKeyConstraints() {
        assertThrows(IllegalArgumentException.class,
                () -> new KeylessPublicKey("iss", new byte[16]));

        KeylessPublicKey key = new KeylessPublicKey("iss", new byte[32]);
        assertThrows(UnsupportedOperationException.class,
                () -> key.verifySignature("m".getBytes(), Signature.fromBytes(new byte[64])));
    }

    @Test
    @DisplayName("AnyPublicKey wraps Keyless keys with the keyless variant")
    void anyPublicKeyKeyless() throws IOException {
        KeylessPublicKey key = new KeylessPublicKey("iss", new byte[32]);
        AnyPublicKey any = new AnyPublicKey(key);
        assertEquals(AnyPublicKey.VARIANT_KEYLESS, any.getVariant());

        Serializer s = new Serializer();
        any.serialize(s);
        AnyPublicKey restored = AnyPublicKey.deserialize(new Deserializer(s.toByteArray()));
        assertEquals(AnyPublicKey.VARIANT_KEYLESS, restored.getVariant());
    }

    @Test
    @DisplayName("MultiEd25519PublicKey serializes vector<key> + threshold and validates inputs")
    void multiEd25519PublicKey() throws IOException {
        Ed25519PublicKey k1 = Ed25519PrivateKey.generate().publicKey();
        Ed25519PublicKey k2 = Ed25519PrivateKey.generate().publicKey();
        MultiEd25519PublicKey multi = new MultiEd25519PublicKey(List.of(k1, k2), 2);

        assertEquals(2, multi.getThreshold());
        assertEquals(2, multi.getPublicKeys().size());
        assertTrue(multi.toString().contains("threshold=2"));

        // 1 (count) + 2*(1 len + 32) + 1 (threshold) = 68
        assertEquals(68, multi.toBytes().length);
        assertEquals(multi.toBytes().length * 2, multi.toHexString().length());
        // authKey derives from first key
        assertEquals(k1.authKey(), multi.authKey());

        assertThrows(IllegalArgumentException.class, () -> new MultiEd25519PublicKey(List.of(), 1));
        assertThrows(IllegalArgumentException.class, () -> new MultiEd25519PublicKey(List.of(k1), 0));
        assertThrows(IllegalArgumentException.class, () -> new MultiEd25519PublicKey(List.of(k1), 2));
    }

    @Test
    @DisplayName("MultiEd25519PublicKey verifies against its first key")
    void multiEd25519PublicKeyVerify() {
        Ed25519PrivateKey priv = Ed25519PrivateKey.generate();
        MultiEd25519PublicKey multi = new MultiEd25519PublicKey(List.of(priv.publicKey()), 1);
        byte[] msg = "m".getBytes();
        assertTrue(multi.verifySignature(msg, priv.sign(msg)));
    }

    @Test
    @DisplayName("MultiEd25519Signature createBitmap sets MSB-first bits and serializes")
    void multiEd25519Signature() throws IOException {
        // bits 0, 2, 5 -> 0b10100100 = 0xA4 in first byte
        byte[] bitmap = MultiEd25519Signature.createBitmap(List.of(0, 2, 5));
        assertEquals(4, bitmap.length);
        assertEquals((byte) 0xA4, bitmap[0]);

        Signature sig1 = Signature.fromBytes(new byte[64]);
        Signature sig2 = Signature.fromBytes(new byte[64]);
        MultiEd25519Signature multiSig = new MultiEd25519Signature(List.of(sig1, sig2), bitmap);

        assertEquals(2, multiSig.getSignatureCount());
        assertSame(sig1, multiSig.getSignature(0));
        assertArrayEquals(bitmap, multiSig.getBitmap());
        assertEquals(2, multiSig.getSignatures().size());

        // serialize: 1 (count) + 2*64 + 4 (bitmap) = 133
        assertEquals(133, multiSig.toBytes().length);
        assertTrue(multiSig.toString().contains("signatures=2"));
    }

    @Test
    @DisplayName("MultiEd25519Signature rejects bad bitmap length and out-of-range bits")
    void multiEd25519SignatureValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> new MultiEd25519Signature(List.of(), new byte[3]));
        assertThrows(IllegalArgumentException.class,
                () -> MultiEd25519Signature.createBitmap(List.of(32)));
        assertThrows(IllegalArgumentException.class,
                () -> MultiEd25519Signature.createBitmap(List.of(-1)));
    }
}
