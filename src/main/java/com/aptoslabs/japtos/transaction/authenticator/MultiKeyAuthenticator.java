package com.aptoslabs.japtos.transaction.authenticator;

import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.crypto.AnyPublicKey;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.core.crypto.PublicKey;
import com.aptoslabs.japtos.core.crypto.Signature;
import com.aptoslabs.japtos.utils.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * AccountAuthenticator for MultiKey signature scheme (scheme 3).
 *
 * <p>This authenticator is used for multi-signature accounts created with MultiKeyAccount.
 * It supports K-of-N signature schemes where K signatures are required from N public keys.</p>
 *
 * <p>The MultiKey scheme uses BCS serialization for the public keys and includes
 * a bitmap to indicate which signers have signed the transaction.</p>
 */
public class MultiKeyAuthenticator implements AccountAuthenticator {
    private static final int AUTHENTICATOR_VARIANT_MULTIKEY = 3;
    private static final int BITMAP_LEN = 4; // 32 bits = 4 bytes

    private final List<PublicKey> publicKeys;
    private final List<Signature> signatures;
    private final int threshold;
    private final List<Integer> signerIndices;

    /**
     * Creates a new MultiKeyAuthenticator.
     *
     * @param publicKeys    All public keys in the multi-key setup
     * @param signatures    The signatures from the signers
     * @param threshold     The minimum number of signatures required
     * @param signerIndices The indices of the signers who signed
     */
    public MultiKeyAuthenticator(List<PublicKey> publicKeys, List<Signature> signatures,
                                 int threshold, List<Integer> signerIndices) {
        this.publicKeys = publicKeys;
        this.signatures = signatures;
        this.threshold = threshold;
        this.signerIndices = signerIndices;
    }

    /**
     * Creates a new MultiKeyAuthenticator with a single signature.
     * This is a convenience constructor for the common case of signing with one key.
     */
    public MultiKeyAuthenticator(List<PublicKey> publicKeys, Signature signature,
                                 int threshold, List<Integer> signerIndices) {
        this.publicKeys = publicKeys;
        this.signatures = new ArrayList<>();
        this.signatures.add(signature);
        this.threshold = threshold;
        this.signerIndices = signerIndices;
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        // Serialize authenticator variant (3 for MultiKey) as ULEB128 (matches TS)
        serializer.serializeU32AsUleb128(AUTHENTICATOR_VARIANT_MULTIKEY);

        // Serialize MultiKeyPublicKey: vector<AnyPublicKey> + u8 threshold
        serializer.serializeU32AsUleb128(publicKeys.size());
        for (PublicKey pk : publicKeys) {
            // Wrap in AnyPublicKey and serialize
            AnyPublicKey anyPk = new AnyPublicKey(pk);
            anyPk.serialize(serializer);
        }
        serializer.serializeU8((byte) threshold);

        // Serialize MultiKeySignature: vector<AnySignature> + bitmap
        serializer.serializeU32AsUleb128(signatures.size());
        for (Signature sig : signatures) {
            // AnySignature variant for Ed25519 is 0 (ULEB128)
            serializer.serializeU32AsUleb128(0);
            // Ed25519Signature.serialize => serializeBytes(bytes)
            serializer.serializeBytes(sig.toBytes());
        }

        byte[] bitmap = createBitmap(signerIndices);
        if (bitmap.length != BITMAP_LEN) {
            throw new IllegalArgumentException("Bitmap must be exactly 4 bytes");
        }
        // Serialize bitmap as bytes (length-prefixed)
        serializer.serializeBytes(bitmap);
    }

    /**
     * Creates a bitmap indicating which signers have signed.
     * Uses MSB-first ordering to match TypeScript SDK.
     *
     * @param signerIndices The indices of the signers
     * @return A 4-byte bitmap
     */
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

    public List<PublicKey> getPublicKeys() {
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
    public byte[] getAuthenticationKey() {
        // For MultiKey, return the first public key's bytes as authentication key
        // This is a simplified implementation - in production you might want to 
        // derive this differently
        return publicKeys.get(0).toBytes();
    }

    @Override
    public byte[] getPublicKey() {
        // Return serialized representation of all public keys
        try {
            Serializer serializer = new Serializer();
            for (PublicKey pk : publicKeys) {
                AnyPublicKey anyPk = new AnyPublicKey(pk);
                anyPk.serialize(serializer);
            }
            return serializer.toByteArray();
        } catch (IOException e) {
            Logger.error("Failed to serialize public keys", e);
            throw new RuntimeException("Failed to serialize public keys", e);
        }
    }

    @Override
    public byte[] getSignature() {
        // Return the concatenated signatures
        if (signatures.isEmpty()) {
            return new byte[0];
        }

        // For now, return the first signature's bytes
        // In a full implementation, this would concatenate all signatures
        return signatures.get(0).toBytes();
    }
}
