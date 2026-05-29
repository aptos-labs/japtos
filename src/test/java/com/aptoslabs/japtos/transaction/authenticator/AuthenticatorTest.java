package com.aptoslabs.japtos.transaction.authenticator;

import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.crypto.Ed25519PrivateKey;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.core.crypto.PublicKey;
import com.aptoslabs.japtos.core.crypto.Signature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for every authenticator implementation, asserting variant tags,
 * bitmap construction and accessor behaviour.
 */
class AuthenticatorTest {

    private final Ed25519PrivateKey priv = Ed25519PrivateKey.generate();
    private final Ed25519PublicKey pub = priv.publicKey();
    private final Signature sig = priv.sign("m".getBytes());

    @Test
    @DisplayName("Ed25519Authenticator: variant 0, plus public key and signature")
    void ed25519Authenticator() throws IOException {
        Ed25519Authenticator auth = new Ed25519Authenticator(pub, sig);
        assertSame(pub, auth.getPublicKeyObject());
        assertSame(sig, auth.getSignatureObject());
        assertArrayEquals(pub.toBytes(), auth.getPublicKey());
        assertArrayEquals(sig.toBytes(), auth.getSignature());
        assertArrayEquals(pub.authKey().toBytes(), auth.getAuthenticationKey());
        assertTrue(auth.toString().contains("Ed25519Authenticator"));

        byte[] bytes = auth.bcsToBytes();
        assertEquals(0x00, bytes[0]); // variant
        // 1 + (1+32) + (1+64) = 99
        assertEquals(99, bytes.length);
    }

    @Test
    @DisplayName("TransactionAuthenticatorEd25519 writes variant 0 as ULEB128")
    void transactionAuthenticatorEd25519() throws IOException {
        TransactionAuthenticatorEd25519 auth = new TransactionAuthenticatorEd25519(pub, sig);
        assertSame(pub, auth.getPublicKey());
        assertSame(sig, auth.getSignature());
        assertTrue(auth.toString().contains("TransactionAuthenticatorEd25519"));
        byte[] bytes = auth.bcsToBytes();
        assertEquals(0x00, bytes[0]);
        assertEquals(99, bytes.length);
    }

    @Test
    @DisplayName("MultiEd25519Authenticator builds an MSB-first bitmap and serializes variant 1")
    void multiEd25519Authenticator() throws IOException {
        Ed25519PublicKey k2 = Ed25519PrivateKey.generate().publicKey();
        MultiEd25519Authenticator auth =
                new MultiEd25519Authenticator(List.of(pub, k2), sig, 1, List.of(0));
        assertEquals(2, auth.getPublicKeys().size());
        assertEquals(1, auth.getThreshold());
        assertEquals(List.of(0), auth.getSignerIndices());
        assertSame(sig, auth.getSignatureObject());
        assertArrayEquals(pub.toBytes(), auth.getPublicKey());
        assertArrayEquals(pub.toBytes(), auth.getAuthenticationKey());
        assertArrayEquals(sig.toBytes(), auth.getSignature());

        byte[] bytes = auth.bcsToBytes();
        assertEquals(0x01, bytes[0]);

        // Convenience constructor defaults to signer index 0
        MultiEd25519Authenticator single = new MultiEd25519Authenticator(List.of(pub), sig, 1);
        assertEquals(List.of(0), single.getSignerIndices());

        // Out-of-range signer index is rejected at serialization time
        MultiEd25519Authenticator bad =
                new MultiEd25519Authenticator(List.of(pub), sig, 1, List.of(5));
        assertThrows(IllegalArgumentException.class, () -> bad.serialize(new Serializer()));
    }

