package com.aptoslabs.japtos.core;

import com.aptoslabs.japtos.utils.CryptoUtils;
import com.aptoslabs.japtos.utils.HexUtils;

import java.util.Arrays;

/**
 * Represents an Aptos authentication key used for account address derivation.
 *
 * <p>Authentication keys are 32-byte cryptographic hashes derived from public keys
 * and signature scheme identifiers. They serve as an intermediate step in the
 * account address generation process and provide a stable identifier for accounts
 * that remains constant even when keys are rotated.</p>
 *
 * <p>Key derivation process:</p>
 * <ol>
 *   <li>Concatenate public key bytes with scheme identifier byte</li>
 *   <li>Compute SHA3-256 hash of the concatenated data</li>
 *   <li>The resulting 32-byte hash is the authentication key</li>
 *   <li>Account address is derived directly from authentication key</li>
 * </ol>
 *
 * <p>Supported signature schemes:</p>
 * <ul>
 *   <li><strong>Ed25519</strong> - Single signature scheme (scheme = 0)</li>
 *   <li><strong>MultiEd25519</strong> - Multi-signature scheme (scheme = 1)</li>
 *   <li><strong>SingleKey</strong> - Modern single key scheme (scheme = 2)</li>
 * </ul>
 *
 * <p>The authentication key enables key rotation by allowing the same account
 * address to be controlled by different keys over time, while maintaining
 * the account's identity and associated resources.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create from Ed25519 public key
 * Ed25519PublicKey publicKey = // ... obtain public key
 * AuthenticationKey authKey = AuthenticationKey.fromPublicKey(publicKey.toBytes());
 *
 * // Derive account address
 * AccountAddress address = authKey.accountAddress();
 *
 * // Create from hex string
 * AuthenticationKey authKey2 = AuthenticationKey.fromHex(
 *     "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
 * );
 * }</pre>
 *
 * @see AccountAddress
 * @see PublicKey
 * @see Ed25519PublicKey
 * @since 1.0.0
 */
public class AuthenticationKey {
    /**
     * The required length of an authentication key in bytes.
     */
    public static final int LENGTH = 32;
    private final byte[] key;

    private AuthenticationKey(byte[] key) {
        if (key.length != LENGTH) {
            throw new IllegalArgumentException("Authentication key must be exactly " + LENGTH + " bytes");
        }
        this.key = Arrays.copyOf(key, key.length);
    }

    /**
     * Creates an AuthenticationKey from a 32-byte array.
     *
     * <p>The byte array must be exactly 32 bytes long. This method creates
     * a defensive copy to prevent external modification of the key data.</p>
     *
     * @param key the 32-byte authentication key data
     * @return a new AuthenticationKey instance
     * @throws IllegalArgumentException if the array is not exactly 32 bytes
     */
    public static AuthenticationKey fromBytes(byte[] key) {
        return new AuthenticationKey(key);
    }

    /**
     * Creates an AuthenticationKey from a hexadecimal string.
     *
     * <p>The hex string can optionally start with '0x' prefix, which will be
     * automatically stripped. The hex string must represent exactly 32 bytes
     * (64 hex characters after prefix removal).</p>
     *
     * @param hex the hexadecimal representation of the authentication key
     * @return a new AuthenticationKey instance
     * @throws IllegalArgumentException if the hex string doesn't represent 32 bytes
     * @throws NumberFormatException    if the string contains invalid hex characters
     */
    public static AuthenticationKey fromHex(String hex) {
        if (hex.startsWith("0x")) {
            hex = hex.substring(2);
        }
        byte[] bytes = HexUtils.hexToBytes(hex);
        return fromBytes(bytes);
    }

    /**
     * Creates an AuthenticationKey from a public key and signature scheme.
     *
     * <p>This method implements the standard Aptos authentication key derivation:
     * AuthKey = SHA3-256(public_key_bytes || scheme_byte)</p>
     *
     * <p>The scheme byte identifies the signature scheme:
     * <ul>
     *   <li>0 = Ed25519 single signature</li>
     *   <li>1 = MultiEd25519 multi-signature</li>
     *   <li>2 = SingleKey (modern scheme)</li>
     * </ul></p>
     *
     * @param scheme the signature scheme identifier byte
     * @param input  the public key bytes
     * @return a new AuthenticationKey derived from the public key and scheme
     * @throws RuntimeException if SHA3-256 algorithm is not available
     */
    public static AuthenticationKey fromSchemeAndBytes(byte scheme, byte[] input) {
        byte[] concat = new byte[input.length + 1];
        System.arraycopy(input, 0, concat, 0, input.length);
        concat[concat.length - 1] = scheme;
        byte[] hash = CryptoUtils.sha3_256(concat);
        return fromBytes(hash);
    }

    /**
     * Creates an AuthenticationKey from an Ed25519 public key using legacy derivation.
     *
     * <p>This is a convenience method that uses scheme byte 0 for Ed25519 signatures.
     * It maintains compatibility with older account creation methods.</p>
     *
     * @param publicKey the Ed25519 public key bytes (32 bytes)
     * @return a new AuthenticationKey for the Ed25519 public key
     * @see #fromSchemeAndBytes(byte, byte[])
     */
    public static AuthenticationKey fromPublicKey(byte[] publicKey) {
        // Ed25519 scheme byte = 0 per TS SDK
        return fromSchemeAndBytes((byte) 0, publicKey);
    }

    /**
     * Get the authentication key as a byte array
     */
    public byte[] toBytes() {
        return Arrays.copyOf(key, key.length);
    }

    /**
     * Get the authentication key as a hex string without 0x prefix
     */
    public String toHexString() {
        return HexUtils.bytesToHex(key);
    }

    /**
     * Get the authentication key as a hex string with 0x prefix
     */
    public String toString() {
        return "0x" + toHexString();
    }

    /**
     * Derives the account address from this authentication key.
     *
     * <p>In Aptos, the account address is identical to the authentication key.
     * This method provides a convenient way to get the AccountAddress
     * representation of this authentication key.</p>
     *
     * @return the AccountAddress derived from this authentication key
     * @see AccountAddress
     */
    public AccountAddress accountAddress() {
        return AccountAddress.fromBytes(key);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AuthenticationKey that = (AuthenticationKey) obj;
        return Arrays.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }
}
