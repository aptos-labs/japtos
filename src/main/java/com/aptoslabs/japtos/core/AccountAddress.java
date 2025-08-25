package com.aptoslabs.japtos.core;

import com.aptoslabs.japtos.utils.HexUtils;

import java.util.Arrays;

/**
 * Represents a unique 32-byte account address on the Aptos blockchain.
 *
 * <p>Account addresses serve as the primary identifier for accounts on the Aptos
 * blockchain. They are derived from authentication keys through cryptographic
 * hashing and provide a stable, permanent identifier for accounts regardless
 * of key rotation or other account changes.</p>
 *
 * <p>Key characteristics:</p>
 * <ul>
 *   <li><strong>Fixed Size</strong> - Always exactly 32 bytes (256 bits)</li>
 *   <li><strong>Deterministic</strong> - Same input always produces same address</li>
 *   <li><strong>Unique</strong> - Cryptographically guaranteed uniqueness</li>
 *   <li><strong>Immutable</strong> - Address never changes once established</li>
 * </ul>
 *
 * <p>Address Derivation:</p>
 * <p>Addresses are typically derived from authentication keys using cryptographic
 * hash functions. For Ed25519 accounts, the address is computed from the
 * public key and scheme identifier.</p>
 *
 * <p>Special Addresses:</p>
 * <ul>
 *   <li><strong>Zero Address</strong> - 0x0...0 (all zeros) - special system address</li>
 *   <li><strong>Core Framework</strong> - 0x1 - Aptos core framework modules</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create from hex string
 * AccountAddress addr1 = AccountAddress.fromHex("0x1");
 * AccountAddress addr2 = AccountAddress.fromHex("a1b2c3d4e5f67890...");
 *
 * // Create from byte array
 * byte[] addressBytes = // ... 32-byte array
 * AccountAddress addr3 = AccountAddress.fromBytes(addressBytes);
 *
 * // Create zero address
 * AccountAddress zero = AccountAddress.zero();
 *
 * // Convert to string representations
 * String hex = addr1.toHexString();    // without 0x prefix
 * String full = addr1.toString();      // with 0x prefix
 * }</pre>
 *
 * @see AuthenticationKey
 * @see PublicKey
 * @since 1.0.0
 */
public class AccountAddress {
    /**
     * The required length of an Aptos account address in bytes.
     */
    public static final int LENGTH = 32;
    private final byte[] address;

    private AccountAddress(byte[] address) {
        if (address.length != LENGTH) {
            throw new IllegalArgumentException("Account address must be exactly " + LENGTH + " bytes");
        }
        this.address = Arrays.copyOf(address, address.length);
    }

    /**
     * Creates an AccountAddress from a 32-byte array.
     *
     * <p>The byte array must be exactly 32 bytes long. This method creates
     * a defensive copy to prevent external modification of the address data.</p>
     *
     * @param address the 32-byte address data
     * @return a new AccountAddress instance
     * @throws IllegalArgumentException if the array is not exactly 32 bytes
     */
    public static AccountAddress fromBytes(byte[] address) {
        return new AccountAddress(address);
    }

    /**
     * Creates an AccountAddress from a hexadecimal string.
     *
     * <p>The hex string can optionally start with '0x' prefix, which will be
     * automatically stripped. The hex string must represent exactly 32 bytes
     * (64 hex characters after prefix removal). Leading zeros must be included
     * to reach the full 32-byte length.</p>
     *
     * @param hex the hexadecimal representation of the address
     * @return a new AccountAddress instance
     * @throws IllegalArgumentException if the hex string doesn't represent 32 bytes
     * @throws NumberFormatException    if the string contains invalid hex characters
     */
    public static AccountAddress fromHex(String hex) {
        if (hex.startsWith("0x")) {
            hex = hex.substring(2);
        }
        byte[] bytes = HexUtils.hexToBytes(hex);
        return fromBytes(bytes);
    }

    /**
     * Creates the special zero address (all bytes are 0).
     *
     * <p>The zero address (0x0000...0000) is a special system address used
     * in various blockchain operations. It represents 'no address' or is
     * used as a placeholder in certain contexts.</p>
     *
     * @return a new AccountAddress representing the zero address
     */
    public static AccountAddress zero() {
        return new AccountAddress(new byte[LENGTH]);
    }

    /**
     * Creates an AccountAddress from a public key.
     *
     * <p><strong>Note:</strong> This is a simplified implementation for compatibility.
     * In practice, Aptos addresses are derived from authentication keys which
     * are computed using SHA3-256 hash of the public key with scheme identifier.
     * This method assumes the public key is already in the correct 32-byte format.</p>
     *
     * @param publicKey the public key bytes (must be 32 bytes)
     * @return a new AccountAddress derived from the public key
     * @throws IllegalArgumentException if the public key is not 32 bytes
     * @deprecated Use proper authentication key derivation instead
     */
    public static AccountAddress fromPublicKey(byte[] publicKey) {
        // In Aptos, the account address is derived from the authentication key
        // For Ed25519, the authentication key is the same as the public key
        return fromBytes(publicKey);
    }

    /**
     * Returns the address as a byte array.
     *
     * <p>This method returns a defensive copy of the address bytes to prevent
     * external modification. The returned array is always exactly 32 bytes long.</p>
     *
     * @return a copy of the 32-byte address data
     */
    public byte[] toBytes() {
        return Arrays.copyOf(address, address.length);
    }

    /**
     * Returns the address as a hexadecimal string without '0x' prefix.
     *
     * <p>The returned string is lowercase and exactly 64 characters long,
     * representing the 32 bytes of the address in hexadecimal format.
     * Leading zeros are preserved to maintain the full length.</p>
     *
     * @return the address as a lowercase hex string (no '0x' prefix)
     */
    public String toHexString() {
        return HexUtils.bytesToHex(address);
    }

    /**
     * Returns the address as a hexadecimal string with '0x' prefix.
     *
     * <p>This method provides the standard blockchain representation of the
     * address with the '0x' prefix indicating hexadecimal format. This is
     * the preferred format for displaying addresses to users.</p>
     *
     * @return the address as a hex string with '0x' prefix
     */
    public String toString() {
        return "0x" + toHexString();
    }

    /**
     * Checks whether this is the zero address (all bytes are 0).
     *
     * <p>The zero address has special meaning in blockchain systems and
     * this method provides a convenient way to test for it.</p>
     *
     * @return true if this is the zero address, false otherwise
     */
    public boolean isZero() {
        for (byte b : address) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AccountAddress that = (AccountAddress) obj;
        return Arrays.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(address);
    }
}
