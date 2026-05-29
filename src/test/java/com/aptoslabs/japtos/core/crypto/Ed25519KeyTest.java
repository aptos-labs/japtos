package com.aptoslabs.japtos.core.crypto;

import com.aptoslabs.japtos.bcs.Deserializer;
import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.utils.HexUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Ed25519 keys and signatures using deterministic key material so the
 * tests assert concrete cryptographic properties rather than mere absence of exceptions.
 */
class Ed25519KeyTest {

    // A fixed 32-byte private key seed.
    private static final String PRIV_HEX =
            "9bf49a6a0755f953811fce125f2683d50429c3bb49e074147e0089a52eae155f";

    @Test
    @DisplayName("publicKey() is deterministic for a fixed private key")
    void deterministicPublicKey() {
        Ed25519PrivateKey priv = Ed25519PrivateKey.fromHex(PRIV_HEX);
        Ed25519PublicKey pub1 = priv.publicKey();
        Ed25519PublicKey pub2 = Ed25519PrivateKey.fromHex("0x" + PRIV_HEX).publicKey();
        assertEquals(pub1, pub2);
        assertEquals(32, pub1.toBytes().length);
    }

    @Test
    @DisplayName("A signature verifies against its message and fails for a different one")
    void signAndVerify() {
        Ed25519PrivateKey priv = Ed25519PrivateKey.fromHex(PRIV_HEX);
        Ed25519PublicKey pub = priv.publicKey();

        byte[] msg = "the quick brown fox".getBytes();
        Signature sig = priv.sign(msg);
        assertEquals(64, sig.toBytes().length);

        assertTrue(pub.verifySignature(msg, sig));
        assertFalse(pub.verifySignature("a different message".getBytes(), sig));
    }

    @Test
    @DisplayName("Signing is deterministic (Ed25519) for the same key and message")
    void deterministicSignature() {
        Ed25519PrivateKey priv = Ed25519PrivateKey.fromHex(PRIV_HEX);
        byte[] msg = "deterministic".getBytes();
        assertEquals(priv.sign(msg), priv.sign(msg));
    }

    @Test
    @DisplayName("generate() produces a fresh, valid keypair each time")
    void generateProducesUniqueKeys() {
        Ed25519PrivateKey a = Ed25519PrivateKey.generate();
        Ed25519PrivateKey b = Ed25519PrivateKey.generate();
        assertNotEquals(a, b);
        byte[] msg = "x".getBytes();
        assertTrue(a.publicKey().verifySignature(msg, a.sign(msg)));
    }

    @Test
    @DisplayName("Private key conversions and length validation")
    void privateKeyConversions() {
        Ed25519PrivateKey priv = Ed25519PrivateKey.fromHex(PRIV_HEX);
        assertEquals(PRIV_HEX, priv.toHexString());
        assertEquals("0x" + PRIV_HEX, priv.toString());
        assertArrayEquals(HexUtils.hexToBytes(PRIV_HEX), priv.toBytes());
        assertThrows(IllegalArgumentException.class, () -> Ed25519PrivateKey.fromBytes(new byte[31]));

        // Defensive copy
        byte[] bytes = priv.toBytes();
        bytes[0] = 0;
        assertEquals(PRIV_HEX, priv.toHexString());
    }

    @Test
    @DisplayName("Private key equals/hashCode are value based")
    void privateKeyEquality() {
        Ed25519PrivateKey a = Ed25519PrivateKey.fromHex(PRIV_HEX);
        Ed25519PrivateKey b = Ed25519PrivateKey.fromHex(PRIV_HEX);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, Ed25519PrivateKey.generate());
        assertNotEquals(a, "x");
        assertEquals(a, a);
    }

    @Test
    @DisplayName("Public key hex/auth-key conversions and validation")
    void publicKeyConversions() {
        Ed25519PublicKey pub = Ed25519PrivateKey.fromHex(PRIV_HEX).publicKey();
        assertEquals("0x" + pub.toHexString(), pub.toString());
        assertEquals(pub, Ed25519PublicKey.fromHex(pub.toString()));
        assertNotNull(pub.authKey());
        assertEquals(pub.authKey().accountAddress(), pub.accountAddress());
        assertThrows(IllegalArgumentException.class, () -> Ed25519PublicKey.fromBytes(new byte[10]));

        assertEquals(pub, pub);
        assertNotEquals(pub, "x");
    }

    @Test
    @DisplayName("Public key BCS serialize/deserialize round-trips")
    void publicKeyBcsRoundTrip() throws IOException {
        Ed25519PublicKey pub = Ed25519PrivateKey.fromHex(PRIV_HEX).publicKey();
        Serializer s = new Serializer();
        pub.serialize(s); // length-prefixed
        byte[] bytes = s.toByteArray();
        assertEquals(33, bytes.length);

        // deserialize() reads a fixed 32 bytes (no length prefix), so skip the prefix byte.
        Deserializer d = new Deserializer(java.util.Arrays.copyOfRange(bytes, 1, bytes.length));
        Ed25519PublicKey restored = Ed25519PublicKey.deserialize(d);
        assertEquals(pub, restored);
    }

    @Test
    @DisplayName("verifySignature returns false on malformed verification rather than throwing")
    void verifyHandlesErrorsGracefully() {
        Ed25519PublicKey pub = Ed25519PrivateKey.generate().publicKey();
        Signature wrong = Signature.fromBytes(new byte[64]);
        assertFalse(pub.verifySignature("msg".getBytes(), wrong));
    }

    @Test
    @DisplayName("Signature conversions and equality")
    void signatureConversions() {
        byte[] raw = new byte[64];
        raw[0] = 0x7f;
        Signature sig = Signature.fromBytes(raw);
        assertEquals(sig, Signature.fromHex(sig.toString()));
        assertEquals(sig, Signature.fromHex(sig.toHexString()));
        assertEquals("0x" + sig.toHexString(), sig.toString());
        assertThrows(IllegalArgumentException.class, () -> Signature.fromBytes(new byte[10]));
        assertEquals(sig, sig);
        assertNotEquals(sig, "x");
        assertEquals(sig.hashCode(), Signature.fromBytes(raw).hashCode());
    }
}
