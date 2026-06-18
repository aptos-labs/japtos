package com.aptoslabs.japtos.transaction;

import com.aptoslabs.japtos.account.Account;
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.account.MultiEd25519Account;
import com.aptoslabs.japtos.account.MultiKeyAccount;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
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
 * Unit tests for {@link SignedTransaction}, covering each supported authenticator type
 * and the authenticator round-trip conversion.
 */
class SignedTransactionTest {

    private RawTransaction raw(AccountAddress sender) {
        TransactionPayload payload = new EntryFunctionPayload(
                new ModuleId(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("coin")),
                new Identifier("transfer"),
                List.of(),
                List.of(new TransactionArgument.U64(1L)));
        return new RawTransaction(sender, 0L, payload, 100000L, 100L, 1700000000L, 4L);
    }

    @Test
    @DisplayName("Ed25519 authenticator round-trips through SignedTransaction")
    void ed25519RoundTrip() throws Exception {
        Ed25519Account account = Account.generate();
        RawTransaction raw = raw(account.getAccountAddress());
        AccountAuthenticator auth = account.signTransactionWithAuthenticator(raw);
        SignedTransaction signed = new SignedTransaction(raw, auth);

        assertSame(raw, signed.getRawTransaction());
        assertTrue(signed.getAuthenticator() instanceof Ed25519Authenticator);
        assertTrue(signed.bcsToBytes().length > raw.bcsToBytes().length);
        assertTrue(signed.toString().contains("SignedTransaction"));
    }

    @Test
    @DisplayName("MultiEd25519 authenticator round-trips through SignedTransaction")
    void multiEd25519RoundTrip() throws Exception {
        Ed25519Account a1 = Account.generate();
        Ed25519Account a2 = Account.generate();
        List<Ed25519PublicKey> pubKeys = List.of(a1.getPublicKey(), a2.getPublicKey());
        MultiEd25519Account multi = MultiEd25519Account.from(List.of(a1), pubKeys, 1);

        RawTransaction raw = raw(multi.getAccountAddress());
        AccountAuthenticator auth = multi.signTransactionWithAuthenticator(raw);
        SignedTransaction signed = new SignedTransaction(raw, auth);

        assertTrue(signed.getAuthenticator() instanceof MultiEd25519Authenticator);
        assertTrue(signed.bcsToBytes().length > 0);
    }

    @Test
    @DisplayName("MultiKey authenticator is wrapped as a SingleSender and unwrapped on read")
    void multiKeyRoundTrip() throws Exception {
        Ed25519Account a1 = Account.generate();
        Ed25519Account a2 = Account.generate();
        List<Ed25519PublicKey> pubKeys = List.of(a1.getPublicKey(), a2.getPublicKey());
        MultiKeyAccount multi = MultiKeyAccount.from(List.of(a1), pubKeys, 1);

        RawTransaction raw = raw(multi.getAccountAddress());
        AccountAuthenticator auth = multi.signTransactionWithAuthenticator(raw);
        SignedTransaction signed = new SignedTransaction(raw, auth);

        // getAuthenticator unwraps the SingleSender back to the inner MultiKeyAuthenticator
        assertTrue(signed.getAuthenticator() instanceof MultiKeyAuthenticator);
        byte[] bytes = signed.bcsToBytes();
        // The transaction authenticator section begins with the SingleSender variant (4).
        byte[] rawBytes = raw.bcsToBytes();
        assertEquals(0x04, bytes[rawBytes.length]);
    }

    @Test
    @DisplayName("Unsupported authenticator types are rejected")
    void unsupportedAuthenticator() {
        Ed25519Account account = Account.generate();
        RawTransaction raw = raw(account.getAccountAddress());
        AccountAuthenticator bogus = new AccountAuthenticator() {
            public byte[] getAuthenticationKey() { return new byte[0]; }
            public byte[] getPublicKey() { return new byte[0]; }
            public byte[] getSignature() { return new byte[0]; }
            public void serialize(com.aptoslabs.japtos.bcs.Serializer s) { }
        };
        assertThrows(IllegalArgumentException.class, () -> new SignedTransaction(raw, bogus));
    }
}
