package com.aptoslabs.japtos.core;

import com.aptoslabs.japtos.core.crypto.Ed25519PrivateKey;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.utils.CryptoUtils;
import com.aptoslabs.japtos.utils.HexUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AuthenticationKey} derivation and conversions.
 */
class AuthenticationKeyTest {

    @Test
    @DisplayName("fromSchemeAndBytes computes sha3_256(input || scheme)")
    void fromSchemeAndBytesMatchesManualDerivation() {
        byte[] input = new byte[32];
        for (int i = 0; i < 32; i++) input[i] = (byte) i;
        byte scheme = 0;

        byte[] concat = new byte[33];
        System.arraycopy(input, 0, concat, 0, 32);
        concat[32] = scheme;
        byte[] expected = CryptoUtils.sha3_256(concat);

        AuthenticationKey ak = AuthenticationKey.fromSchemeAndBytes(scheme, input);
        assertArrayEquals(expected, ak.toBytes());
    }

    @Test
    @DisplayName("fromPublicKey uses the Ed25519 scheme byte 0")
    void fromPublicKeyUsesScheme0() {
        byte[] pk = new byte[32];
        AuthenticationKey viaPublicKey = AuthenticationKey.fromPublicKey(pk);
        AuthenticationKey viaScheme = AuthenticationKey.fromSchemeAndBytes((byte) 0, pk);
        assertEquals(viaScheme, viaPublicKey);
    }

    @Test
    @DisplayName("Account address derived from an auth key equals the key bytes")
    void accountAddressEqualsKeyBytes() {
        Ed25519PublicKey pub = Ed25519PrivateKey.generate().publicKey();
        AuthenticationKey ak = pub.authKey();
        AccountAddress addr = ak.accountAddress();
        assertArrayEquals(ak.toBytes(), addr.toBytes());
    }

    @Test
    @DisplayName("fromHex / fromBytes round-trip and reject bad lengths")
    void hexAndBytesConversions() {
        byte[] bytes = new byte[32];
        bytes[31] = 0x09;
        AuthenticationKey ak = AuthenticationKey.fromBytes(bytes);
        String hex = ak.toHexString();
        assertEquals(64, hex.length());
        assertEquals("0x" + hex, ak.toString());
        assertEquals(ak, AuthenticationKey.fromHex("0x" + hex));
        assertEquals(ak, AuthenticationKey.fromHex(hex));
        assertArrayEquals(bytes, HexUtils.hexToBytes(hex));

        assertThrows(IllegalArgumentException.class, () -> AuthenticationKey.fromBytes(new byte[16]));
    }

    @Test
    @DisplayName("equals and hashCode are value-based")
    void equalsAndHashCode() {
        AuthenticationKey a = AuthenticationKey.fromBytes(new byte[32]);
        AuthenticationKey b = AuthenticationKey.fromBytes(new byte[32]);
        byte[] other = new byte[32];
        other[0] = 1;
        AuthenticationKey c = AuthenticationKey.fromBytes(other);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, "x");
        assertEquals(a, a);
    }
}
