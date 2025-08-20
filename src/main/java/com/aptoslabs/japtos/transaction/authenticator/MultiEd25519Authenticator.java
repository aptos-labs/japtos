package com.aptoslabs.japtos.transaction.authenticator;

import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.core.crypto.Signature;

import java.io.IOException;
import java.util.List;

/**
 * MultiEd25519 authenticator for multi-signature transactions.

 */
public class MultiEd25519Authenticator implements AccountAuthenticator {
    private final List<Ed25519PublicKey> publicKeys;
    private final Signature signature;
    private final int threshold;

    public MultiEd25519Authenticator(List<Ed25519PublicKey> publicKeys, Signature signature, int threshold) {
        this.publicKeys = publicKeys;
        this.signature = signature;
        this.threshold = threshold;
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        // AccountAuthenticator variant for MultiEd25519 is 1 (Ed25519=0, MultiEd25519=1, SingleKey=2...)
        serializer.serializeU8((byte) 1);

        // MultiEd25519PublicKey BCS structure as bytes: concat(pubkeys[0..n-1], threshold)
        byte[] pkBytes = new byte[publicKeys.size() * 32 + 1];
        int offset = 0;
        for (Ed25519PublicKey publicKey : publicKeys) {
            byte[] pk = publicKey.toBytes();
            if (pk.length != 32) {
                throw new IllegalArgumentException("Ed25519 public key must be 32 bytes");
            }
            System.arraycopy(pk, 0, pkBytes, offset, 32);
            offset += 32;
        }
        pkBytes[offset] = (byte) (threshold & 0xFF);
        serializer.serializeBytes(pkBytes);

        // MultiEd25519Signature BCS structure as bytes: concat(signatures..., bitmap)
        byte[] sig = signature.toBytes();
        if (sig.length != 64) {
            throw new IllegalArgumentException("Ed25519 signature must be 64 bytes");
        }
        byte[] sigBytes = new byte[64 + 4];
        System.arraycopy(sig, 0, sigBytes, 0, 64);
        // Bitmap MSB-first per byte: set bit 0 => 0x80 00 00 00
        sigBytes[64] = (byte) 0x80;
        sigBytes[65] = 0x00;
        sigBytes[66] = 0x00;
        sigBytes[67] = 0x00;
        serializer.serializeBytes(sigBytes);
    }

    @Override
    public byte[] getAuthenticationKey() {
        // For MultiEd25519, the authentication key is derived from the public keys
        // This is a simplified implementation
        return publicKeys.get(0).toBytes();
    }

    @Override
    public byte[] getPublicKey() {
        // Return the first public key as the primary one
        return publicKeys.get(0).toBytes();
    }

    @Override
    public byte[] getSignature() {
        return signature.toBytes();
    }

    public Signature getSignatureObject() {
        return signature;
    }

    public List<Ed25519PublicKey> getPublicKeys() {
        return publicKeys;
    }

    public int getThreshold() {
        return threshold;
    }
}
