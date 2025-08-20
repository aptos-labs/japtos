package com.aptoslabs.japtos;

import com.aptoslabs.japtos.account.Account;
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.api.AptosConfig;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.types.*;
import com.aptoslabs.japtos.utils.HexUtils;

import com.aptoslabs.japtos.utils.TestConfig;
import org.junit.jupiter.api.*;
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
        
        System.out.println("=== Transaction Signing Tests ===");
        System.out.println("Sender: " + senderAccount.getAccountAddress());
        System.out.println("Recipient: " + recipientAccount.getAccountAddress());
    }
    
    @Test
    @DisplayName("Test transaction creation and signing")
    void testTransactionCreationAndSigning() throws Exception {
        System.out.println("\n1. Testing transaction creation and signing...");
        
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
        System.out.println("   Transaction signed successfully");
        System.out.println("   Signature: " + com.aptoslabs.japtos.utils.HexUtils.bytesToHex(sig.toBytes()));
        
        // Create the same message that was signed: sha3("APTOS::RawTransaction") || BCS(RawTransaction)
        byte[] domain = "APTOS::RawTransaction".getBytes();
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA3-256");
        byte[] prefixHash = digest.digest(domain);
        byte[] transactionBytes = raw.bcsToBytes();
        byte[] signingMessage = new byte[prefixHash.length + transactionBytes.length];
        System.arraycopy(prefixHash, 0, signingMessage, 0, prefixHash.length);
        System.arraycopy(transactionBytes, 0, signingMessage, prefixHash.length, transactionBytes.length);
        
        // Verify signature against the signing message (not hashed)
        boolean isValid = senderAccount.verifySignature(signingMessage, sig);
        System.out.println("   Signature verification: " + isValid);
        
        assertTrue(isValid);
        assertTrue(transactionBytes.length > 0);
    }
    
    @Test
    @DisplayName("Test factory method transaction creation")
    void testFactoryMethodTransactionCreation() throws Exception {
        System.out.println("\n2. Testing factory method transaction creation...");
        
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
        System.out.println("\n3. Testing custom coin type transaction...");
        
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
        System.out.println("\n4. Testing message signing...");
        
        String message = "Hello, Aptos!";
        byte[] messageBytes = message.getBytes();
        
        var signature = senderAccount.sign(messageBytes);
        System.out.println("   Message: " + message);
        System.out.println("   Signature: " + HexUtils.bytesToHex(signature.toBytes()));
        
        boolean isValid = senderAccount.verifySignature(messageBytes, signature);
        System.out.println("   Signature valid: " + isValid);
        
        assertTrue(isValid);
        
        String wrongMessage = "Wrong message";
        byte[] wrongBytes = wrongMessage.getBytes();
        boolean isInvalid = senderAccount.verifySignature(wrongBytes, signature);
        System.out.println("   Wrong message valid: " + isInvalid);
        
        assertFalse(isInvalid);
    }
    
    @Test
    @DisplayName("Test transaction serialization")
    void testTransactionSerialization() throws Exception {
        System.out.println("\n5. Testing transaction serialization...");
        
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
        System.out.println("   Serialized transaction length: " + serialized.length + " bytes");
        System.out.println("   Serialized transaction hex: " + HexUtils.bytesToHex(serialized));
        
        var sig = senderAccount.signTransaction(raw);
        System.out.println("   Signature: " + HexUtils.bytesToHex(sig.toBytes()));
        
        // Create the same message that was signed: sha3("APTOS::RawTransaction") || BCS(RawTransaction)
        byte[] domain = "APTOS::RawTransaction".getBytes();
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA3-256");
        byte[] prefixHash = digest.digest(domain);
        byte[] signingMessage = new byte[prefixHash.length + serialized.length];
        System.arraycopy(prefixHash, 0, signingMessage, 0, prefixHash.length);
        System.arraycopy(serialized, 0, signingMessage, prefixHash.length, serialized.length);
        
        boolean isValid = senderAccount.verifySignature(signingMessage, sig);
        System.out.println("   Signature verification: " + isValid);
        
        assertTrue(isValid);
        assertTrue(serialized.length > 0);
    }
} 