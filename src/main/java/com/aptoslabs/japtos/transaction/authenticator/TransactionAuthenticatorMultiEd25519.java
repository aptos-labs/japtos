package com.aptoslabs.japtos.transaction.authenticator;

import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.core.crypto.Signature;

import java.io.IOException;
import java.util.List;

/**
 * MultiEd25519 transaction authenticator.
 */
public class TransactionAuthenticatorMultiEd25519 implements Serializable {
    private final List<Ed25519PublicKey> publicKeys;
    private final Signature signature;
    private final int threshold;

    public TransactionAuthenticatorMultiEd25519(List<Ed25519PublicKey> publicKeys, Signature signature, int threshold) {
        this.publicKeys = publicKeys;
        this.signature = signature;
        this.threshold = threshold;
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        // TransactionAuthenticator variant: 1 = MultiEd25519
        serializer.serializeU32AsUleb128(1);

        // MultiEd25519PublicKey as bytes: concat(pubkeys..., threshold)
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

        // MultiEd25519Signature structure as bytes: concat(signatures..., bitmap)
        byte[] sig = signature.toBytes();
        if (sig.length != 64) {
            throw new IllegalArgumentException("Ed25519 signature must be 64 bytes");
        }
        byte[] sigBytes = new byte[64 + 4];
        System.arraycopy(sig, 0, sigBytes, 0, 64);
        // Bitmap MSB-first per byte: first bit set = 0x80 00 00 00
        sigBytes[64] = (byte) 0x80;
        sigBytes[65] = 0x00;
        sigBytes[66] = 0x00;
        sigBytes[67] = 0x00;
        serializer.serializeBytes(sigBytes);
    }

    public List<Ed25519PublicKey> getPublicKeys() {
        return publicKeys;
    }

    public Signature getSignature() {
        return signature;
    }

    public int getThreshold() {
        return threshold;
    }

    @Override
    public String toString() {
        return "TransactionAuthenticatorMultiEd25519{" +
                "publicKeys=" + publicKeys.toString() +
                ", signature=" + signature.toString() +
                ", threshold=" + threshold +
                '}';
    }
}
