package com.aptoslabs.japtos;

import com.aptoslabs.japtos.account.Account;
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.account.MultiEd25519Account;
import com.aptoslabs.japtos.account.MultiKeyAccount;
import com.aptoslabs.japtos.api.AptosConfig;
import com.aptoslabs.japtos.client.AptosClient;
import com.aptoslabs.japtos.client.dto.PendingTransaction;
import com.aptoslabs.japtos.client.dto.Transaction;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.transaction.SignedTransaction;
import com.aptoslabs.japtos.transaction.authenticator.AccountAuthenticator;
import com.aptoslabs.japtos.types.*;
import com.aptoslabs.japtos.utils.FundingUtils;
import com.aptoslabs.japtos.utils.TestConfig;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.junit.jupiter.api.*;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MultiKeyAccountTests {
    private AptosClient client;
    private AccountAddress receiver;
    private AptosConfig config;

    @BeforeAll
    void setup() {
        config = AptosConfig.builder().network(AptosConfig.Network.LOCALNET).build();
        client = new AptosClient(config);

        // Create a random receiver for all tests
        Ed25519Account receiverAccount = Ed25519Account.generate();
        receiver = receiverAccount.getAccountAddress();
        System.out.println("Test receiver address: " + receiver);
    }

    @Test
    @Order(1)
    @DisplayName("1-of-2 MultiKeyAccount: fund and transfer (LOCALNET)")
    void localnetMultiKeyAccountTest() throws Exception {
        System.out.println("=== Testing MultiKeyAccount on LOCALNET ===");

        // Generate two random accounts
        Ed25519Account account1 = Ed25519Account.generate();
        Ed25519Account account2 = Ed25519Account.generate();

        System.out.println("Generated random accounts:");
        System.out.println("   Account 1: " + account1.getAccountAddress());
        System.out.println("   Account 2: " + account2.getAccountAddress());

        // Set up the multi-signature configuration (1-of-2)
        List<Account> signers = List.of(account1);
        List<Ed25519PublicKey> publicKeys = Arrays.asList(
                account1.getPublicKey(),
                account2.getPublicKey()
        );

        int threshold = 1;

        // Create the multi-signature account using MultiKeyAccount
        MultiKeyAccount multiKey = MultiKeyAccount.from(signers, publicKeys, threshold);

        System.out.println("MultiKeyAccount created:");
        System.out.println("   - Address: " + multiKey.getAccountAddress());
        System.out.println("   - Number of signers: " + signers.size());
        System.out.println("   - Number of public keys: " + publicKeys.size());
        System.out.println("   - Threshold: " + threshold);
        System.out.println("   - Scheme: MultiKey (3)");

        // Fund the account
        System.out.println("Funding MultiKeyAccount...");
        String fundHash = FundingUtils.fundAccount(multiKey.getAccountAddress().toString(), TestConfig.FUND_AMOUNT, config.getNetwork());
        assertNotNull(fundHash);
        Thread.sleep(TestConfig.FUNDING_DELAY_MS);

        // Check initial balance
        long initialBalance = client.getAccountCoinAmount(multiKey.getAccountAddress());
        System.out.println("   Initial balance: " + initialBalance + " octas");

        // Build transfer payload
        ModuleId moduleId = new ModuleId(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("coin"));
        Identifier functionName = new Identifier("transfer");
        TransactionPayload payload = new EntryFunctionPayload(
                moduleId,
                functionName,
                java.util.List.of(new TypeTag.Struct(new StructTag(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("aptos_coin"), new Identifier("AptosCoin"), java.util.List.of()))),
                java.util.List.of(
                        new TransactionArgument.AccountAddress(receiver),
                        new TransactionArgument.U64(TestConfig.SMALL_TRANSFER)
                )
        );

        // Get current sequence number
        long sequenceNumber = client.getNextSequenceNumber(multiKey.getAccountAddress());
        System.out.println("   Current sequence number: " + sequenceNumber);

        // Build raw transaction
        RawTransaction raw = new RawTransaction(
                multiKey.getAccountAddress(),
                sequenceNumber,
                payload,
                TestConfig.DEFAULT_MAX_GAS,
                TestConfig.DEFAULT_GAS_PRICE,
                (System.currentTimeMillis() / 1000L) + 3600,
                config.getNetwork().getChainId()
        );

        // Sign with multi-sig
        System.out.println("Signing transaction with MultiKeyAccount...");
        AccountAuthenticator authenticator = multiKey.signTransactionWithAuthenticator(raw);
        SignedTransaction signed = new SignedTransaction(raw, authenticator);

        // Verify we can serialize the transaction
        byte[] transactionBytes = signed.bcsToBytes();
        assertTrue(transactionBytes.length > 0);
        System.out.println("   Transaction serialized successfully (" + transactionBytes.length + " bytes)");

        // Submit the transaction
        System.out.println("Submitting MultiKeyAccount transaction...");
        try {
            PendingTransaction pending = client.submitTransaction(signed);
            assertNotNull(pending);
            System.out.println("   ✅ MultiKeyAccount transaction submitted! Hash: " + pending.getHash());

            // Wait for transaction to be committed
            Transaction committed = client.waitForTransaction(pending.getHash());
            assertNotNull(committed);
            System.out.println("   ✅ MultiKeyAccount transaction committed successfully!");

            // Check final balance
            long finalBalance = client.getAccountCoinAmount(multiKey.getAccountAddress());
            System.out.println("   Final balance: " + finalBalance + " octas");
            System.out.println("   Balance change: " + (finalBalance - initialBalance) + " octas");

        } catch (Exception e) {
            System.out.println("   ❌ MultiKeyAccount transaction failed: " + e.getMessage());
            if (e.getMessage().contains("invalid value: integer")) {
                System.out.println("   ⚠️  Got serialization variant error - MultiKey scheme not supported");
            }
            throw e;
        }
    }

    @Test
    @Order(2)
    @DisplayName("1-of-2 MultiEd25519Account: fund and transfer (LOCALNET)")
    void localnetMultiEd25519AccountTest() throws Exception {
        System.out.println("=== Testing MultiEd25519Account on LOCALNET ===");

        // Generate two random accounts (same setup as MultiKey test)
        Ed25519Account account1 = Ed25519Account.generate();
        Ed25519Account account2 = Ed25519Account.generate();

        System.out.println("Generated random accounts:");
        System.out.println("   Account 1: " + account1.getAccountAddress());
        System.out.println("   Account 2: " + account2.getAccountAddress());

        // Set up the multi-signature configuration (1-of-2)
        List<Account> signers = List.of(account1);
        List<Ed25519PublicKey> publicKeys = Arrays.asList(
                account1.getPublicKey(),
                account2.getPublicKey()
        );

        int threshold = 1;

        // Create the multi-signature account using MultiEd25519Account (scheme 1)
        MultiEd25519Account multiEd25519 = MultiEd25519Account.from(signers, publicKeys, threshold);

        System.out.println("MultiEd25519Account created:");
        System.out.println("   - Address: " + multiEd25519.getAccountAddress());
        System.out.println("   - Number of signers: " + signers.size());
        System.out.println("   - Number of public keys: " + publicKeys.size());
        System.out.println("   - Threshold: " + threshold);
        System.out.println("   - Scheme: MultiEd25519 (1)");

        // Fund the account
        System.out.println("Funding MultiEd25519Account...");
        String fundHash = FundingUtils.fundAccount(multiEd25519.getAccountAddress().toString(), TestConfig.FUND_AMOUNT, config.getNetwork());
        assertNotNull(fundHash);
        Thread.sleep(TestConfig.FUNDING_DELAY_MS);

        // Check initial balance
        long initialBalance = client.getAccountCoinAmount(multiEd25519.getAccountAddress());
        System.out.println("   Initial balance: " + initialBalance + " octas");

        // Build transfer payload
        ModuleId moduleId = new ModuleId(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("coin"));
        Identifier functionName = new Identifier("transfer");
        TransactionPayload payload = new EntryFunctionPayload(
                moduleId,
                functionName,
                java.util.List.of(new TypeTag.Struct(new StructTag(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("aptos_coin"), new Identifier("AptosCoin"), java.util.List.of()))),
                java.util.List.of(
                        new TransactionArgument.AccountAddress(receiver),
                        new TransactionArgument.U64(TestConfig.SMALL_TRANSFER)
                )
        );

        // Get current sequence number
        long sequenceNumber = client.getNextSequenceNumber(multiEd25519.getAccountAddress());
        System.out.println("   Current sequence number: " + sequenceNumber);

        // Build raw transaction
        RawTransaction raw = new RawTransaction(
                multiEd25519.getAccountAddress(),
                sequenceNumber,
                payload,
                TestConfig.DEFAULT_MAX_GAS,
                TestConfig.DEFAULT_GAS_PRICE,
                (System.currentTimeMillis() / 1000L) + 3600,
                config.getNetwork().getChainId()
        );

        // Sign with multi-sig
        System.out.println("Signing transaction with MultiEd25519Account...");
        AccountAuthenticator authenticator = multiEd25519.signTransactionWithAuthenticator(raw);
        SignedTransaction signed = new SignedTransaction(raw, authenticator);

        // Verify we can serialize the transaction
        byte[] transactionBytes = signed.bcsToBytes();
        assertTrue(transactionBytes.length > 0);
        System.out.println("   Transaction serialized successfully (" + transactionBytes.length + " bytes)");

        // Submit the transaction
        System.out.println("Submitting MultiEd25519Account transaction...");
        try {
            PendingTransaction pending = client.submitTransaction(signed);
            assertNotNull(pending);
            System.out.println("   ✅ MultiEd25519Account transaction submitted! Hash: " + pending.getHash());

            // Wait for transaction to be committed
            Transaction committed = client.waitForTransaction(pending.getHash());
            assertNotNull(committed);
            System.out.println("   ✅ MultiEd25519Account transaction committed successfully!");

            // Check final balance
            long finalBalance = client.getAccountCoinAmount(multiEd25519.getAccountAddress());
            System.out.println("   Final balance: " + finalBalance + " octas");
            System.out.println("   Balance change: " + (finalBalance - initialBalance) + " octas");

        } catch (Exception e) {
            System.out.println("   ❌ MultiEd25519Account transaction failed: " + e.getMessage());
            throw e;
        }
    }

    private KeyPairBytes generateKeyPair() {
        Ed25519KeyPairGenerator gen = new Ed25519KeyPairGenerator();
        gen.init(new Ed25519KeyGenerationParameters(new SecureRandom()));
        var kp = gen.generateKeyPair();
        var priv = (Ed25519PrivateKeyParameters) kp.getPrivate();
        var pub = (org.bouncycastle.crypto.params.Ed25519PublicKeyParameters) kp.getPublic();
        KeyPairBytes out = new KeyPairBytes();
        out.privateKey = priv.getEncoded();
        out.publicKey = pub.getEncoded();
        return out;
    }

    // Helper method to generate key pairs
    private static class KeyPairBytes {
        byte[] privateKey;
        byte[] publicKey;
    }
}