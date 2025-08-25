package com.aptoslabs.japtos;

import com.aptoslabs.japtos.account.Account;
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.core.crypto.Signature;
import com.aptoslabs.japtos.utils.HexUtils;

/**
 * Aptos Java SDK - Main demonstration class
 */
public class App {
    public static void main(String[] args) {
        System.out.println("=== Aptos Java SDK Demo ===\n");

        try {
            // 1. Account Management Demo
            System.out.println("1. Account Management:");
            Ed25519Account account = Account.generate();
            System.out.println("   Generated account address: " + account.getAccountAddress());
            System.out.println("   Public key: " + account.getPublicKeyHex());
            System.out.println("   Private key: " + account.getPrivateKeyHex());

            // 2. Message Signing Demo
            System.out.println("\n2. Message Signing:");
            String message = "Hello, Aptos!";
            byte[] messageBytes = message.getBytes();
            Signature signature = account.sign(messageBytes);
            System.out.println("   Message: " + message);
            System.out.println("   Signature: " + signature.toString());

            boolean isValid = account.verifySignature(messageBytes, signature);
            System.out.println("   Signature valid: " + isValid);

            // 3. Hex Utilities Demo
            System.out.println("\n3. Hex Utilities:");
            byte[] testBytes = {0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF};
            String hex = HexUtils.bytesToHex(testBytes);
            System.out.println("   Bytes to hex: " + hex);
            byte[] convertedBytes = HexUtils.hexToBytes(hex);
            System.out.println("   Hex to bytes: " + HexUtils.bytesToHex(convertedBytes));

            // 4. Account Address Demo
            System.out.println("\n4. Account Address:");
            AccountAddress zeroAddress = AccountAddress.zero();
            System.out.println("   Zero address: " + zeroAddress);
            System.out.println("   Is zero: " + zeroAddress.isZero());

            // 5. Client Demo (commented out to avoid network calls)
            System.out.println("\n5. Client Operations:");
            System.out.println("   Note: Network operations are commented out to avoid external dependencies");
            /*
            AptosConfig config = AptosConfig.builder()
                .network(AptosConfig.Network.DEVNET)
                .build();
            AptosClient client = new AptosClient(config);
            
            // Get account info
            var accountInfo = client.getAccount(account.getAccountAddress());
            System.out.println("   Account sequence number: " + accountInfo.getSequenceNumber());
            
            // Get ledger info
            var ledgerInfo = client.getLedgerInfo();
            System.out.println("   Chain ID: " + ledgerInfo.getChainId());
            System.out.println("   Ledger version: " + ledgerInfo.getLedgerVersion());
            */

            System.out.println("\n=== Demo completed successfully! ===");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
