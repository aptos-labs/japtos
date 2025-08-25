package com.aptoslabs.japtos.transaction;

import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.transaction.authenticator.*;

import java.io.IOException;

/**
 * Represents a signed Aptos transaction ready for submission to the blockchain.
 *
 * <p>A SignedTransaction combines a RawTransaction with its corresponding
 * cryptographic signature(s) and authentication information. This is the final
 * form of a transaction that can be submitted to the Aptos network for execution.</p>
 *
 * <p>The class supports multiple authentication schemes:</p>
 * <ul>
 *   <li><strong>Ed25519</strong> - Single signature using Ed25519 cryptography</li>
 *   <li><strong>MultiEd25519</strong> - Multi-signature with threshold requirements</li>
 * </ul>
 *
 * <p>Transaction Flow:</p>
 * <ol>
 *   <li>Create a RawTransaction with all transaction parameters</li>
 *   <li>Sign the RawTransaction using appropriate private key(s)</li>
 *   <li>Create SignedTransaction with raw transaction + authenticator</li>
 *   <li>Serialize and submit to the Aptos network</li>
 *   <li>Network validates signature and executes transaction</li>
 * </ol>
 *
 * <p>The SignedTransaction implements BCS serialization to produce the canonical
 * byte representation that is transmitted to the blockchain network. The
 * serialization includes both the raw transaction data and the authentication
 * information required for signature verification.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create raw transaction
 * RawTransaction rawTx = new RawTransaction(sender, seqNum, payload, ...);
 *
 * // Sign with Ed25519 account
 * Ed25519Account account = // ... obtain account
 * AccountAuthenticator auth = account.signTransactionWithAuthenticator(rawTx);
 * SignedTransaction signedTx = new SignedTransaction(rawTx, auth);
 *
 * // Submit to network
 * byte[] txBytes = serializer.serialize(signedTx);
 * aptosClient.submitTransaction(txBytes);
 * }</pre>
 *
 * @see RawTransaction
 * @see AccountAuthenticator
 * @see Ed25519Authenticator
 * @see MultiEd25519Authenticator
 * @see Serializable
 * @since 1.0.0
 */
public class SignedTransaction implements Serializable {
    private final RawTransaction rawTransaction;
    private final Serializable authenticator; // Can be either Ed25519 or MultiEd25519

    /**
     * Creates a new SignedTransaction with the specified raw transaction and authenticator.
     *
     * <p>This constructor converts the provided AccountAuthenticator into the appropriate
     * internal transaction authenticator format for serialization. It supports both
     * Ed25519 and MultiEd25519 authentication schemes.</p>
     *
     * @param rawTransaction the raw (unsigned) transaction to be signed
     * @param authenticator  the account authenticator containing signature and public key info
     * @throws IllegalArgumentException if the authenticator type is not supported
     */
    public SignedTransaction(RawTransaction rawTransaction, AccountAuthenticator authenticator) {
        this.rawTransaction = rawTransaction;
        // Extract the appropriate transaction authenticator from the AccountAuthenticator
        if (authenticator instanceof Ed25519Authenticator) {
            Ed25519Authenticator ed25519Auth = (Ed25519Authenticator) authenticator;
            this.authenticator = new TransactionAuthenticatorEd25519(
                    ed25519Auth.getPublicKeyObject(),
                    ed25519Auth.getSignatureObject()
            );
        } else if (authenticator instanceof MultiEd25519Authenticator) {
            MultiEd25519Authenticator multiAuth = (MultiEd25519Authenticator) authenticator;
            this.authenticator = new TransactionAuthenticatorMultiEd25519(
                    multiAuth.getPublicKeys(),
                    multiAuth.getSignatureObject(),
                    multiAuth.getThreshold()
            );
        } else if (authenticator instanceof MultiKeyAuthenticator) {
            MultiKeyAuthenticator multiKeyAuth = (MultiKeyAuthenticator) authenticator;
            // Wrap the AccountAuthenticator(MultiKey) inside TransactionAuthenticator.SingleSender(4)
            this.authenticator = new com.aptoslabs.japtos.transaction.authenticator.TransactionAuthenticatorSingleSender(multiKeyAuth);
        } else {
            throw new IllegalArgumentException("Unsupported authenticator type: " + authenticator.getClass().getName());
        }
    }

    /**
     * Serializes this SignedTransaction using BCS (Binary Canonical Serialization).
     *
     * <p>The serialization format includes:
     * <ol>
     *   <li>Raw transaction data (all transaction parameters)</li>
     *   <li>Transaction authenticator (signatures and public keys)</li>
     * </ol>
     *
     * <p>This serialized form is what gets submitted to the Aptos network.</p>
     *
     * @param serializer the BCS serializer to write to
     * @throws IOException if serialization fails
     */
    @Override
    public void serialize(Serializer serializer) throws IOException {
        rawTransaction.serialize(serializer);
        authenticator.serialize(serializer);
    }

    /**
     * Returns the raw transaction component of this signed transaction.
     *
     * <p>The raw transaction contains all the transaction parameters including
     * sender, sequence number, payload, gas parameters, expiration, and chain ID.</p>
     *
     * @return the raw transaction
     */
    public RawTransaction getRawTransaction() {
        return rawTransaction;
    }

    /**
     * Returns the account authenticator for this signed transaction.
     *
     * <p>The authenticator contains the cryptographic signatures and public key
     * information needed to verify the transaction's authenticity. This method
     * converts the internal transaction authenticator back to the standard
     * AccountAuthenticator interface for compatibility.</p>
     *
     * @return the account authenticator containing signatures and public keys
     * @throws IllegalStateException if the internal authenticator type is unknown
     */
    public AccountAuthenticator getAuthenticator() {
        // Convert back to AccountAuthenticator for compatibility
        if (authenticator instanceof TransactionAuthenticatorEd25519) {
            TransactionAuthenticatorEd25519 ed25519Auth = (TransactionAuthenticatorEd25519) authenticator;
            return new Ed25519Authenticator(ed25519Auth.getPublicKey(), ed25519Auth.getSignature());
        } else if (authenticator instanceof TransactionAuthenticatorMultiEd25519) {
            TransactionAuthenticatorMultiEd25519 multiAuth = (TransactionAuthenticatorMultiEd25519) authenticator;
            return new MultiEd25519Authenticator(multiAuth.getPublicKeys(), multiAuth.getSignature(), multiAuth.getThreshold());
        } else {
            throw new IllegalStateException("Unknown authenticator type");
        }
    }

    @Override
    public String toString() {
        return "SignedTransaction{" +
                "rawTransaction=" + rawTransaction +
                ", authenticator=" + authenticator +
                '}';
    }
} 