    @Test
    @DisplayName("TransactionAuthenticatorMultiEd25519 serializes variant 1 with fixed bitmap")
    void transactionAuthenticatorMultiEd25519() throws IOException {
        Ed25519PublicKey k2 = Ed25519PrivateKey.generate().publicKey();
        TransactionAuthenticatorMultiEd25519 auth =
                new TransactionAuthenticatorMultiEd25519(List.of(pub, k2), sig, 1);
        assertEquals(2, auth.getPublicKeys().size());
        assertSame(sig, auth.getSignature());
        assertEquals(1, auth.getThreshold());
        assertTrue(auth.toString().contains("MultiEd25519"));
        byte[] bytes = auth.bcsToBytes();
        assertEquals(0x01, bytes[0]);
    }

    @Test
    @DisplayName("MultiKeyAuthenticator serializes variant 3 and supports single/multi signatures")
    void multiKeyAuthenticator() throws IOException {
        List<PublicKey> keys = List.of(pub, Ed25519PrivateKey.generate().publicKey());

        // Single-signature convenience constructor
        MultiKeyAuthenticator single = new MultiKeyAuthenticator(keys, sig, 1, List.of(0));
        assertEquals(1, single.getSignatures().size());
        assertEquals(keys, single.getPublicKeys());
        assertEquals(1, single.getThreshold());
        assertEquals(List.of(0), single.getSignerIndices());
        assertArrayEquals(pub.toBytes(), single.getAuthenticationKey());
        assertTrue(single.getPublicKey().length > 0);
        assertArrayEquals(sig.toBytes(), single.getSignature());

        byte[] bytes = single.bcsToBytes();
        assertEquals(0x03, bytes[0]);

        // Multi-signature list constructor
        MultiKeyAuthenticator multi =
                new MultiKeyAuthenticator(keys, List.of(sig, sig), 2, List.of(0, 1));
        assertEquals(2, multi.getSignatures().size());
        assertEquals(0x03, multi.bcsToBytes()[0]);

        // signer index >= 32 rejected
        MultiKeyAuthenticator bad = new MultiKeyAuthenticator(keys, sig, 1, List.of(40));
        assertThrows(IllegalArgumentException.class, () -> bad.serialize(new Serializer()));
    }

    @Test
    @DisplayName("MultiKeyAuthenticator with no signatures yields an empty signature blob")
    void multiKeyAuthenticatorEmptySignatures() {
        List<PublicKey> keys = List.of(pub);
        MultiKeyAuthenticator empty =
                new MultiKeyAuthenticator(keys, List.<Signature>of(), 1, List.of(0));
        assertEquals(0, empty.getSignature().length);
    }

    @Test
    @DisplayName("TransactionAuthenticatorMultiKey serializes variant 3 with key/sig vectors")
    void transactionAuthenticatorMultiKey() throws IOException {
        List<Ed25519PublicKey> keys = List.of(pub, Ed25519PrivateKey.generate().publicKey());
        TransactionAuthenticatorMultiKey auth =
                new TransactionAuthenticatorMultiKey(keys, List.of(sig), 1, List.of(0));
        assertEquals(2, auth.getPublicKeys().size());
        assertEquals(1, auth.getSignatures().size());
        assertEquals(1, auth.getThreshold());
        assertEquals(List.of(0), auth.getSignerIndices());
        assertTrue(auth.toString().contains("MultiKey"));

        byte[] bytes = auth.bcsToBytes();
        assertEquals(0x03, bytes[0]);

        TransactionAuthenticatorMultiKey bad =
                new TransactionAuthenticatorMultiKey(keys, List.of(sig), 1, List.of(40));
        assertThrows(IllegalArgumentException.class, () -> bad.serialize(new Serializer()));
    }

    @Test
    @DisplayName("TransactionAuthenticatorSingleSender wraps an inner authenticator under variant 4")
    void singleSender() throws IOException {
        Ed25519Authenticator inner = new Ed25519Authenticator(pub, sig);
        TransactionAuthenticatorSingleSender wrapper =
                new TransactionAuthenticatorSingleSender(inner);
        assertSame(inner, wrapper.getSenderAuthenticator());

        byte[] bytes = wrapper.bcsToBytes();
        assertEquals(0x04, bytes[0]); // SingleSender variant
        assertEquals(0x00, bytes[1]); // inner Ed25519 AccountAuthenticator variant
    }
}
