package com.aptoslabs.japtos;

import com.aptoslabs.japtos.utils.Logger;

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
        Logger.info("Test receiver address: " + receiver);
    }

    @Test
    @Order(1)
    @DisplayName("1-of-2 MultiKeyAccount: fund and transfer (LOCALNET)")
    void localnetMultiKeyAccountTest() throws Exception {
        Logger.info("=== Testing MultiKeyAccount on LOCALNET ===");

        // Generate two random accounts
        Ed25519Account account1 = Ed25519Account.generate();
        Ed25519Account account2 = Ed25519Account.generate();

        Logger.info("Generated random accounts:");
        Logger.info("   Account 1: " + account1.getAccountAddress());
        Logger.info("   Account 2: " + account2.getAccountAddress());

        // Set up the multi-signature configuration (1-of-2)
        List<Account> signers = List.of(account1);
        List<Ed25519PublicKey> publicKeys = Arrays.asList(
                account1.getPublicKey(),
                account2.getPublicKey()
        );

        int threshold = 1;

        // Create the multi-signature account using MultiKeyAccount
        MultiKeyAccount multiKey = MultiKeyAccount.from(signers, publicKeys, threshold);

        Logger.info("MultiKeyAccount created:");
        Logger.info("   - Address: " + multiKey.getAccountAddress());
        Logger.info("   - Number of signers: " + signers.size());
        Logger.info("   - Number of public keys: " + publicKeys.size());
        Logger.info("   - Threshold: " + threshold);
        Logger.info("   - Scheme: MultiKey (3)");

        // Fund the account
        Logger.info("Funding MultiKeyAccount...");
        String fundHash = FundingUtils.fundAccount(multiKey.getAccountAddress().toString(), TestConfig.FUND_AMOUNT, config.getNetwork());
        assertNotNull(fundHash);
        Thread.sleep(TestConfig.FUNDING_DELAY_MS);

        // Check initial balance
        long initialBalance = client.getAccountCoinAmount(multiKey.getAccountAddress());
        Logger.info("   Initial balance: " + initialBalance + " octas");

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
        Logger.info("   Current sequence number: " + sequenceNumber);

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
        Logger.info("Signing transaction with MultiKeyAccount...");
        AccountAuthenticator authenticator = multiKey.signTransactionWithAuthenticator(raw);
        SignedTransaction signed = new SignedTransaction(raw, authenticator);

        // Verify we can serialize the transaction
        byte[] transactionBytes = signed.bcsToBytes();
        assertTrue(transactionBytes.length > 0);
        Logger.info("   Transaction serialized successfully (" + transactionBytes.length + " bytes)");

        // Submit the transaction
        Logger.info("Submitting MultiKeyAccount transaction...");
        try {
            PendingTransaction pending = client.submitTransaction(signed);
            assertNotNull(pending);
            Logger.info("   ✅ MultiKeyAccount transaction submitted! Hash: " + pending.getHash());

            // Wait for transaction to be committed
            Transaction committed = client.waitForTransaction(pending.getHash());
            assertNotNull(committed);
            Logger.info("   ✅ MultiKeyAccount transaction committed successfully!");

            // Check final balance
            long finalBalance = client.getAccountCoinAmount(multiKey.getAccountAddress());
            Logger.info("   Final balance: " + finalBalance + " octas");
            Logger.info("   Balance change: " + (finalBalance - initialBalance) + " octas");

        } catch (Exception e) {
            Logger.info("   ❌ MultiKeyAccount transaction failed: " + e.getMessage());
            if (e.getMessage().contains("invalid value: integer")) {
                Logger.info("   ⚠️  Got serialization variant error - MultiKey scheme not supported");
            }
            throw e;
        }
    }

    @Test
    @Order(2)
    @DisplayName("1-of-2 MultiEd25519Account: fund and transfer (LOCALNET)")
    void localnetMultiEd25519AccountTest() throws Exception {
        Logger.info("=== Testing MultiEd25519Account on LOCALNET ===");

        // Generate two random accounts (same setup as MultiKey test)
        Ed25519Account account1 = Ed25519Account.generate();
        Ed25519Account account2 = Ed25519Account.generate();

        Logger.info("Generated random accounts:");
        Logger.info("   Account 1: " + account1.getAccountAddress());
        Logger.info("   Account 2: " + account2.getAccountAddress());

        // Set up the multi-signature configuration (1-of-2)
        List<Account> signers = List.of(account1);
        List<Ed25519PublicKey> publicKeys = Arrays.asList(
                account1.getPublicKey(),
                account2.getPublicKey()
        );

        int threshold = 1;

        // Create the multi-signature account using MultiEd25519Account (scheme 1)
        MultiEd25519Account multiEd25519 = MultiEd25519Account.from(signers, publicKeys, threshold);

        Logger.info("MultiEd25519Account created:");
        Logger.info("   - Address: " + multiEd25519.getAccountAddress());
        Logger.info("   - Number of signers: " + signers.size());
        Logger.info("   - Number of public keys: " + publicKeys.size());
        Logger.info("   - Threshold: " + threshold);
        Logger.info("   - Scheme: MultiEd25519 (1)");

        // Fund the account
        Logger.info("Funding MultiEd25519Account...");
        String fundHash = FundingUtils.fundAccount(multiEd25519.getAccountAddress().toString(), TestConfig.FUND_AMOUNT, config.getNetwork());
        assertNotNull(fundHash);
        Thread.sleep(TestConfig.FUNDING_DELAY_MS);

        // Check initial balance
        long initialBalance = client.getAccountCoinAmount(multiEd25519.getAccountAddress());
        Logger.info("   Initial balance: " + initialBalance + " octas");

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
        Logger.info("   Current sequence number: " + sequenceNumber);

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
        Logger.info("Signing transaction with MultiEd25519Account...");
        AccountAuthenticator authenticator = multiEd25519.signTransactionWithAuthenticator(raw);
        SignedTransaction signed = new SignedTransaction(raw, authenticator);

        // Verify we can serialize the transaction
        byte[] transactionBytes = signed.bcsToBytes();
        assertTrue(transactionBytes.length > 0);
        Logger.info("   Transaction serialized successfully (" + transactionBytes.length + " bytes)");

        // Submit the transaction
        Logger.info("Submitting MultiEd25519Account transaction...");
        try {
            PendingTransaction pending = client.submitTransaction(signed);
            assertNotNull(pending);
            Logger.info("   ✅ MultiEd25519Account transaction submitted! Hash: " + pending.getHash());

            // Wait for transaction to be committed
            Transaction committed = client.waitForTransaction(pending.getHash());
            assertNotNull(committed);
            Logger.info("   ✅ MultiEd25519Account transaction committed successfully!");

            // Check final balance
            long finalBalance = client.getAccountCoinAmount(multiEd25519.getAccountAddress());
            Logger.info("   Final balance: " + finalBalance + " octas");
            Logger.info("   Balance change: " + (finalBalance - initialBalance) + " octas");

        } catch (Exception e) {
            Logger.info("   ❌ MultiEd25519Account transaction failed: " + e.getMessage());
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