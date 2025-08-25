package com.aptoslabs.japtos.core.crypto;

import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.core.AuthenticationKey;

/**
 * Interface defining the contract for public keys in the Aptos cryptographic system.
 *
 * <p>This interface provides a common abstraction for different types of public keys
 * used in Aptos, including Ed25519, Multi-Ed25519, and other signature schemes.
 * Public keys are used for signature verification, account address derivation,
 * and authentication key generation.</p>
 *
 * <p>All public key implementations must provide methods for:</p>
 * <ul>
 *   <li>Binary and hexadecimal representation</li>
 *   <li>Authentication key derivation</li>
 *   <li>Account address computation</li>
 *   <li>Signature verification</li>
 * </ul>
 *
 * <p>The authentication key is a cryptographic hash derived from the public key
 * and is used to generate the account address on the Aptos blockchain.</p>
 *
 * @see Ed25519PublicKey
 * @see MultiEd25519PublicKey
 * @see AuthenticationKey
 * @see AccountAddress
 * @see Signature
 * @since 1.0.0
 */
public interface PublicKey {

    /**
     * Returns the public key as a byte array.
     *
     * <p>This method provides the raw binary representation of the public key,
     * which is used for cryptographic operations and serialization. The exact
     * format and length depend on the specific key type.</p>
     *
     * @return the public key bytes
     */
    byte[] toBytes();

    /**
     * Returns the public key as a hexadecimal string without the '0x' prefix.
     *
     * <p>This method provides a human-readable representation of the public key
     * in lowercase hexadecimal format. This format is commonly used in APIs
     * and configuration files.</p>
     *
     * @return the public key as a hex string (lowercase, no '0x' prefix)
     */
    String toHexString();

    /**
     * Returns the public key as a hexadecimal string with the '0x' prefix.
     *
     * <p>This method provides a standard blockchain-style representation of
     * the public key. The '0x' prefix indicates that the following characters
     * represent a hexadecimal value.</p>
     *
     * @return the public key as a hex string with '0x' prefix
     */
    String toString();

    /**
     * Derives the authentication key from this public key.
     *
     * <p>The authentication key is a cryptographic hash of the public key
     * that is used to generate the account address. The specific hash
     * algorithm and format depend on the key type (e.g., Ed25519 uses
     * SHA3-256 with a specific suffix).</p>
     *
     * @return the authentication key derived from this public key
     * @see AuthenticationKey
     */
    AuthenticationKey authKey();

    /**
     * Derives the account address from this public key.
     *
     * <p>This is a convenience method that derives the authentication key
     * from the public key and then computes the account address from that
     * authentication key. The account address is the primary identifier
     * for accounts on the Aptos blockchain.</p>
     *
     * @return the account address derived from this public key
     * @see AccountAddress
     * @see #authKey()
     */
    default AccountAddress accountAddress() {
        return authKey().accountAddress();
    }

    /**
     * Verifies a cryptographic signature against this public key.
     *
     * <p>This method checks whether the given signature was created by the
     * corresponding private key when signing the provided message. The
     * verification algorithm depends on the specific key and signature types.</p>
     *
     * @param message   the original message that was signed
     * @param signature the signature to verify
     * @return true if the signature is valid for the given message and this public key
     * @see Signature
     */
    boolean verifySignature(byte[] message, Signature signature);
}
