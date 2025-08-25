package com.aptoslabs.japtos;

import com.aptoslabs.japtos.account.Account;
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.api.AptosConfig;
import com.aptoslabs.japtos.client.AptosClient;
import com.aptoslabs.japtos.client.AptosClientException;
import com.aptoslabs.japtos.client.dto.AccountInfo;
import com.aptoslabs.japtos.client.dto.PendingTransaction;
import com.aptoslabs.japtos.client.dto.Transaction;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.transaction.SignedTransaction;
import com.aptoslabs.japtos.transaction.authenticator.AccountAuthenticator;
import com.aptoslabs.japtos.types.*;
import com.aptoslabs.japtos.utils.CryptoUtils;
import com.aptoslabs.japtos.utils.FundingUtils;
import com.aptoslabs.japtos.utils.HexUtils;
import com.aptoslabs.japtos.utils.TestConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for real transaction signing on Aptos localnet
 */
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TransactionTests {

    private AptosClient client;
    private Ed25519Account senderAccount;
    private Ed25519Account recipientAccount;
    private AptosConfig.Network network;

    @BeforeAll
    void setUp() throws Exception {
        network = AptosConfig.Network.LOCALNET;
        AptosConfig config = AptosConfig.builder().network(network).build();
        client = new AptosClient(config);

        // Generate test accounts
        senderAccount = Account.generate();
        recipientAccount = Account.generate();

        System.out.println("=== Localnet Transaction Tests ===");
        System.out.println("Network: " + network.name());
        System.out.println("Fullnode URL: " + config.getFullnode());
        System.out.println("Faucet URL: " + config.getFaucet());
        System.out.println("Sender: " + senderAccount.getAccountAddress());
        System.out.println("Recipient: " + recipientAccount.getAccountAddress());

        // Print localnet information
        try {
            var ledgerInfo = client.getLedgerInfo();
            System.out.println("=== Localnet Information ===");
            System.out.println("Chain ID: " + ledgerInfo.getChainId());
            System.out.println("Ledger Version: " + ledgerInfo.getLedgerVersion());
            System.out.println("Block Height: " + ledgerInfo.getBlockHeight());
            System.out.println("Timestamp: " + ledgerInfo.getLedgerTimestamp());
            System.out.println("===========================");
        } catch (AptosClientException e) {
            System.err.println("Failed to get localnet info: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("Fund sender account using localnet faucet")
    void testFundSenderAccount() throws Exception {
        System.out.println("\n1. Funding sender account on localnet...");

        // Fund the sender account
        String fundingHash = FundingUtils.fundAccount(senderAccount.getAccountAddress().toString(), TestConfig.FUND_AMOUNT, network);
        System.out.println("   Funding transaction hash: " + fundingHash);

        // Wait for account to be funded
        Thread.sleep(2000); // Wait for transaction to be processed
        var accountInfo = client.getAccount(senderAccount.getAccountAddress());
        long sequenceNumber = accountInfo.getSequenceNumber();
        System.out.println("   Sequence number: " + sequenceNumber);
        System.out.println("   Account funded successfully");

        assertTrue(sequenceNumber >= 0);
    }

    @Test
    @Order(2)
    @DisplayName("Fund recipient account using localnet faucet")
    void testFundRecipientAccount() throws Exception {
        System.out.println("\n2. Funding recipient account on localnet...");

        // Fund the recipient account
        String fundingHash = FundingUtils.fundAccount(recipientAccount.getAccountAddress().toString(), TestConfig.FUND_AMOUNT, network);
        System.out.println("   Funding transaction hash: " + fundingHash);

        // Wait for account to be funded
        Thread.sleep(2000); // Wait for transaction to be processed
        var accountInfo = client.getAccount(recipientAccount.getAccountAddress());
        long sequenceNumber = accountInfo.getSequenceNumber();
        System.out.println("   Sequence number: " + sequenceNumber);
        System.out.println("   Account funded successfully");

        assertTrue(sequenceNumber >= 0);
    }

    @Test
    @Order(3)
    @DisplayName("Sign and submit simple APT transfer transaction on localnet")
    void testSimpleTransfer() throws Exception {
        System.out.println("\n3. Testing simple APT transfer on localnet...");

        // Get current sequence number
        AccountInfo senderInfo = client.getAccount(senderAccount.getAccountAddress());
        long sequenceNumber = senderInfo.getSequenceNumber();

        // Fund the sender account if needed
        long senderBalance = client.getAccountCoinAmount(senderAccount.getAccountAddress());
        long fundAmount = Long.parseLong(TestConfig.FUND_AMOUNT);
        if (senderBalance < fundAmount) {
            String fundingHash = FundingUtils.fundAccount(senderAccount.getAccountAddress().toString(), TestConfig.FUND_AMOUNT, network);
            System.out.println("   Funding sender account with hash: " + fundingHash);

            // Wait for funding transaction to be committed
            System.out.println("   Waiting for funding transaction to be committed...");
            Thread.sleep(3000); // Give time for transaction to be processed
        }

        // Create transfer payload for 0x1::coin::transfer(recipient, amount)
        ModuleId moduleId = new ModuleId(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("coin"));
        Identifier functionName = new Identifier("transfer");
        TransactionPayload transferPayload = new EntryFunctionPayload(
                moduleId,
                functionName,
                java.util.List.of(new TypeTag.Struct(new StructTag(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("aptos_coin"), new Identifier("AptosCoin"), java.util.List.of()))), // Correct AptosCoin type argument
                java.util.List.of(
                        new TransactionArgument.AccountAddress(recipientAccount.getAccountAddress()),
                        new TransactionArgument.U64(TestConfig.TRANSFER_AMOUNT)
                )
        );

        // Build raw transaction
        long chainId = network.getChainId();
        System.out.println("   Using chain ID: " + chainId);
        RawTransaction raw = new RawTransaction(
                senderAccount.getAccountAddress(),
                sequenceNumber,
                transferPayload,
                TestConfig.DEFAULT_MAX_GAS,
                TestConfig.DEFAULT_GAS_PRICE,
                (System.currentTimeMillis() / 1000L) + 3600,
                chainId
        );

        // Sign with authenticator and create SignedTransaction
        AccountAuthenticator authenticator = senderAccount.signTransactionWithAuthenticator(raw);
        SignedTransaction signed = new SignedTransaction(raw, authenticator);
        System.out.println("   Transaction signed");

        // Submit the transaction
        System.out.println("   Submitting transaction...");
        PendingTransaction pending = client.submitTransaction(signed);
        assertNotNull(pending);
        System.out.println("   Transaction submitted with hash: " + pending.getHash());

        // Wait for transaction to be committed
        System.out.println("   Waiting for transaction to be committed...");
        Transaction committed = client.waitForTransaction(pending.getHash());
        assertNotNull(committed);
        System.out.println("   Transaction committed successfully");

        // Verify we can serialize the transaction
        byte[] transactionBytes = signed.bcsToBytes();
        assertTrue(transactionBytes.length > 0);
        System.out.println("   Transaction serialized successfully (" + transactionBytes.length + " bytes)");
    }

    @Test
    @Order(4)
    @DisplayName("Test transaction signature verification (deterministic)")
    void testSignatureVerification() throws Exception {
        System.out.println("\n4. Testing signature verification...");

        // Create a simple transfer payload
        ModuleId moduleId = new ModuleId(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("coin"));
        Identifier functionName = new Identifier("transfer");
        TransactionPayload payload = new EntryFunctionPayload(
                moduleId,
                functionName,
                java.util.List.of(new TypeTag.Struct(new StructTag(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("aptos_coin"), new Identifier("AptosCoin"), java.util.List.of()))), // Correct AptosCoin type argument
                java.util.List.of(
                        new TransactionArgument.AccountAddress(recipientAccount.getAccountAddress()),
                        new TransactionArgument.U64(TestConfig.SMALL_TRANSFER)
                )
        );

        // Build raw transaction
        RawTransaction raw = new RawTransaction(
                senderAccount.getAccountAddress(),
                0L,
                payload,
                TestConfig.DEFAULT_MAX_GAS,
                TestConfig.DEFAULT_GAS_PRICE,
                (System.currentTimeMillis() / 1000L) + 3600,
                network.getChainId()
        );

        var signature = senderAccount.signTransaction(raw);

        // Create the same message that was signed: sha3("APTOS::RawTransaction") || BCS(RawTransaction)
        byte[] domain = "APTOS::RawTransaction".getBytes();
        byte[] prefixHash = CryptoUtils.sha3_256(domain);
        byte[] transactionBytes = raw.bcsToBytes();
        byte[] signingMessage = new byte[prefixHash.length + transactionBytes.length];
        System.arraycopy(prefixHash, 0, signingMessage, 0, prefixHash.length);
        System.arraycopy(transactionBytes, 0, signingMessage, prefixHash.length, transactionBytes.length);

        boolean isValid = senderAccount.verifySignature(signingMessage, signature);
        System.out.println("   Signature verification: " + isValid);
        assertTrue(isValid);

        byte[] wrong = "wrong message".getBytes();
        assertFalse(senderAccount.verifySignature(wrong, signature));
    }

    @Test
    @Order(5)
    @DisplayName("Test message signing and verification")
    void testMessageSigning() {
        System.out.println("\n5. Testing message signing...");

        String message = "Hello, Aptos Devnet!";
        byte[] messageBytes = message.getBytes();

        var signature = senderAccount.sign(messageBytes);
        System.out.println("   Message: " + message);
        System.out.println("   Signature: " + HexUtils.bytesToHex(signature.toBytes()));

        boolean isValid = senderAccount.verifySignature(messageBytes, signature);
        System.out.println("   Signature valid: " + isValid);
        assertTrue(isValid);

        String wrongMessage = "Wrong message";
        assertFalse(senderAccount.verifySignature(wrongMessage.getBytes(), signature));
    }

    @AfterAll
    void tearDown() {
        System.out.println("\n=== Localnet Transaction Tests Completed ===");
    }
}
