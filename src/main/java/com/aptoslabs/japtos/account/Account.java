package com.aptoslabs.japtos.account;

import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.core.AuthenticationKey;
import com.aptoslabs.japtos.core.crypto.Ed25519PrivateKey;
import com.aptoslabs.japtos.core.crypto.PublicKey;
import com.aptoslabs.japtos.core.crypto.Signature;
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.transaction.SignedTransaction;
import com.aptoslabs.japtos.transaction.authenticator.AccountAuthenticator;

/**
 * Abstract base class representing a generic Aptos account.
 * 
 * <p>This class provides the foundational interface for interacting with accounts
 * on the Aptos blockchain. Accounts are cryptographic entities that can hold
 * resources, execute transactions, and interact with smart contracts.</p>
 * 
 * <p>The Account class supports multiple signing schemes including Ed25519,
 * Multi-Ed25519, and Multi-Key authentication. It provides both low-level
 * signing operations and higher-level transaction signing capabilities.</p>
 * 
 * <p>Key features include:</p>
 * <ul>
 *   <li>Transaction signing and verification</li>
 *   <li>Message signing with cryptographic proof</li>
 *   <li>Account address derivation from public keys</li>
 *   <li>Multiple signing scheme support</li>
 *   <li>Factory methods for account creation</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Generate a new Ed25519 account
 * Account account = Account.generate();
 * 
 * // Create account from existing private key
 * Ed25519PrivateKey privateKey = // ... obtain private key
 * Account account = Account.fromPrivateKey(
 *     new CreateAccountFromPrivateKeyArgs(privateKey)
 * );
 * 
 * // Sign a transaction
 * RawTransaction transaction = // ... create transaction
 * Signature signature = account.signTransaction(transaction);
 * }</pre>
 * 
 * @see Ed25519Account
 * @see SingleKeyAccount
 * @see AccountAddress
 * @see PublicKey
 * @since 1.0.0
 */
public abstract class Account {
    
    /**
     * Gets the public key associated with this account.
     * 
     * <p>The public key is used for signature verification and deriving
     * the account address. Different account types may use different
     * types of public keys (Ed25519, Multi-Ed25519, etc.).</p>
     * 
     * @return the public key for this account
     */
    public abstract PublicKey getPublicKey();
    
    /**
     * Gets the account address associated with this account.
     * 
     * <p>The account address is a unique 32-byte identifier derived from
     * the account's authentication key. It serves as the account's identity
     * on the Aptos blockchain and is used in all transactions and resource
     * operations.</p>
     * 
     * @return the account address for this account
     */
    public abstract AccountAddress getAccountAddress();
    
    /**
     * Gets the signing scheme used by this account.
     * 
     * <p>The signing scheme determines how signatures are generated and
     * verified for this account. Supported schemes include Ed25519 for
     * single-key accounts, Multi-Ed25519 for multi-signature accounts,
     * and Multi-Key for advanced authentication scenarios.</p>
     * 
     * @return the signing scheme for this account
     * @see SigningScheme
     */
    public abstract SigningScheme getSigningScheme();
    
    /**
     * Generates a new Ed25519 account with a randomly generated private key.
     * 
     * <p>This is the simplest way to create a new account for testing or
     * development purposes. The generated account uses the Ed25519 signature
     * scheme and creates a legacy-style authentication key by default.</p>
     * 
     * @return a new Ed25519Account with a randomly generated private key
     * @see Ed25519Account#generate()
     */
    public static Ed25519Account generate() {
        return Ed25519Account.generate();
    }
    
    /**
     * Generates a new Ed25519 account with specific parameters.
     * 
     * <p>This method allows customization of the account generation process,
     * such as specifying whether to use legacy authentication key derivation.</p>
     * 
     * @param args the generation parameters
     * @return a new Ed25519Account configured according to the provided arguments
     * @see Ed25519Account#generate(GenerateEd25519AccountArgs)
     * @see GenerateEd25519AccountArgs
     */
    public static Ed25519Account generate(GenerateEd25519AccountArgs args) {
        return Ed25519Account.generate(args);
    }
    
    /**
     * Creates an account from an existing private key.
     * 
     * <p>This factory method creates the appropriate account type based on the
     * private key type and legacy flag. For Ed25519 private keys with legacy=true,
     * it returns an Ed25519Account. Otherwise, it returns a SingleKeyAccount
     * which supports modern authentication schemes.</p>
     * 
     * @param args the parameters including private key, optional address, and legacy flag
     * @return an Account instance of the appropriate type
     * @see CreateAccountFromPrivateKeyArgs
     * @see Ed25519Account
     * @see SingleKeyAccount
     */
    public static Account fromPrivateKey(CreateAccountFromPrivateKeyArgs args) {
        if (args.privateKey instanceof Ed25519PrivateKey && args.legacy) {
            return new Ed25519Account(args.privateKey, args.address);
        }
        return new SingleKeyAccount(args.privateKey, args.address);
    }
    
