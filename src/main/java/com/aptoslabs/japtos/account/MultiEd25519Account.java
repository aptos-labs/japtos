package com.aptoslabs.japtos.account;

import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.core.AuthenticationKey;
import com.aptoslabs.japtos.core.crypto.Ed25519PrivateKey;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.core.crypto.Signature;
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.transaction.authenticator.AccountAuthenticator;
import com.aptoslabs.japtos.transaction.authenticator.MultiEd25519Authenticator;

import java.util.ArrayList;
import java.util.List;

/**
 * MultiEd25519 account implementation for multi-signature transactions.

 */
public class MultiEd25519Account extends Account {
    private final List<Ed25519PrivateKey> privateKeys;
    private final List<Ed25519PublicKey> publicKeys;
    private final int threshold;
    private final AccountAddress accountAddress;

    private MultiEd25519Account(List<Ed25519PrivateKey> privateKeys, List<Ed25519PublicKey> publicKeys, int threshold) {
        this.privateKeys = privateKeys;
        this.publicKeys = publicKeys;
        this.threshold = threshold;
        this.accountAddress = deriveAccountAddress();
    }

    public static MultiEd25519Account fromPrivateKeys(List<Ed25519PrivateKey> privateKeys, int threshold) {
        List<Ed25519PublicKey> publicKeys = new ArrayList<>();
        for (Ed25519PrivateKey privateKey : privateKeys) {
            publicKeys.add(privateKey.publicKey());
        }
        return new MultiEd25519Account(privateKeys, publicKeys, threshold);
    }

    public static MultiEd25519Account from(List<Account> signers, List<Ed25519PublicKey> publicKeys, int threshold) {
        // Validate inputs
        if (signers == null || publicKeys == null) {
            throw new IllegalArgumentException("Signers and public keys cannot be null");
        }
        if (signers.size() > threshold) {
            throw new IllegalArgumentException("Number of signers cannot exceed threshold");
        }
        if (signers.size() == 0) {
            throw new IllegalArgumentException("At least one signer is required");
        }
        if (publicKeys.size() == 0) {
            throw new IllegalArgumentException("At least one public key is required");
        }

        // Extract private keys from signers
        List<Ed25519PrivateKey> privateKeys = new ArrayList<>();
        for (Account signer : signers) {
            if (signer instanceof Ed25519Account) {
                Ed25519Account ed25519Account = (Ed25519Account) signer;
                privateKeys.add(ed25519Account.getPrivateKey());
            } else {
                throw new IllegalArgumentException("All signers must be Ed25519Account instances");
            }
        }

        // Use the provided public keys directly
        return new MultiEd25519Account(privateKeys, publicKeys, threshold);
    }

    @Override
    public Ed25519PublicKey getPublicKey() {
        // Return the first public key as the primary one
        return publicKeys.get(0);
    }

    @Override
    public AccountAddress getAccountAddress() {
        return accountAddress;
    }

    @Override
    public SigningScheme getSigningScheme() {
        return SigningScheme.MULTI_ED25519;
    }

    @Override
    public AccountAuthenticator signWithAuthenticator(byte[] message) {
        // For simplicity, sign with the first private key
        Signature signature = privateKeys.get(0).sign(message);
        return new MultiEd25519Authenticator(publicKeys, signature, threshold);
    }

    @Override
    public AccountAuthenticator signTransactionWithAuthenticator(RawTransaction transaction) throws Exception {
        // For simplicity, sign with the first private key
        Signature signature = signTransaction(transaction);
        return new MultiEd25519Authenticator(publicKeys, signature, threshold);
    }

    @Override
    public Signature sign(byte[] message) {
        // For simplicity, sign with the first private key
        return privateKeys.get(0).sign(message);
    }

    @Override
    public Signature signTransaction(RawTransaction transaction) throws Exception {
        // Match Ed25519Account.signTransaction: sha3("APTOS::RawTransaction") || BCS(RawTransaction)
        byte[] domain = "APTOS::RawTransaction".getBytes();
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA3-256");
        byte[] prefixHash = digest.digest(domain);
        byte[] txnBytes = transaction.bcsToBytes();
        byte[] signingMessage = new byte[prefixHash.length + txnBytes.length];
        System.arraycopy(prefixHash, 0, signingMessage, 0, prefixHash.length);
        System.arraycopy(txnBytes, 0, signingMessage, prefixHash.length, txnBytes.length);
        return privateKeys.get(0).sign(signingMessage);
    }

    private AccountAddress deriveAccountAddress() {
        // Derive auth key for MultiEd25519: sha3_256(concat(pubkeys..., threshold) || 0x01)
        int n = publicKeys.size();
        byte[] multiPk = new byte[n * 32 + 1];
        int off = 0;
        for (Ed25519PublicKey pk : publicKeys) {
            byte[] b = pk.toBytes();
            System.arraycopy(b, 0, multiPk, off, 32);
            off += 32;
        }
        multiPk[off] = (byte) (threshold & 0xFF);
        // Scheme 1 for MultiEd25519
        AuthenticationKey ak = AuthenticationKey.fromSchemeAndBytes((byte) 1, multiPk);
        return ak.accountAddress();
    }

    public List<Ed25519PrivateKey> getPrivateKeys() {
        return privateKeys;
    }

    public List<Ed25519PublicKey> getPublicKeys() {
        return publicKeys;
    }

    public int getThreshold() {
        return threshold;
    }
}
