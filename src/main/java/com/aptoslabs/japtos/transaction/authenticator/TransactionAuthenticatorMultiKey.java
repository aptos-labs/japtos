package com.aptoslabs.japtos.transaction.authenticator;

import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.core.crypto.Signature;

import java.io.IOException;
import java.util.List;

/**
 * MultiKey transaction authenticator for scheme 3.
 * <p>
 * This authenticator is used for MultiKeyAccount transactions.
 */
public class TransactionAuthenticatorMultiKey implements Serializable {
    private static final int AUTHENTICATOR_VARIANT_MULTIKEY = 3;
    private static final int BITMAP_LEN = 4;

    private final List<Ed25519PublicKey> publicKeys;
    private final List<Signature> signatures;
    private final int threshold;
    private final List<Integer> signerIndices;

    public TransactionAuthenticatorMultiKey(List<Ed25519PublicKey> publicKeys,
                                            List<Signature> signatures,
                                            int threshold,
                                            List<Integer> signerIndices) {
        this.publicKeys = publicKeys;
        this.signatures = signatures;
        this.threshold = threshold;
        this.signerIndices = signerIndices;
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        // TransactionAuthenticator variant: 3 = MultiKey
        serializer.serializeU32AsUleb128(AUTHENTICATOR_VARIANT_MULTIKEY);

        // Serialize MultiKeyPublicKey: vector<AnyPublicKey> + u8 threshold (match ts-sdk)
        serializer.serializeU32AsUleb128(publicKeys.size());
        for (Ed25519PublicKey pk : publicKeys) {
            // AnyPublicKey variant for Ed25519 is 0 (ULEB128)
            serializer.serializeU32AsUleb128(0);
            byte[] pkBytes = pk.toBytes();
            if (pkBytes.length != 32) {
                throw new IllegalArgumentException("Ed25519 public key must be 32 bytes");
            }
            // Ed25519PublicKey.serialize => serializeBytes(bytes)
            serializer.serializeBytes(pkBytes);
        }
        serializer.serializeU8((byte) threshold);


        // Serialize MultiKeySignature: vector<AnySignature> + bitmap (match ts-sdk)
        serializer.serializeU32AsUleb128(signatures.size());
        for (Signature sig : signatures) {
            // AnySignature variant for Ed25519 is 0 (ULEB128)
            serializer.serializeU32AsUleb128(0);
            byte[] sigBytes = sig.toBytes();
            if (sigBytes.length != 64) {
                throw new IllegalArgumentException("Ed25519 signature must be 64 bytes");
            }
            // Ed25519Signature.serialize => serializeBytes(bytes)
            serializer.serializeBytes(sigBytes);
        }

        // Serialize bitmap as bytes (length-prefixed)
        byte[] bitmap = createBitmap(signerIndices);
        if (bitmap.length != BITMAP_LEN) {
            throw new IllegalArgumentException("Bitmap must be exactly 4 bytes");
        }
        serializer.serializeBytes(bitmap);

    }

    private byte[] createBitmap(List<Integer> signerIndices) {
        byte[] bitmap = new byte[BITMAP_LEN];
        for (int index : signerIndices) {
            if (index >= 32) {
                throw new IllegalArgumentException("Signer index cannot be >= 32");
            }
            int byteIndex = index / 8;
            int bitIndex = 7 - (index % 8); // MSB-first: bit 0 is the leftmost bit
            bitmap[byteIndex] |= (1 << bitIndex);
        }
        return bitmap;
    }

    public List<Ed25519PublicKey> getPublicKeys() {
        return publicKeys;
    }

    public List<Signature> getSignatures() {
        return signatures;
    }

    public int getThreshold() {
        return threshold;
    }

    public List<Integer> getSignerIndices() {
        return signerIndices;
    }

    @Override
    public String toString() {
        return "TransactionAuthenticatorMultiKey{" +
                "publicKeys=" + publicKeys.toString() +
                ", signatures=" + signatures.toString() +
                ", threshold=" + threshold +
                ", signerIndices=" + signerIndices.toString() +
                '}';
    }
}
