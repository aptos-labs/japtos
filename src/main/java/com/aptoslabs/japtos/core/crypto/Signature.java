package com.aptoslabs.japtos.core.crypto;

import com.aptoslabs.japtos.utils.HexUtils;

import java.util.Arrays;

/**
 * Represents a cryptographic signature in the Aptos ecosystem.
 *
 * <p>This class encapsulates a digital signature, which is a cryptographic proof
 * that a message was signed by the holder of a specific private key. In Aptos,
 * signatures are primarily Ed25519 signatures with a fixed length of 64 bytes.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>64-byte signature representation (Ed25519 standard)</li>
 *   <li>Immutable signature data with defensive copying</li>
 *   <li>Hex and binary serialization support</li>
 *   <li>Proper equals() and hashCode() implementation</li>
 * </ul>
 *
 * <p>Signatures are created by private keys when signing messages or transactions
 * and are verified using the corresponding public keys. The signature provides
 * cryptographic proof of authenticity and integrity.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create from hex string
 * Signature signature = Signature.fromHex(
 *     "0x1234...abcd" // 128 hex characters (64 bytes)
 * );
 *
 * // Create from byte array
 * byte[] signatureBytes = // ... 64-byte signature
 * Signature signature = Signature.fromBytes(signatureBytes);
 *
 * // Use with public key verification
 * Ed25519PublicKey publicKey = // ... obtain public key
 * byte[] message = "Hello, Aptos!".getBytes();
 * boolean isValid = publicKey.verifySignature(message, signature);
 * }</pre>
 *
 * @see Ed25519PrivateKey#sign(byte[])
 * @see PublicKey#verifySignature(byte[], Signature)
 * @see Ed25519PublicKey
 * @since 1.0.0
 */
public class Signature {
    /**
     * The required length of an Ed25519 signature in bytes.
     */
    public static final int LENGTH = 64;
    private final byte[] signature;

    protected Signature(byte[] signature) {
        if (signature.length != LENGTH) {
            throw new IllegalArgumentException("Signature must be exactly " + LENGTH + " bytes");
        }
        this.signature = Arrays.copyOf(signature, signature.length);
    }

    /**
     * Creates a Signature from a byte array.
     *
     * <p>The byte array must be exactly 64 bytes long for Ed25519 signatures.
     * This method creates a defensive copy of the input array to prevent
     * external modification of the signature data.</p>
     *
     * @param signature the 64-byte signature data
     * @return a new Signature instance
     * @throws IllegalArgumentException if the signature is not exactly 64 bytes
     */
    public static Signature fromBytes(byte[] signature) {
        return new Signature(signature);
    }

    /**
     * Creates a Signature from a hexadecimal string.
     *
     * <p>The hex string can optionally start with '0x' prefix, which will be
     * automatically stripped. The hex string must represent exactly 64 bytes
     * (128 hex characters after prefix removal).</p>
     *
     * @param hex the hexadecimal representation of the signature
     * @return a new Signature instance
     * @throws IllegalArgumentException if the hex string doesn't represent 64 bytes
     * @throws NumberFormatException    if the string contains invalid hex characters
     */
    public static Signature fromHex(String hex) {
        if (hex.startsWith("0x")) {
            hex = hex.substring(2);
        }
        byte[] bytes = HexUtils.hexToBytes(hex);
        return fromBytes(bytes);
    }

    /**
     * Returns the signature as a byte array.
     *
     * <p>This method returns a defensive copy of the signature bytes to prevent
     * external modification. The returned array is always exactly 64 bytes long.</p>
     *
     * @return a copy of the 64-byte signature data
     */
    public byte[] toBytes() {
        return Arrays.copyOf(signature, signature.length);
    }

    /**
     * Returns the signature as a hexadecimal string without '0x' prefix.
     *
     * <p>The returned string is lowercase and exactly 128 characters long,
     * representing the 64 bytes of the signature in hexadecimal format.</p>
     *
     * @return the signature as a lowercase hex string (no '0x' prefix)
     */
    public String toHexString() {
        return HexUtils.bytesToHex(signature);
    }

    /**
     * Returns the signature as a hexadecimal string with '0x' prefix.
     *
     * <p>This method provides a blockchain-standard representation of the
     * signature with the '0x' prefix indicating hexadecimal format.</p>
     *
     * @return the signature as a hex string with '0x' prefix
     */
    public String toString() {
        return "0x" + toHexString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Signature that = (Signature) obj;
        return Arrays.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(signature);
    }
}
