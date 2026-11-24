package com.aptoslabs.japtos.account;

import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.core.crypto.SlhDsaSha2128sPrivateKey;
import com.aptoslabs.japtos.core.crypto.SlhDsaSha2128sPublicKey;
import com.aptoslabs.japtos.core.crypto.SlhDsaSha2128sSignature;
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.transaction.authenticator.AccountAuthenticator;
import com.aptoslabs.japtos.transaction.authenticator.SingleKeyAuthenticator;
import com.aptoslabs.japtos.utils.CryptoUtils;

import java.nio.charset.StandardCharsets;

/**
 * SingleKey account backed by an SLH-DSA-SHA2-128s (post-quantum, AIP-137)
 * key pair, mirroring {@code SingleKeyAccount} with
 * {@code SigningSchemeInput.SlhDsaSha2128s} in the TypeScript SDK.
 *
 * <p>The account address is the SingleKey authentication key of the
 * {@link com.aptoslabs.japtos.core.crypto.AnyPublicKey} wrapping (variant 5).
 * Signing produces a {@link SingleKeyAuthenticator}; when placed in a
 * {@link com.aptoslabs.japtos.transaction.SignedTransaction} it is wrapped in
 * TransactionAuthenticator SingleSender (variant 4), so single-sender,
 * multi-agent, and fee-payer flows accepting an {@link AccountAuthenticator}
 * work unchanged.</p>
 */
public class SlhDsaSha2128sAccount extends Account {
    private final SlhDsaSha2128sPrivateKey privateKey;
    private final SlhDsaSha2128sPublicKey publicKey;
    private final AccountAddress accountAddress;

    SlhDsaSha2128sAccount(SlhDsaSha2128sPrivateKey privateKey, AccountAddress address) {
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }
        this.privateKey = privateKey;
        this.publicKey = privateKey.publicKey();
        this.accountAddress = address != null ? address : publicKey.accountAddress();
    }

    /** Generates a new SLH-DSA-SHA2-128s account with a random 48-byte seed triple. */
    public static SlhDsaSha2128sAccount create() {
        return new SlhDsaSha2128sAccount(SlhDsaSha2128sPrivateKey.generate(), null);
    }

    public static SlhDsaSha2128sAccount fromPrivateKey(SlhDsaSha2128sPrivateKey privateKey) {
        return new SlhDsaSha2128sAccount(privateKey, null);
    }

    public static SlhDsaSha2128sAccount fromPrivateKey(SlhDsaSha2128sPrivateKey privateKey, AccountAddress address) {
        return new SlhDsaSha2128sAccount(privateKey, address);
    }

    public static SlhDsaSha2128sAccount fromPrivateKeyHex(String privateKeyHex) {
        return fromPrivateKey(SlhDsaSha2128sPrivateKey.fromHex(privateKeyHex));
    }

    /** Derives an SLH-DSA-SHA2-128s account from a hardened derivation path and mnemonic. */
    public static SlhDsaSha2128sAccount createFromDerivationPath(String path, String mnemonic) {
        return fromPrivateKey(SlhDsaSha2128sPrivateKey.fromDerivationPath(path, mnemonic));
    }

    @Override
    public SlhDsaSha2128sPublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public AccountAddress getAccountAddress() {
        return accountAddress;
    }

    @Override
    public SigningScheme getSigningScheme() {
        return SigningScheme.SLH_DSA_SHA2_128S;
    }

    @Override
    public AccountAuthenticator signWithAuthenticator(byte[] message) {
        return new SingleKeyAuthenticator(publicKey, sign(message));
    }

    @Override
    public AccountAuthenticator signTransactionWithAuthenticator(RawTransaction transaction) throws Exception {
        return new SingleKeyAuthenticator(publicKey, signTransaction(transaction));
    }

    @Override
    public SlhDsaSha2128sSignature sign(byte[] message) {
        return privateKey.sign(message);
    }

    @Override
    public SlhDsaSha2128sSignature signTransaction(RawTransaction transaction) throws Exception {
        byte[] domain = "APTOS::RawTransaction".getBytes(StandardCharsets.UTF_8);
        byte[] prefixHash = CryptoUtils.sha3_256(domain);
        byte[] txnBytes = transaction.bcsToBytes();
        byte[] signingMessage = new byte[prefixHash.length + txnBytes.length];
        System.arraycopy(prefixHash, 0, signingMessage, 0, prefixHash.length);
        System.arraycopy(txnBytes, 0, signingMessage, prefixHash.length, txnBytes.length);
        return privateKey.sign(signingMessage);
    }

    public SlhDsaSha2128sPrivateKey getPrivateKey() {
        return privateKey;
    }

    public String getPrivateKeyHex() {
        return privateKey.toString();
    }

    public String getPublicKeyHex() {
        return publicKey.toString();
    }

    @Override
    public String toString() {
        return "SlhDsaSha2128sAccount{address=" + accountAddress + ", publicKey=" + getPublicKeyHex() + '}';
    }
}
