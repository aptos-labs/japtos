package com.aptoslabs.japtos;

import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.api.AptosConfig;
import com.aptoslabs.japtos.client.AptosClient;
import com.aptoslabs.japtos.client.dto.AccountInfo;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.types.*;
import com.aptoslabs.japtos.utils.FundingUtils;
import com.aptoslabs.japtos.utils.HexUtils;
import com.aptoslabs.japtos.transaction.*;
import com.aptoslabs.japtos.transaction.authenticator.AccountAuthenticator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInstance;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * mvn test -Dtest=MoveOptionIntegrationTest
 * Test disabled due to Devnet resets and test will fail...
 */
@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MoveOptionIntegrationTest {
    
    // Module deployed at: https://explorer.aptoslabs.com/txn/0xaadcad0f9fa5ae19d1c49eee2c33cb53f396290c1d2f8774dfd170856109cb02?network=devnet
    // Move sample code.
    /**
        module simple_option_test::simple_option_test {
            use std::option::{Self, Option};
            use std::string::String;

            struct TestResult has key {
                u64_val: Option<u64>,
                string_val: Option<String>,
                bool_val: Option<bool>,
                address_val: Option<address>
            }

            public entry fun test_options(
                account: &signer,
                u64_opt: Option<u64>,
                string_opt: Option<String>,
                bool_opt: Option<bool>,
                address_opt: Option<address>
            ) {
                // Just receive the options to test serialization
                let _ = account;
                let _ = u64_opt;
                let _ = string_opt;
                let _ = bool_opt;
                let _ = address_opt;
            }

            public entry fun test_mixed(
                account: &signer,
                required_u64: u64,
                optional_string: Option<String>,
                required_bool: bool,
                optional_u64: Option<u64>
            ) {
                // Test mixing required and optional params
                let _ = account;
                let _ = required_u64;
                let _ = optional_string;
                let _ = required_bool;
                let _ = optional_u64;
            }
        }
     */
    private static final String MODULE_ADDRESS = "0xc69f62c5ac662466747019a2ed7c2659d9ccfab6610d42ec459bba7f382019b1";
    private static final String DEPLOYER_PRIVATE_KEY = "7BE3F3893046429844F41E91319EFE0CA9CFF283D09561E454BB0A00D95D0A27";
    
    private Ed25519Account deployerAccount;
    private AccountAddress moduleAddress;
    private AptosClient client;
    
    @BeforeAll
    public void setup() throws Exception {
        // Create account from private key
        com.aptoslabs.japtos.core.crypto.Ed25519PrivateKey privateKey = com.aptoslabs.japtos.core.crypto.Ed25519PrivateKey.fromHex(DEPLOYER_PRIVATE_KEY);
        deployerAccount = Ed25519Account.fromPrivateKey(privateKey);
        moduleAddress = AccountAddress.fromHex(MODULE_ADDRESS);
        
        AptosConfig config = AptosConfig.builder()
            .network(AptosConfig.Network.DEVNET)
            .build();
        client = new AptosClient(config);
        
        System.out.println("Test account: " + deployerAccount.getAccountAddress());
        System.out.println("Module address: " + moduleAddress);
    }
    
    @Test
    public void testAllOptionsOnChain() throws Exception {
        if (DEPLOYER_PRIVATE_KEY.isEmpty()) {
            System.out.println("Skipping test - module not deployed");
            return;
        }
        
        System.out.println("\n=== Testing all option types on-chain ===");
        
        // Create arguments with mix of Some and None values
        List<TransactionArgument> args = Arrays.asList(
            MoveOption.u64(12345L),                              // Some(12345)
            MoveOption.u128(null),                               // None
            MoveOption.string("hello from japtos"),              // Some("hello from japtos")
            MoveOption.address(null),                            // None
            MoveOption.bool(true),                               // Some(true)
            MoveOption.u8(null),                                 // None
            MoveOption.u256(BigInteger.valueOf(999))            // Some(999)
        );
        
        // Log what we're sending
        System.out.println("Sending transaction with:");
        System.out.println("  u64_opt: Some(12345)");
        System.out.println("  u128_opt: None");
        System.out.println("  string_opt: Some(\"hello from japtos\")");
        System.out.println("  address_opt: None");
        System.out.println("  bool_opt: Some(true)");
        System.out.println("  u8_opt: None");
        System.out.println("  u256_opt: Some(999)");
        
        // Create and execute transaction - using test_options from simple_option_test
        String txHash = executeModuleFunction("test_options", Arrays.asList(
            args.get(0), // u64_opt
            args.get(2), // string_opt  
            args.get(4), // bool_opt
            args.get(3)  // address_opt
        ));
        System.out.println("Transaction hash: " + txHash);
        System.out.println("test_all_options executed successfully!");
    }
    
    @Test
    public void testMixedArgsOnChain() throws Exception {
        if (DEPLOYER_PRIVATE_KEY.isEmpty()) {
            System.out.println("Skipping test - module not deployed");
            return;
        }
        
        System.out.println("\n=== Testing mixed required and optional args on-chain ===");
        
        // Create arguments mixing required and optional parameters
        List<TransactionArgument> args = Arrays.asList(
            new TransactionArgument.U64(456L),                   // required_u64
            MoveOption.string("optional value"),                 // optional_string: Some(...)
            new TransactionArgument.Bool(false),                 // required_bool
            MoveOption.address(deployerAccount.getAccountAddress()), // optional_address: Some(...)
            MoveOption.u64(null)                                 // optional_amount: None
        );
        
        // Log what we're sending
        System.out.println("Sending transaction with:");
        System.out.println("  required_u64: 456");
        System.out.println("  optional_string: Some(\"optional value\")");
        System.out.println("  required_bool: false");
        System.out.println("  optional_address: Some(" + deployerAccount.getAccountAddress() + ")");
        System.out.println("  optional_amount: None");
        
        // Create and execute transaction - using test_mixed from simple_option_test
        String txHash = executeModuleFunction("test_mixed", Arrays.asList(
            args.get(0), // required_u64
            args.get(1), // optional_string
            args.get(2), // required_bool
            args.get(4)  // optional_u64 (using optional_amount)
        ));
        System.out.println("Transaction hash: " + txHash);
        System.out.println("test_mixed_args executed successfully!");
    }
    
    @Test 
    public void testEdgeCases() throws Exception {
        if (DEPLOYER_PRIVATE_KEY.isEmpty()) {
            System.out.println("Skipping test - module not deployed");
            return;
        }
        
        System.out.println("\n=== Testing edge cases ===");
        
        // Test with all None values
        List<TransactionArgument> allNone = Arrays.asList(
            MoveOption.u64(null),
            MoveOption.string(null),
            MoveOption.bool(null),
            MoveOption.address(null)
        );
        
        System.out.println("Testing with all None values...");
        String txHash1 = executeModuleFunction("test_options", allNone);
        System.out.println("Transaction hash: " + txHash1);
        System.out.println("All None values handled correctly!");
        
        // Test with all Some values
        List<TransactionArgument> allSome = Arrays.asList(
            MoveOption.u64(Long.MAX_VALUE),
            MoveOption.string("A very long string to test serialization of longer strings in Move Option types"),
            MoveOption.bool(false),
            MoveOption.address(AccountAddress.fromHex("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"))
        );
        
        System.out.println("\nTesting with maximum values...");
        String txHash2 = executeModuleFunction("test_options", allSome);
        System.out.println("Transaction hash: " + txHash2);
        System.out.println("Maximum values handled correctly!");
    }
    
    /**
     * Execute a module function on-chain
     */
    private String executeModuleFunction(String functionName, List<TransactionArgument> args) throws Exception {
        // Get account balance to ensure we have gas
        long balance = client.getAccountCoinAmount(deployerAccount.getAccountAddress());
        if (balance == 0) {
            System.out.println("Account has no balance, funding...");
            String fundingHash = FundingUtils.fundAccount(
                deployerAccount.getAccountAddress().toString(), 
                "100000000",  // 1 APT in octas
                AptosConfig.Network.DEVNET
            );
            client.waitForTransaction(fundingHash);
            Thread.sleep(2000);
        }
        
        // Get account info
        AccountInfo accountInfo = client.getAccount(deployerAccount.getAccountAddress());
        Long sequenceNumber = accountInfo.getSequenceNumber();
        
        // Build payload
        ModuleId moduleId = new ModuleId(moduleAddress, new Identifier("simple_option_test"));
        EntryFunctionPayload payload = new EntryFunctionPayload(
            moduleId,
            new Identifier(functionName),
            Collections.emptyList(),
            args
        );
        
        // Build raw transaction
        RawTransaction rawTx = new RawTransaction(
            deployerAccount.getAccountAddress(),
            sequenceNumber,
            payload,
            100000L,
            100L,
            System.currentTimeMillis() / 1000 + 600,
            13L
        );
        
        // Sign transaction
        AccountAuthenticator auth = deployerAccount.signTransactionWithAuthenticator(rawTx);
        SignedTransaction signedTx = new SignedTransaction(rawTx, auth);
        
        // Submit transaction
        var pending = client.submitTransaction(signedTx);
        String txHash = pending.getHash();
        
        // Wait for transaction
        var tx = client.waitForTransaction(txHash);
        assertNotNull(tx);
        
        return txHash;
    }
}
