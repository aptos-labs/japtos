package com.aptoslabs.japtos.account;

import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.core.crypto.Ed25519PrivateKey;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.core.crypto.Signature;
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.transaction.authenticator.AccountAuthenticator;
import com.aptoslabs.japtos.transaction.authenticator.Ed25519Authenticator;

/**
 * Represents an Ed25519 account.

 */
public class Ed25519Account extends Account {
    private final Ed25519PrivateKey privateKey;
    private final Ed25519PublicKey publicKey;
    private final AccountAddress accountAddress;
    
    Ed25519Account(Ed25519PrivateKey privateKey, AccountAddress address) {
        this.privateKey = privateKey;
        this.publicKey = privateKey.publicKey();
        this.accountAddress = address != null ? address : publicKey.accountAddress();
    }
    
    /**
     * Generate a new Ed25519 account
     */
    public static Ed25519Account generate() {
        return generate(new GenerateEd25519AccountArgs());
    }
    
    /**
     * Generate a new Ed25519 account with specific parameters
     */
    public static Ed25519Account generate(GenerateEd25519AccountArgs args) {
        Ed25519PrivateKey privateKey = Ed25519PrivateKey.generate();
        return new Ed25519Account(privateKey, null);
    }
    
    /**
     * Create an Ed25519 account from a private key
     */
    public static Ed25519Account fromPrivateKey(Ed25519PrivateKey privateKey) {
        return new Ed25519Account(privateKey, null);
    }
    
    /**
     * Create an Ed25519 account from a private key hex string
     */
    public static Ed25519Account fromPrivateKeyHex(String privateKeyHex) {
        Ed25519PrivateKey privateKey = Ed25519PrivateKey.fromHex(privateKeyHex);
        return fromPrivateKey(privateKey);
    }
    
    /**
     * Creates an Ed25519 account from a BIP44 derivation path and mnemonic seed phrase.
     * 
     * <p>This method generates an Ed25519Account using hierarchical deterministic
     * key derivation based on the BIP44 standard. The path must follow the format:
     * m/44'/637'/account'/change'/address_index' where all components are hardened.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * String mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
     * String path = "m/44'/637'/0'/0'/0'";
     * Ed25519Account account = Ed25519Account.fromDerivationPath(path, mnemonic);
     * }</pre>
     * 
     * @param path the BIP44 derivation path (e.g., "m/44'/637'/0'/0'/0'")
     * @param mnemonic the mnemonic seed phrase (12 or 24 words)
     * @return a new Ed25519Account derived from the path and mnemonic
     * @throws IllegalArgumentException if the path format is invalid
     * @throws RuntimeException if key derivation fails
     * @see Ed25519PrivateKey#fromDerivationPath(String, String)
     */
    public static Ed25519Account fromDerivationPath(String path, String mnemonic) {
        Ed25519PrivateKey privateKey = Ed25519PrivateKey.fromDerivationPath(path, mnemonic);
        return new Ed25519Account(privateKey, null);
    }
    
    @Override
    public Ed25519PublicKey getPublicKey() {
        return publicKey;
    }
    
    @Override
    public AccountAddress getAccountAddress() {
        return accountAddress;
    }
    
    @Override
    public SigningScheme getSigningScheme() {
        return SigningScheme.ED25519;
    }
    
    @Override
    public AccountAuthenticator signWithAuthenticator(byte[] message) {
        Signature signature = sign(message);
        return new Ed25519Authenticator(publicKey, signature);
    }
    
    @Override
    public AccountAuthenticator signTransactionWithAuthenticator(RawTransaction transaction) throws Exception {
        Signature signature = signTransaction(transaction);
        return new Ed25519Authenticator(publicKey, signature);
    }
    
    @Override
    public Signature sign(byte[] message) {
        return privateKey.sign(message);
    }
    
    @Override
    public Signature signTransaction(RawTransaction transaction) throws Exception {
        // Aptos signing message: sha3("APTOS::RawTransaction") || BCS(RawTransaction)
        byte[] domain = "APTOS::RawTransaction".getBytes();
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA3-256");
        byte[] prefixHash = digest.digest(domain);
        byte[] txnBytes = transaction.bcsToBytes();
        byte[] signingMessage = new byte[prefixHash.length + txnBytes.length];
        System.arraycopy(prefixHash, 0, signingMessage, 0, prefixHash.length);
        System.arraycopy(txnBytes, 0, signingMessage, prefixHash.length, txnBytes.length);
        
        // Sign the signing message directly (no extra hash)
        return privateKey.sign(signingMessage);
    }
    
    /**
     * Hash a message using SHA3-256
     */
    private byte[] hashMessage(byte[] message) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA3-256");
            return digest.digest(message);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA3-256 not available", e);
        }
    }
    
    /**
     * Get the private key
     */
    public Ed25519PrivateKey getPrivateKey() {
        return privateKey;
    }
    
    /**
     * Get private key as hex string
     */
    public String getPrivateKeyHex() {
        return privateKey.toString();
    }
    
    /**
     * Get public key as hex string
     */
    public String getPublicKeyHex() {
        return publicKey.toString();
    }
    
    @Override
    public String toString() {
        return "Ed25519Account{" +
            "address=" + accountAddress +
            ", publicKey=" + getPublicKeyHex() +
            '}';
    }
}
