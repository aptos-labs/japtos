package com.aptoslabs.japtos;

import com.aptoslabs.japtos.account.Account;
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.api.AptosConfig;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.client.AptosClient;
import com.aptoslabs.japtos.client.dto.PendingTransaction;
import com.aptoslabs.japtos.client.dto.Transaction;
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.transaction.SignedTransaction;
import com.aptoslabs.japtos.transaction.authenticator.AccountAuthenticator;
import com.aptoslabs.japtos.types.TransactionPayload;
import com.aptoslabs.japtos.types.EntryFunctionPayload;
import com.aptoslabs.japtos.types.ModuleId;
import com.aptoslabs.japtos.types.Identifier;
import com.aptoslabs.japtos.types.TypeTag;
import com.aptoslabs.japtos.types.StructTag;
import com.aptoslabs.japtos.types.TransactionArgument;
import com.aptoslabs.japtos.utils.FundingUtils;
import com.aptoslabs.japtos.utils.TestConfig;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ModuleFunctionTests {
    private AptosClient client;
    private Ed25519Account sender;
    private Ed25519Account recipient;
    private AptosConfig config;
    private AptosConfig.Network  network = AptosConfig.Network.LOCALNET;

    @BeforeAll
    void setup() throws IOException {
        config = AptosConfig.builder().network(network).build();
        client = new AptosClient(config);

        // Fail fast if localnet is not available
        try {
            client.getLedgerInfo();
        } catch (Exception e) {
            throw new RuntimeException("LOCALNET not available at " + config.getFullnode(), e);
        }

        sender = Account.generate();
        recipient = Account.generate();

        // Attempt to fund both accounts with 0.1 APT for LOCALNET (best-effort)
        String fundAmount = "10000000"; // 0.1 APT in octas
        try {
            String h1 = FundingUtils.fundAccount(sender.getAccountAddress().toString(), fundAmount, config.getNetwork());
            String h2 = FundingUtils.fundAccount(recipient.getAccountAddress().toString(), fundAmount, config.getNetwork());
            try { client.waitForTransaction(h1); } catch (Exception ignored) {}
            try { client.waitForTransaction(h2); } catch (Exception ignored) {}
        } catch (Exception ignored) {
            // Continue even if faucet fails; transaction may still succeed depending on localnet config
        }
    }

    @Test
    @Order(1)
    @DisplayName("Call Move function 0x1::coin::transfer and submit on-chain")
    void callMoveFunctionAndSubmit() throws Exception {

        FundingUtils.fundAccount(sender.getAccountAddress().toString(), TestConfig.FUND_AMOUNT, network);
        Thread.sleep(5000);

        // Get current sequence number
        long sequenceNumber = client.getNextSequenceNumber(sender.getAccountAddress());
        System.out.println("   Current sequence number: " + sequenceNumber);
        
        // Check initial balance
        long initialBalance = client.getAccountCoinAmount(sender.getAccountAddress());
        System.out.println("   Initial balance: " + initialBalance + " octas");
        
        // Build entry function payload for 0x1::coin::transfer(recipient, amount)
        ModuleId moduleId = new ModuleId(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("coin"));
        Identifier functionName = new Identifier("transfer");
        TransactionPayload payload = new EntryFunctionPayload(
            moduleId,
            functionName,
            java.util.List.of(new TypeTag.Struct(new StructTag(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("aptos_coin"), new Identifier("AptosCoin"), java.util.List.of()))), // Correct AptosCoin type argument
            java.util.List.of(
                new TransactionArgument.AccountAddress(recipient.getAccountAddress()),
                new TransactionArgument.U64(TestConfig.SMALL_TRANSFER)
            )
        );

        // Build raw transaction with proper sequence number
        RawTransaction raw = new RawTransaction(
            sender.getAccountAddress(),
            sequenceNumber, // Use actual sequence number
            payload,
            TestConfig.DEFAULT_MAX_GAS,
            TestConfig.DEFAULT_GAS_PRICE,
            (System.currentTimeMillis() / 1000L) + 3600,
            config.getNetwork().getChainId()
        );

        // Sign and wrap into SignedTransaction
        AccountAuthenticator authenticator = sender.signTransactionWithAuthenticator(raw);
        SignedTransaction signed = new SignedTransaction(raw, authenticator);

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
        
        // Check final balance
        long finalBalance = client.getAccountCoinAmount(sender.getAccountAddress());
        System.out.println("   Final balance: " + finalBalance + " octas");
        System.out.println("   Balance change: " + (finalBalance - initialBalance) + " octas");
        
        // Verify we can serialize the transaction
        byte[] transactionBytes = signed.bcsToBytes();
        assertTrue(transactionBytes.length > 0);
        System.out.println("   Transaction serialized successfully (" + transactionBytes.length + " bytes)");
    }
} 