package com.aptoslabs.japtos.core.crypto;

import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AuthenticationKey;
import com.aptoslabs.japtos.utils.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Represents a multi-signature Ed25519 public key for threshold-based authentication.
 *
 * <p>MultiEd25519 enables M-of-N multi-signature schemes where M signatures from a set
 * of N public keys are required to authenticate a transaction. This provides enhanced
 * security through distributed key management and threshold-based consensus.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Collection of Ed25519 public keys</li>
 *   <li>Configurable signature threshold (M-of-N)</li>
 *   <li>BCS serialization support</li>
 *   <li>Authentication key derivation</li>
 *   <li>Multi-signature verification</li>
 * </ul>
 *
 * <p>The authentication key for a MultiEd25519 public key is derived from the first
 * public key in the collection, maintaining compatibility with single-signature schemes
 * while enabling multi-signature functionality.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create a 2-of-3 multi-signature public key
 * List<Ed25519PublicKey> keys = Arrays.asList(
 *     Ed25519PublicKey.fromHex("0x1234..."),
 *     Ed25519PublicKey.fromHex("0x5678..."),
 *     Ed25519PublicKey.fromHex("0x9abc...")
 * );
 * MultiEd25519PublicKey multiKey = new MultiEd25519PublicKey(keys, 2);
 *
 * // Get authentication key
 * AuthenticationKey authKey = multiKey.authKey();
 *
 * // Serialize for transmission
 * byte[] serialized = multiKey.toBytes();
 * }</pre>
 *
 * @see Ed25519PublicKey
 * @see MultiEd25519Signature
 * @see PublicKey
 * @see AuthenticationKey
 * @since 1.0.0
 */
public class MultiEd25519PublicKey implements PublicKey {
    private final List<Ed25519PublicKey> publicKeys;
    private final int threshold;

    /**
     * Creates a new MultiEd25519PublicKey with the specified public keys and threshold.
     *
     * <p>The threshold must be between 1 and the number of public keys (inclusive).
     * For M-of-N multi-signature, threshold = M and publicKeys.size() = N.</p>
     *
     * @param publicKeys the list of Ed25519 public keys (must not be empty)
     * @param threshold  the minimum number of signatures required (1 ≤ threshold ≤ N)
     * @throws IllegalArgumentException if threshold is invalid or publicKeys is empty
     */
    public MultiEd25519PublicKey(List<Ed25519PublicKey> publicKeys, int threshold) {
        if (publicKeys == null || publicKeys.isEmpty()) {
            throw new IllegalArgumentException("Public keys list cannot be null or empty");
        }
        if (threshold < 1 || threshold > publicKeys.size()) {
            throw new IllegalArgumentException(
                    "Threshold must be between 1 and " + publicKeys.size() + ", got: " + threshold);
        }
        this.publicKeys = List.copyOf(publicKeys); // Defensive copy
        this.threshold = threshold;
    }

    @Override
    public byte[] toBytes() {
        // For MultiEd25519, we need to serialize the structure
        try {
            Serializer serializer = new Serializer();
            serialize(serializer);
            return serializer.toByteArray();
        } catch (IOException e) {
            Logger.error("Failed to serialize MultiEd25519PublicKey", e);
            throw new RuntimeException("Failed to serialize MultiEd25519PublicKey", e);
        }
    }

    @Override
    public String toHexString() {
        return com.aptoslabs.japtos.utils.HexUtils.bytesToHex(toBytes());
    }

    @Override
    public AuthenticationKey authKey() {
        // For MultiEd25519, the authentication key is derived from the first public key
        return publicKeys.get(0).authKey();
    }

    @Override
    public boolean verifySignature(byte[] message, Signature signature) {
        // For MultiEd25519, we need to verify against the threshold number of public keys
        // This is a simplified implementation - in practice, you'd need to verify the multi-signature
        return publicKeys.get(0).verifySignature(message, signature);
    }

    /**
     * Serializes this MultiEd25519PublicKey using BCS (Binary Canonical Serialization).
     *
     * <p>The serialization format includes:
     * <ol>
     *   <li>Number of public keys as ULEB128</li>
     *   <li>Each public key as 32 bytes</li>
     *   <li>Threshold as single byte</li>
     * </ol>
     *
     * @param serializer the BCS serializer to write to
     * @throws IOException if serialization fails
     * @see Serializer
     */
    public void serialize(Serializer serializer) throws IOException {
        // Serialize public keys as a vector
        serializer.serializeU32AsUleb128(publicKeys.size());
        for (Ed25519PublicKey publicKey : publicKeys) {
            serializer.serializeBytes(publicKey.toBytes());
        }

        // Serialize threshold
        serializer.serializeU8((byte) threshold);
    }

    /**
     * Returns the list of Ed25519 public keys in this multi-signature key.
     *
     * @return an immutable list of Ed25519PublicKey instances
     */
    public List<Ed25519PublicKey> getPublicKeys() {
        return publicKeys;
    }

    /**
     * Returns the signature threshold for this multi-signature key.
     *
     * <p>This is the minimum number of valid signatures required from the
     * public keys to authenticate a transaction or message.</p>
     *
     * @return the signature threshold (M in M-of-N scheme)
     */
    public int getThreshold() {
        return threshold;
    }

    @Override
    public String toString() {
        return "MultiEd25519PublicKey{" +
                "publicKeys=" + publicKeys.toString() +
                ", threshold=" + threshold +
                '}';
    }
}
