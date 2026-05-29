package com.aptoslabs.japtos.account;

import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.core.AuthenticationKey;
import com.aptoslabs.japtos.core.crypto.Ed25519PrivateKey;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.core.crypto.Signature;
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.transaction.authenticator.AccountAuthenticator;
import com.aptoslabs.japtos.transaction.authenticator.Ed25519Authenticator;
import com.aptoslabs.japtos.transaction.authenticator.MultiEd25519Authenticator;
import com.aptoslabs.japtos.transaction.authenticator.MultiKeyAuthenticator;
import com.aptoslabs.japtos.types.EntryFunctionPayload;
import com.aptoslabs.japtos.types.Identifier;
import com.aptoslabs.japtos.types.ModuleId;
import com.aptoslabs.japtos.types.TransactionArgument;
import com.aptoslabs.japtos.types.TransactionPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Account} factory and its concrete subclasses.
 */
class AccountVariantsTest {

    private static final String PRIV_HEX =
            "9bf49a6a0755f953811fce125f2683d50429c3bb49e074147e0089a52eae155f";

    private RawTransaction raw(AccountAddress sender) {
        TransactionPayload payload = new EntryFunctionPayload(
                new ModuleId(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("coin")),
                new Identifier("transfer"),
                List.of(),
                List.of(new TransactionArgument.U64(1L)));
        return new RawTransaction(sender, 0L, payload, 100000L, 100L, 1700000000L, 4L);
    }

    @Test
    @DisplayName("Account.generate produces a usable Ed25519 account")
    void generate() throws Exception {
        Ed25519Account account = Account.generate();
        assertEquals(Account.SigningScheme.ED25519, account.getSigningScheme());
        assertNotNull(account.getAccountAddress());

        byte[] msg = "hello".getBytes();
        Signature sig = account.sign(msg);
        assertTrue(account.verifySignature(msg, sig));

        // generate(args) overload
        Ed25519Account legacy = Account.generate(new Account.GenerateEd25519AccountArgs(true));
        assertNotNull(legacy.getAccountAddress());
    }

    @Test
    @DisplayName("Ed25519Account.signTransaction verifies against the Aptos signing message")
    void ed25519SignTransaction() throws Exception {
        Ed25519Account account = Ed25519Account.fromPrivateKeyHex(PRIV_HEX);
        RawTransaction raw = raw(account.getAccountAddress());
        Signature sig = account.signTransaction(raw);

        byte[] domain = "APTOS::RawTransaction".getBytes();
        byte[] prefix = com.aptoslabs.japtos.utils.CryptoUtils.sha3_256(domain);
        byte[] txn = raw.bcsToBytes();
        byte[] signingMessage = new byte[prefix.length + txn.length];
        System.arraycopy(prefix, 0, signingMessage, 0, prefix.length);
        System.arraycopy(txn, 0, signingMessage, prefix.length, txn.length);
        assertTrue(account.verifySignature(signingMessage, sig));

        AccountAuthenticator auth = account.signTransactionWithAuthenticator(raw);
        assertTrue(auth instanceof Ed25519Authenticator);

        AccountAuthenticator msgAuth = account.signWithAuthenticator("m".getBytes());
        assertTrue(msgAuth instanceof Ed25519Authenticator);
    }

    @Test
    @DisplayName("Ed25519Account exposes hex helpers and a descriptive toString")
    void ed25519Accessors() {
        Ed25519Account account = Ed25519Account.fromPrivateKeyHex(PRIV_HEX);
        assertEquals("0x" + PRIV_HEX, account.getPrivateKeyHex());
        assertTrue(account.getPublicKeyHex().startsWith("0x"));
        assertNotNull(account.getPrivateKey());
        assertNotNull(account.getPublicKey());
        assertTrue(account.toString().contains("Ed25519Account"));

        // Custom-address constructor path via Account.fromPrivateKey(Ed25519 args)
        AccountAddress custom = AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000005");
        Ed25519Account withAddr = Account.fromPrivateKey(
                new Account.CreateEd25519AccountFromPrivateKeyArgs(
                        Ed25519PrivateKey.fromHex(PRIV_HEX), custom));
        assertEquals(custom, withAddr.getAccountAddress());
    }