    /**
     * Creates an Ed25519 account from an existing Ed25519 private key.
     * 
     * <p>This method specifically creates an Ed25519Account, which uses the
     * legacy Ed25519 authentication scheme. This is useful when you need
     * compatibility with older account formats or specific Ed25519 behavior.</p>
     * 
     * @param args the parameters including Ed25519 private key and optional address
     * @return a new Ed25519Account instance
     * @see CreateEd25519AccountFromPrivateKeyArgs
     * @see Ed25519Account
     */
    public static Ed25519Account fromPrivateKey(CreateEd25519AccountFromPrivateKeyArgs args) {
        return new Ed25519Account(args.privateKey, args.address);
    }
    
    /**
     * Creates an account from a BIP44 derivation path and mnemonic seed phrase.
     * 
     * <p>This method generates an Ed25519Account using hierarchical deterministic
     * key derivation based on the BIP44 standard. The path must follow the format:
     * m/44'/637'/account'/change'/address_index' where all components are hardened
     * (indicated by the apostrophe).</p>
     * 
     * <p>Aptos uses coin type 637 as defined in SLIP-0044. The derivation path
     * components have the following meanings:</p>
     * <ul>
     *   <li><strong>44'</strong> - BIP44 purpose (constant)</li>
     *   <li><strong>637'</strong> - Aptos coin type (constant)</li>
     *   <li><strong>account'</strong> - Account index (0', 1', 2', ...)</li>
     *   <li><strong>change'</strong> - Change index (typically 0')</li>
     *   <li><strong>address_index'</strong> - Address index (0', 1', 2', ...)</li>
     * </ul>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * String mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
     * String path = "m/44'/637'/0'/0'/0'";
     * Account account = Account.fromDerivationPath(path, mnemonic);
     * }</pre>
     * 
     * @param path the BIP44 derivation path (e.g., "m/44'/637'/0'/0'/0'")
     * @param mnemonic the mnemonic seed phrase (12 or 24 words)
     * @return a new Ed25519Account derived from the path and mnemonic
     * @throws IllegalArgumentException if the path format is invalid
     * @throws RuntimeException if key derivation fails
     * @see Ed25519Account
     * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki">BIP-44</a>
     * @see <a href="https://github.com/satoshilabs/slips/blob/master/slip-0044.md">SLIP-0044</a>
     */
    public static Ed25519Account fromDerivationPath(String path, String mnemonic) {
        return Ed25519Account.fromDerivationPath(path, mnemonic);
    }

    
    /**
     * Derives the authentication key for a given public key.
     * 
     * <p>The authentication key is a cryptographic hash derived from the public key
     * and is used to generate the account address. This is a utility method that
     * delegates to the public key's authKey() method.</p>
     * 
     * @param publicKey the public key to derive the authentication key from
     * @return the authentication key for the given public key
     * @see PublicKey#authKey()
     * @see AuthenticationKey
     */
    public static AuthenticationKey authKey(PublicKey publicKey) {
        return publicKey.authKey();
    }
    
    /**
     * Signs a message using the account's authenticator.
     * 
     * <p>This method creates a full AccountAuthenticator that includes both
     * the signature and the necessary public key information for verification.
     * This is typically used for authentication purposes rather than transaction signing.</p>
     * 
     * @param message the message bytes to sign
     * @return an AccountAuthenticator containing the signature and public key info
     * @see AccountAuthenticator
     */
    public abstract AccountAuthenticator signWithAuthenticator(byte[] message);
    
    /**
     * Signs a transaction using the account's authenticator.
     * 
     * <p>This method signs a RawTransaction and returns an AccountAuthenticator
     * that can be used to create a SignedTransaction. The authenticator includes
     * both the signature and the necessary authentication information.</p>
     * 
     * @param transaction the raw transaction to sign
     * @return an AccountAuthenticator for the signed transaction
     * @throws Exception if transaction signing fails
     * @see RawTransaction
     * @see AccountAuthenticator
     * @see SignedTransaction
     */
    public abstract AccountAuthenticator signTransactionWithAuthenticator(RawTransaction transaction) throws Exception;
    
    /**
     * Signs a message and returns just the signature.
     * 
     * <p>This method provides a lower-level signing interface that returns
     * only the cryptographic signature without additional authentication
     * metadata. Use this when you only need the signature itself.</p>
     * 
     * @param message the message bytes to sign
     * @return the cryptographic signature
     * @see Signature
     */
    public abstract Signature sign(byte[] message);
    
    /**
     * Signs a transaction and returns just the signature.
     * 
     * <p>This method provides a lower-level transaction signing interface
     * that returns only the cryptographic signature. For most use cases,
     * consider using signTransactionWithAuthenticator() instead.</p>
     * 
     * @param transaction the raw transaction to sign
     * @return the cryptographic signature
     * @throws Exception if transaction signing fails
     * @see RawTransaction
     * @see Signature
     */
    public abstract Signature signTransaction(RawTransaction transaction) throws Exception;
    
