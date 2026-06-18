package com.aptoslabs.japtos;

import com.aptoslabs.japtos.utils.Logger;

import com.aptoslabs.japtos.account.Account;
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.api.AptosConfig;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.types.*;
import com.aptoslabs.japtos.utils.CryptoUtils;
import com.aptoslabs.japtos.utils.HexUtils;
import com.aptoslabs.japtos.utils.TestConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for transaction signing functionality (without network calls)
 */
@TestInstance(Lifecycle.PER_CLASS)
public class SigningTests {

    private Ed25519Account senderAccount;
    private Ed25519Account recipientAccount;

    @BeforeAll
    void setUp() {
        // Generate test accounts
        senderAccount = Account.generate();
        recipientAccount = Account.generate();

        Logger.info("=== Transaction Signing Tests ===");
        Logger.info("Sender: " + senderAccount.getAccountAddress());
        Logger.info("Recipient: " + recipientAccount.getAccountAddress());
    }

    @Test
    @DisplayName("Test transaction creation and signing")
    void testTransactionCreationAndSigning() throws Exception {
        Logger.info("\n1. Testing transaction creation and signing...");

        // Build entry function payload for 0x1::coin::transfer(recipient, amount)
        ModuleId moduleId = new ModuleId(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("coin"));
        Identifier functionName = new Identifier("transfer");
        TransactionPayload payload = new EntryFunctionPayload(
                moduleId,
                functionName,
                java.util.List.of(new TypeTag.Struct(new StructTag(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("aptos_coin"), new Identifier("AptosCoin"), java.util.List.of()))), // Correct AptosCoin type argument
                java.util.List.of(
                        new TransactionArgument.AccountAddress(recipientAccount.getAccountAddress()),
                        new TransactionArgument.U64(TestConfig.TRANSFER_AMOUNT)
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
                AptosConfig.Network.LOCALNET.getChainId()
        );

        // Sign
        var sig = senderAccount.signTransaction(raw);
        Logger.info("   Transaction signed successfully");
        Logger.info("   Signature: " + com.aptoslabs.japtos.utils.HexUtils.bytesToHex(sig.toBytes()));

        // Create the same message that was signed: sha3("APTOS::RawTransaction") || BCS(RawTransaction)
        byte[] domain = "APTOS::RawTransaction".getBytes();
        byte[] prefixHash = CryptoUtils.sha3_256(domain);
        byte[] transactionBytes = raw.bcsToBytes();
        byte[] signingMessage = new byte[prefixHash.length + transactionBytes.length];
        System.arraycopy(prefixHash, 0, signingMessage, 0, prefixHash.length);
        System.arraycopy(transactionBytes, 0, signingMessage, prefixHash.length, transactionBytes.length);

        // Verify signature against the signing message (not hashed)
        boolean isValid = senderAccount.verifySignature(signingMessage, sig);
        Logger.info("   Signature verification: " + isValid);

        assertTrue(isValid);
        assertTrue(transactionBytes.length > 0);
    }

    @Test
    @DisplayName("Test factory method transaction creation")
    void testFactoryMethodTransactionCreation() throws Exception {
        Logger.info("\n2. Testing factory method transaction creation...");

        // Build entry function payload via 'create' convenience
        ModuleId coinModule = new ModuleId(
                AccountAddress.fromHex(TestConfig.APTOS_FRAMEWORK_ADDRESS),
                new Identifier("coin")
        );
        TransactionPayload payload = new EntryFunctionPayload(
                coinModule,
                new Identifier("transfer"),
                java.util.List.of(new TypeTag.Struct(new StructTag(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("aptos_coin"), new Identifier("AptosCoin"), java.util.List.of()))), // Correct AptosCoin type argument
                java.util.List.of(
                        new TransactionArgument.AccountAddress(recipientAccount.getAccountAddress()),
                        new TransactionArgument.U64(TestConfig.SMALL_TRANSFER)
                )
        );

        RawTransaction raw = new RawTransaction(
                senderAccount.getAccountAddress(),
                1L,
                payload,
                TestConfig.DEFAULT_MAX_GAS,
                TestConfig.DEFAULT_GAS_PRICE,
                (System.currentTimeMillis() / 1000L) + 3600,
                AptosConfig.Network.LOCALNET.getChainId()
        );

        var sig = senderAccount.signTransaction(raw);
        assertNotNull(sig);
    }

    @Test
    @DisplayName("Test custom coin type transaction")
    void testCustomCoinTypeTransaction() throws Exception {
        Logger.info("\n3. Testing custom coin type transaction...");

        // Create a custom coin type (simulating USDC)
        StructTag usdcType = new StructTag(
                AccountAddress.fromHex(TestConfig.APTOS_FRAMEWORK_ADDRESS),
                new Identifier("coin"),
                new Identifier("USDC"),
                java.util.List.of()
        );
        TypeTag usdcTypeTag = new TypeTag.Struct(usdcType);

        // Create transfer with custom coin type
        TransactionPayload payload = new EntryFunctionPayload(
                new ModuleId(AccountAddress.fromHex(TestConfig.APTOS_FRAMEWORK_ADDRESS), new Identifier("coin")),
                new Identifier("transfer"),
                java.util.List.of(usdcTypeTag),
                java.util.List.of(
                        new TransactionArgument.AccountAddress(recipientAccount.getAccountAddress()),
                        new TransactionArgument.U64(TestConfig.SMALL_TRANSFER)
                )
        );

        RawTransaction raw = new RawTransaction(
                senderAccount.getAccountAddress(),
                2L,
                payload,
                TestConfig.DEFAULT_MAX_GAS,
                TestConfig.DEFAULT_GAS_PRICE,
                (System.currentTimeMillis() / 1000L) + 3600,
                AptosConfig.Network.LOCALNET.getChainId()
        );

        var sig = senderAccount.signTransaction(raw);
        assertNotNull(sig);
    }

    @Test
    @DisplayName("Test message signing and verification")
    void testMessageSigning() {
        Logger.info("\n4. Testing message signing...");

        String message = "Hello, Aptos!";
        byte[] messageBytes = message.getBytes();

        var signature = senderAccount.sign(messageBytes);
        Logger.info("   Message: " + message);
        Logger.info("   Signature: " + HexUtils.bytesToHex(signature.toBytes()));

        boolean isValid = senderAccount.verifySignature(messageBytes, signature);
        Logger.info("   Signature valid: " + isValid);

        assertTrue(isValid);

        String wrongMessage = "Wrong message";
        byte[] wrongBytes = wrongMessage.getBytes();
        boolean isInvalid = senderAccount.verifySignature(wrongBytes, signature);
        Logger.info("   Wrong message valid: " + isInvalid);

        assertFalse(isInvalid);
    }

    @Test
    @DisplayName("Test transaction serialization")
    void testTransactionSerialization() throws Exception {
        Logger.info("\n5. Testing transaction serialization...");

        // Create a simple transfer
        TransactionPayload payload = new EntryFunctionPayload(
                new ModuleId(AccountAddress.fromHex(TestConfig.APTOS_FRAMEWORK_ADDRESS), new Identifier("coin")),
                new Identifier("transfer"),
                java.util.List.of(new TypeTag.Struct(new StructTag(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("aptos_coin"), new Identifier("AptosCoin"), java.util.List.of()))), // Correct AptosCoin type argument
                java.util.List.of(
                        new TransactionArgument.AccountAddress(recipientAccount.getAccountAddress()),
                        new TransactionArgument.U64(TestConfig.SMALL_TRANSFER)
                )
        );

        RawTransaction raw = new RawTransaction(
                senderAccount.getAccountAddress(),
                3L,
                payload,
                TestConfig.DEFAULT_MAX_GAS,
                TestConfig.DEFAULT_GAS_PRICE,
                (System.currentTimeMillis() / 1000L) + 3600,
                AptosConfig.Network.LOCALNET.getChainId()
        );

        byte[] serialized = raw.bcsToBytes();
        Logger.info("   Serialized transaction length: " + serialized.length + " bytes");
        Logger.info("   Serialized transaction hex: " + HexUtils.bytesToHex(serialized));

        var sig = senderAccount.signTransaction(raw);
        Logger.info("   Signature: " + HexUtils.bytesToHex(sig.toBytes()));

        // Create the same message that was signed: sha3("APTOS::RawTransaction") || BCS(RawTransaction)
        byte[] domain = "APTOS::RawTransaction".getBytes();
        byte[] prefixHash = CryptoUtils.sha3_256(domain);
        byte[] signingMessage = new byte[prefixHash.length + serialized.length];
        System.arraycopy(prefixHash, 0, signingMessage, 0, prefixHash.length);
        System.arraycopy(serialized, 0, signingMessage, prefixHash.length, serialized.length);

        boolean isValid = senderAccount.verifySignature(signingMessage, sig);
        Logger.info("   Signature verification: " + isValid);

        assertTrue(isValid);
        assertTrue(serialized.length > 0);
    }
} 