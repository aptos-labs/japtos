package com.aptoslabs.japtos;

import com.aptoslabs.japtos.account.Account;
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.core.crypto.Signature;
import com.aptoslabs.japtos.utils.HexUtils;
import com.aptoslabs.japtos.utils.Logger;

/**
 * Aptos Java SDK - Main demonstration class
 */
public class App {
    public static void main(String[] args) {
        Logger.info("=== Aptos Java SDK Demo ===");

        try {
            // 1. Account Management Demo
            Logger.info("1. Account Management:");
            Ed25519Account account = Account.generate();
            Logger.info("   Generated account address: %s", account.getAccountAddress());
            Logger.info("   Public key: %s", account.getPublicKeyHex());
            Logger.debug("   Private key: %s", account.getPrivateKeyHex());

            // 2. Message Signing Demo
            Logger.info("");
            Logger.info("2. Message Signing:");
            String message = "Hello, Aptos!";
            byte[] messageBytes = message.getBytes();
            Signature signature = account.sign(messageBytes);
            Logger.info("   Message: %s", message);
            Logger.debug("   Signature: %s", signature.toString());

            boolean isValid = account.verifySignature(messageBytes, signature);
            Logger.info("   Signature valid: %s", isValid);

            // 3. Hex Utilities Demo
            Logger.info("");
            Logger.info("3. Hex Utilities:");
            byte[] testBytes = {0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF};
            String hex = HexUtils.bytesToHex(testBytes);
            Logger.debug("   Bytes to hex: %s", hex);
            byte[] convertedBytes = HexUtils.hexToBytes(hex);
            Logger.debug("   Hex to bytes: %s", HexUtils.bytesToHex(convertedBytes));

            // 4. Account Address Demo
            Logger.info("");
            Logger.info("4. Account Address:");
            AccountAddress zeroAddress = AccountAddress.zero();
            Logger.info("   Zero address: %s", zeroAddress);
            Logger.info("   Is zero: %s", zeroAddress.isZero());

            // 5. Client Demo (commented out to avoid network calls)
            Logger.info("");
            Logger.info("5. Client Operations:");
            Logger.info("   Note: Network operations are commented out to avoid external dependencies");
            /*
            AptosConfig config = AptosConfig.builder()
                .network(AptosConfig.Network.DEVNET)
                .build();
            AptosClient client = new AptosClient(config);
            
            // Get account info
            var accountInfo = client.getAccount(account.getAccountAddress());
            Logger.info("   Account sequence number: %s", accountInfo.getSequenceNumber());
            
            // Get ledger info
            var ledgerInfo = client.getLedgerInfo();
            Logger.info("   Chain ID: %s", ledgerInfo.getChainId());
            Logger.info("   Ledger version: %s", ledgerInfo.getLedgerVersion());
            */

            Logger.info("");
            Logger.info("=== Demo completed successfully! ===");

        } catch (Exception e) {
            Logger.error("Error in demo", e);
        }
    }
}