    @Test
    @DisplayName("SingleKeyAccount signs with a SHA3-256 hashed message and verifies")
    void singleKeyAccount() throws Exception {
        SingleKeyAccount account = SingleKeyAccount.fromPrivateKeyHex(PRIV_HEX);
        assertEquals(Account.SigningScheme.ED25519, account.getSigningScheme());
        assertNotNull(account.getPrivateKey());
        assertEquals("0x" + PRIV_HEX, account.getPrivateKeyHex());
        assertTrue(account.getPublicKeyHex().startsWith("0x"));
        assertTrue(account.toString().contains("SingleKeyAccount"));

        RawTransaction raw = raw(account.getAccountAddress());
        Signature sig = account.signTransaction(raw);
        // SingleKeyAccount hashes prefix||txn with SHA3-256 then signs the hash.
        byte[] prefix = "APTOS::RawTransaction".getBytes();
        byte[] txn = raw.bcsToBytes();
        byte[] msg = new byte[prefix.length + txn.length];
        System.arraycopy(prefix, 0, msg, 0, prefix.length);
        System.arraycopy(txn, 0, msg, prefix.length, txn.length);
        byte[] hash = com.aptoslabs.japtos.utils.CryptoUtils.sha3_256(msg);
        assertTrue(account.verifySignature(hash, sig));

        assertTrue(account.signWithAuthenticator("m".getBytes()) instanceof Ed25519Authenticator);
        assertTrue(account.signTransactionWithAuthenticator(raw) instanceof Ed25519Authenticator);
    }

    @Test
    @DisplayName("Account.fromPrivateKey returns Ed25519Account for legacy and SingleKey otherwise")
    void factoryDispatch() {
        Ed25519PrivateKey priv = Ed25519PrivateKey.fromHex(PRIV_HEX);
        Account legacy = Account.fromPrivateKey(new Account.CreateAccountFromPrivateKeyArgs(priv));
        assertTrue(legacy instanceof Ed25519Account);

        Account modern = Account.fromPrivateKey(
                new Account.CreateAccountFromPrivateKeyArgs(priv, null, false));
        assertTrue(modern instanceof SingleKeyAccount);
    }

    @Test
    @DisplayName("Account.authKey delegates to the public key")
    void authKeyDelegates() {
        Ed25519PublicKey pub = Ed25519PrivateKey.fromHex(PRIV_HEX).publicKey();
        AuthenticationKey ak = Account.authKey(pub);
        assertEquals(pub.authKey(), ak);
    }

    @Test
    @DisplayName("Account.fromDerivationPath derives a deterministic account from a mnemonic")
    void fromDerivationPath() {
        String mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
        String path = "m/44'/637'/0'/0'/0'";
        Ed25519Account a = Account.fromDerivationPath(path, mnemonic);
        Ed25519Account b = Ed25519Account.fromDerivationPath(path, mnemonic);
        assertEquals(a.getAccountAddress(), b.getAccountAddress());
    }

    @Test
    @DisplayName("MultiEd25519Account derives a scheme-1 address and signs with the first key")
    void multiEd25519Account() throws Exception {
        Ed25519Account a1 = Account.generate();
        Ed25519Account a2 = Account.generate();
        List<Ed25519PublicKey> pubKeys = List.of(a1.getPublicKey(), a2.getPublicKey());

        MultiEd25519Account fromPriv = MultiEd25519Account.fromPrivateKeys(
                List.of(a1.getPrivateKey(), a2.getPrivateKey()), 2);
        assertEquals(2, fromPriv.getThreshold());
        assertEquals(List.of(0, 1), fromPriv.getSignerIndices());
        assertEquals(2, fromPriv.getPrivateKeys().size());
        assertEquals(2, fromPriv.getPublicKeys().size());
        assertEquals(Account.SigningScheme.MULTI_ED25519, fromPriv.getSigningScheme());

        MultiEd25519Account multi = MultiEd25519Account.from(List.of(a1), pubKeys, 1);
        assertNotNull(multi.getAccountAddress());
        assertEquals(a1.getPublicKey(), multi.getPublicKey());

        RawTransaction raw = raw(multi.getAccountAddress());
        assertTrue(multi.signTransactionWithAuthenticator(raw) instanceof MultiEd25519Authenticator);
        assertTrue(multi.signWithAuthenticator("m".getBytes()) instanceof MultiEd25519Authenticator);
        assertNotNull(multi.sign("m".getBytes()));
    }

