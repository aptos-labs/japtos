package com.aptoslabs.japtos.transaction.authenticator;

import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.core.crypto.Signature;

import java.io.IOException;
import java.util.List;

/**
 * MultiEd25519 authenticator for multi-signature transactions.
 *
 * <p>This authenticator handles multi-signature transactions where multiple
 * Ed25519 public keys can sign a transaction, with a configurable threshold
 * of required signatures.</p>
 *
 * <p>The authenticator includes:</p>
 * <ul>
 *   <li>A list of Ed25519 public keys</li>
 *   <li>A signature from one or more of the private keys</li>
 *   <li>A threshold indicating how many signatures are required</li>
 *   <li>A bitmap indicating which public keys contributed to the signature</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * List<Ed25519PublicKey> publicKeys = Arrays.asList(key1, key2, key3);
 * Signature signature = privateKey.sign(message);
 * MultiEd25519Authenticator auth = new MultiEd25519Authenticator(
 *     publicKeys, signature, 2, Arrays.asList(0, 2)
 * );
 * }</pre>
 *
 * @see Ed25519Authenticator
 * @see AccountAuthenticator
 * @since 1.0.0
 */
public class MultiEd25519Authenticator implements AccountAuthenticator {
    private final List<Ed25519PublicKey> publicKeys;
    private final Signature signature;
    private final int threshold;
    private final List<Integer> signerIndices;

    /**
     * Creates a new MultiEd25519Authenticator with the specified parameters.
     *
     * @param publicKeys    the list of Ed25519 public keys in the multi-signature setup
     * @param signature     the signature from one or more of the private keys
     * @param threshold     the minimum number of signatures required
     * @param signerIndices the indices of the public keys that contributed to the signature
     */
    public MultiEd25519Authenticator(List<Ed25519PublicKey> publicKeys, Signature signature, int threshold, List<Integer> signerIndices) {
        this.publicKeys = publicKeys;
        this.signature = signature;
        this.threshold = threshold;
        this.signerIndices = signerIndices;
    }

    /**
     * Creates a new MultiEd25519Authenticator for single signer (backward compatibility).
     *
     * @param publicKeys the list of Ed25519 public keys in the multi-signature setup
     * @param signature  the signature from the private key
     * @param threshold  the minimum number of signatures required
     */
    public MultiEd25519Authenticator(List<Ed25519PublicKey> publicKeys, Signature signature, int threshold) {
        this(publicKeys, signature, threshold, List.of(0)); // Default to first signer
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

        // Create bitmap based on signer indices
        byte[] bitmap = createBitmap(signerIndices, publicKeys.size());
        System.arraycopy(bitmap, 0, sigBytes, 64, 4);

        serializer.serializeBytes(sigBytes);
    }

    /**
     * Creates a bitmap indicating which public keys contributed to the signature.
     *
     * <p>The bitmap is a 4-byte array where each bit represents whether a public key
     * at that index contributed to the signature. The bitmap is MSB-first per byte.</p>
     *
     * @param signerIndices the indices of the public keys that signed
     * @param totalKeys     the total number of public keys in the multi-signature setup
     * @return a 4-byte bitmap array
     */
    private byte[] createBitmap(List<Integer> signerIndices, int totalKeys) {
        byte[] bitmap = new byte[4];

        for (int index : signerIndices) {
            if (index < 0 || index >= totalKeys) {
                throw new IllegalArgumentException("Signer index " + index + " is out of bounds for " + totalKeys + " keys");
            }

            // Calculate byte and bit position
            int byteIndex = index / 8;
            int bitPosition = 7 - (index % 8); // MSB-first

            if (byteIndex < 4) {
                bitmap[byteIndex] |= (1 << bitPosition);
            }
        }

        return bitmap;
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

    public List<Integer> getSignerIndices() {
        return signerIndices;
    }
}
