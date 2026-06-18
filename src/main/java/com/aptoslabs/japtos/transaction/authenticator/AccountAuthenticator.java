package com.aptoslabs.japtos.transaction.authenticator;

import com.aptoslabs.japtos.bcs.Serializable;

/**
 * Interface defining the contract for account authenticators in Aptos transactions.
 *
 * <p>Account authenticators provide the cryptographic proof that a transaction
 * was authorized by the account owner. They encapsulate the public key, signature,
 * and authentication key information needed to verify transaction authenticity.</p>
 *
 * <p>Different authenticator implementations support various signature schemes:</p>
 * <ul>
 *   <li><strong>Ed25519Authenticator</strong> - Single Ed25519 signature</li>
 *   <li><strong>MultiEd25519Authenticator</strong> - Multi-signature with threshold</li>
 * </ul>
 *
 * <p>The authenticator is created during the transaction signing process and
 * contains all the information needed by the network to verify that the
 * transaction was properly authorized by the sending account.</p>
 *
 * <p>Authentication Flow:</p>
 * <ol>
 *   <li>Account signs a RawTransaction with private key(s)</li>
 *   <li>Authenticator is created with signature(s) and public key(s)</li>
 *   <li>SignedTransaction combines RawTransaction + AccountAuthenticator</li>
 *   <li>Network uses authenticator to verify transaction signatures</li>
 * </ol>
 *
 * @see Ed25519Authenticator
 * @see MultiEd25519Authenticator
 * @see SignedTransaction
 * @see Serializable
 * @since 1.0.0
 */
public interface AccountAuthenticator extends Serializable {

    /**
     * Returns the authentication key for this authenticator.
     *
     * <p>The authentication key is a cryptographic hash derived from the public key(s)
     * and is used to derive the account address. This key serves as a stable identifier
     * for the account regardless of the specific signature scheme used.</p>
     *
     * @return the authentication key as a byte array
     */
    byte[] getAuthenticationKey();

    /**
     * Returns the public key(s) for this authenticator.
     *
     * <p>The public key is used by the network to verify the signature(s) provided
     * in this authenticator. For single-signature schemes, this returns a single
     * public key. For multi-signature schemes, this may return a serialized
     * representation of multiple public keys.</p>
     *
     * @return the public key data as a byte array
     */
    byte[] getPublicKey();

    /**
     * Returns the signature(s) for this authenticator.
     *
     * <p>The signature provides cryptographic proof that the transaction was
     * authorized by the holder(s) of the corresponding private key(s). The format
     * and content depend on the specific signature scheme (Ed25519, MultiEd25519, etc.).</p>
     *
     * @return the signature data as a byte array
     */
    byte[] getSignature();
}