    @Test
    @DisplayName("MultiEd25519Account.from validates signer/threshold/key constraints")
    void multiEd25519Validation() {
        Ed25519Account a1 = Account.generate();
        Ed25519Account a2 = Account.generate();
        List<Ed25519PublicKey> pubKeys = List.of(a1.getPublicKey(), a2.getPublicKey());

        assertThrows(IllegalArgumentException.class,
                () -> MultiEd25519Account.from(null, pubKeys, 1));
        assertThrows(IllegalArgumentException.class,
                () -> MultiEd25519Account.from(List.of(a1, a2), pubKeys, 1)); // signers > threshold
        assertThrows(IllegalArgumentException.class,
                () -> MultiEd25519Account.from(List.of(), pubKeys, 1));
        // Signer's key not in the public key set
        Ed25519Account stranger = Account.generate();
        assertThrows(IllegalArgumentException.class,
                () -> MultiEd25519Account.from(List.of(stranger), pubKeys, 1));
    }

    @Test
    @DisplayName("MultiKeyAccount derives a scheme-3 address and produces MultiKey authenticators")
    void multiKeyAccount() throws Exception {
        Ed25519Account a1 = Account.generate();
        Ed25519Account a2 = Account.generate();
        List<Ed25519PublicKey> pubKeys = List.of(a1.getPublicKey(), a2.getPublicKey());

        MultiKeyAccount fromPriv = MultiKeyAccount.fromPrivateKeys(
                List.of(a1.getPrivateKey(), a2.getPrivateKey()), 2);
        assertEquals(2, fromPriv.getThreshold());
        assertEquals(List.of(0, 1), fromPriv.getSignerIndices());
        assertEquals(2, fromPriv.getPrivateKeys().size());
        assertEquals(2, fromPriv.getPublicKeys().size());
        assertEquals(Account.SigningScheme.MULTI_KEY, fromPriv.getSigningScheme());

        MultiKeyAccount multi = MultiKeyAccount.from(List.of(a1), pubKeys, 1);
        assertNotNull(multi.getAccountAddress());
        assertEquals(a1.getPublicKey(), multi.getPublicKey());

        RawTransaction raw = raw(multi.getAccountAddress());
        assertTrue(multi.signTransactionWithAuthenticator(raw) instanceof MultiKeyAuthenticator);
        assertTrue(multi.signWithAuthenticator("m".getBytes()) instanceof MultiKeyAuthenticator);
        assertNotNull(multi.sign("m".getBytes()));
    }

    @Test
    @DisplayName("MultiKeyAccount.fromPublicKeysAndSigners validates its inputs")
    void multiKeyValidation() {
        Ed25519Account a1 = Account.generate();
        List<com.aptoslabs.japtos.core.crypto.PublicKey> pubKeys = List.of(a1.getPublicKey());

        assertThrows(IllegalArgumentException.class,
                () -> MultiKeyAccount.fromPublicKeysAndSigners(pubKeys, null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> MultiKeyAccount.fromPublicKeysAndSigners(pubKeys, List.of(), 1));
        assertThrows(IllegalArgumentException.class,
                () -> MultiKeyAccount.fromPublicKeysAndSigners(List.of(), List.of(a1), 1));
    }
}