    /**
     * Verifies a signature against a message using this account's public key.
     * 
     * <p>This method checks whether the provided signature was created by
     * the private key corresponding to this account's public key when signing
     * the given message.</p>
     * 
     * @param message the original message that was signed
     * @param signature the signature to verify
     * @return true if the signature is valid for the message and public key, false otherwise
     * @see PublicKey#verifySignature(byte[], Signature)
     */
    public boolean verifySignature(byte[] message, Signature signature) {
        return getPublicKey().verifySignature(message, signature);
    }
    
    /**
     * Configuration arguments for creating an Ed25519 account from an existing private key.
     * 
     * <p>This class encapsulates the parameters needed to create an Ed25519Account
     * from an existing Ed25519PrivateKey. It supports optional account address
     * specification and legacy authentication key generation.</p>
     * 
     * <p>If no address is provided, it will be derived from the private key.
     * The legacy flag determines whether to use legacy Ed25519 authentication
     * key derivation (default: true).</p>
     * 
     * @see Ed25519Account
     * @see Ed25519PrivateKey
     * @see AccountAddress
     */
    public static class CreateEd25519AccountFromPrivateKeyArgs {
        public final Ed25519PrivateKey privateKey;
        public final AccountAddress address;
        public final boolean legacy;
        
        public CreateEd25519AccountFromPrivateKeyArgs(Ed25519PrivateKey privateKey, AccountAddress address, boolean legacy) {
            this.privateKey = privateKey;
            this.address = address;
            this.legacy = legacy;
        }
        
        public CreateEd25519AccountFromPrivateKeyArgs(Ed25519PrivateKey privateKey, AccountAddress address) {
            this(privateKey, address, true);
        }
        
        public CreateEd25519AccountFromPrivateKeyArgs(Ed25519PrivateKey privateKey) {
            this(privateKey, null, true);
        }
    }
    
    /**
     * Configuration arguments for creating an account from an existing private key.
     * 
     * <p>This class encapsulates the parameters needed to create an Account
     * from an existing private key. The actual account type created depends
     * on the private key type and the legacy flag setting.</p>
     * 
     * <p>If no address is provided, it will be derived from the private key.
     * The legacy flag determines the account type: true for Ed25519Account,
     * false for SingleKeyAccount (default: true).</p>
     * 
     * @see Account#fromPrivateKey(CreateAccountFromPrivateKeyArgs)
     * @see Ed25519Account
     * @see SingleKeyAccount
     */
    public static class CreateAccountFromPrivateKeyArgs {
        public final Ed25519PrivateKey privateKey;
        public final AccountAddress address;
        public final boolean legacy;
        
        public CreateAccountFromPrivateKeyArgs(Ed25519PrivateKey privateKey, AccountAddress address, boolean legacy) {
            this.privateKey = privateKey;
            this.address = address;
            this.legacy = legacy;
        }
        
        public CreateAccountFromPrivateKeyArgs(Ed25519PrivateKey privateKey, AccountAddress address) {
            this(privateKey, address, true);
        }
        
        public CreateAccountFromPrivateKeyArgs(Ed25519PrivateKey privateKey) {
            this(privateKey, null, true);
        }
    }
    
    /**
     * Configuration arguments for generating a new Ed25519 account.
     * 
     * <p>This class encapsulates the parameters that control how a new
     * Ed25519Account is generated. Currently, the main parameter is the
     * legacy flag which determines the authentication key derivation method.</p>
     * 
     * <p>When legacy is true (default), the account uses the traditional
     * Ed25519 authentication key derivation. When false, it uses the newer
     * single-key authentication scheme.</p>
     * 
     * @see Ed25519Account#generate(GenerateEd25519AccountArgs)
     */
    public static class GenerateEd25519AccountArgs {
        public final boolean legacy;
        
        public GenerateEd25519AccountArgs(boolean legacy) {
            this.legacy = legacy;
        }
        
        public GenerateEd25519AccountArgs() {
            this(true);
        }
    }
    
    /**
     * Enumeration of supported cryptographic signing schemes.
     * 
     * <p>This enum defines the different signature schemes supported by
     * Aptos accounts. Each scheme has different characteristics in terms
     * of security, performance, and multi-signature capabilities.</p>
     * 
     * <p>Supported schemes:</p>
     * <ul>
     *   <li><strong>ED25519</strong> - Single Ed25519 signature (legacy)</li>
     *   <li><strong>MULTI_ED25519</strong> - Multi-signature using Ed25519 keys</li>
     *   <li><strong>MULTI_KEY</strong> - Multi-signature with mixed key types</li>
     * </ul>
     * 
     * @see Ed25519Account
     * @see Account#getSigningScheme()
     */
    public enum SigningScheme {
        ED25519,
        MULTI_ED25519,
        MULTI_KEY
    }
} 