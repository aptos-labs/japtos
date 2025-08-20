package com.aptoslabs.japtos.account;

import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.core.crypto.Ed25519PrivateKey;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.core.crypto.Signature;
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.transaction.authenticator.AccountAuthenticator;
import com.aptoslabs.japtos.transaction.authenticator.Ed25519Authenticator;

/**
 * Represents a single key account.

 */
public class SingleKeyAccount extends Account {
    private final Ed25519PrivateKey privateKey;
    private final Ed25519PublicKey publicKey;
    private final AccountAddress accountAddress;
    
    SingleKeyAccount(Ed25519PrivateKey privateKey, AccountAddress address) {
        this.privateKey = privateKey;
        this.publicKey = privateKey.publicKey();
        this.accountAddress = address != null ? address : publicKey.accountAddress();
    }
    
    /**
     * Create a SingleKeyAccount from a private key
     */
    public static SingleKeyAccount fromPrivateKey(Ed25519PrivateKey privateKey) {
        return new SingleKeyAccount(privateKey, null);
    }
    
    /**
     * Create a SingleKeyAccount from a private key hex string
     */
    public static SingleKeyAccount fromPrivateKeyHex(String privateKeyHex) {
        Ed25519PrivateKey privateKey = Ed25519PrivateKey.fromHex(privateKeyHex);
        return fromPrivateKey(privateKey);
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
        return SigningScheme.ED25519; // SingleKeyAccount uses Ed25519 for now
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
        // Aptos requires a signing prefix for transactions
        byte[] prefix = "APTOS::RawTransaction".getBytes();
        byte[] transactionBytes = transaction.bcsToBytes();
        
        // Combine prefix and transaction bytes
        byte[] message = new byte[prefix.length + transactionBytes.length];
        System.arraycopy(prefix, 0, message, 0, prefix.length);
        System.arraycopy(transactionBytes, 0, message, prefix.length, transactionBytes.length);
        
        // Hash with SHA3-256
        byte[] hash = hashMessage(message);
        
        // Sign the hash
        return privateKey.sign(hash);
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
        return "SingleKeyAccount{" +
            "address=" + accountAddress +
            ", publicKey=" + getPublicKeyHex() +
            '}';
    }
}
