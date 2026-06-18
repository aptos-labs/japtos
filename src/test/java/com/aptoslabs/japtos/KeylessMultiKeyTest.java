package com.aptoslabs.japtos;

import com.aptoslabs.japtos.utils.Logger;

import com.aptoslabs.japtos.account.Account;
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.account.MultiKeyAccount;
import com.aptoslabs.japtos.core.crypto.KeylessPublicKey;
import com.aptoslabs.japtos.core.crypto.PublicKey;
import com.aptoslabs.japtos.utils.Bip39Utils;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MultiKey accounts with Keyless public keys.
 * Verifies address derivation matches TypeScript SDK behavior.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class KeylessMultiKeyTest {

    private static final String DERIVATION_PATH = "m/44'/637'/0'/0'/0'";

    @Test
    @Order(1)
    @DisplayName("Test MultiKey address derivation with Keyless + Ed25519 keys")
    void testKeylessMultiKeyAddressDerivation() throws Exception {
        Logger.info("\n=== Testing Keyless MultiKey Address Derivation ===");
        
        // 1. Derive wallet from UUID (matching TypeScript implementation)
        // Note: Taking first 32 characters of UUID string (with hyphens) as entropy
        String uuid = "46f6393c-51ce-40d7-9006-d01c59f4dd83";
        Logger.info("UUID: " + uuid);
        
        // Take first 32 characters as entropy (includes hyphens)
        String entropy = uuid.substring(0, 32);
        Logger.info("Entropy (first 32 chars): " + entropy);
        
        // Convert to mnemonic
        String mnemonic = Bip39Utils.entropyToMnemonic(entropy);
        Logger.info("Mnemonic: " + mnemonic);
        
        // Derive account from mnemonic
        Ed25519Account passWallet = Account.fromDerivationPath(DERIVATION_PATH, mnemonic);
        Logger.info("Pass wallet address: " + passWallet.getAccountAddress());
        Logger.info("Pass wallet public key: " + passWallet.getPublicKey());
        
        // 2. Deserialize keyless public key from hex
        String keylessHex = "1b68747470733a2f2f6163636f756e74732e676f6f676c652e636f6d2049b12b386092b5efeca7f8c0ecf5dd0607913dbd7f921ce45d3d103689c7a921";
        KeylessPublicKey keylessKey = KeylessPublicKey.fromHexString(keylessHex);
        Logger.info("Keyless public key ISS: " + keylessKey.getIss());
        
        // 3. Create MultiKey account with publicKeys in order: [keyless, passWallet]
        List<PublicKey> publicKeys = Arrays.asList(keylessKey, passWallet.getPublicKey());
        List<Account> signers = Arrays.asList(passWallet);
        
        MultiKeyAccount multiKey = MultiKeyAccount.fromPublicKeysAndSigners(publicKeys, signers, 1);
        
        // 4. Verify address matches expected value
        String expectedAddress = "0x4e056eb1b087ac86f98b8bd831d57bfb66e2ae87a85fe99301628bac28216d83";
        String actualAddress = multiKey.getAccountAddress().toString();
        
        Logger.info("\nMultiKey account address: " + actualAddress);
        
        assertEquals(expectedAddress, actualAddress, 
            "MultiKey account address should match TypeScript SDK derivation");
        
        Logger.info("\n✓ Keyless MultiKey address derivation successful!");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test KeylessPublicKey deserialization")
    void testKeylessPublicKeyDeserialization() throws Exception {
        Logger.info("\n=== Testing KeylessPublicKey Deserialization ===");
        
        String keylessHex = "1b68747470733a2f2f6163636f756e74732e676f6f676c652e636f6d2049b12b386092b5efeca7f8c0ecf5dd0607913dbd7f921ce45d3d103689c7a921";
        
        KeylessPublicKey keylessKey = KeylessPublicKey.fromHexString(keylessHex);
        
        assertNotNull(keylessKey);
        assertNotNull(keylessKey.getIss());
        assertEquals(32, keylessKey.getIdCommitment().length);
        
        Logger.info("KeylessPublicKey:");
        Logger.info("  ISS: " + keylessKey.getIss());
        Logger.info("  ID Commitment: " + keylessKey.getIdCommitment().length + " bytes");
        Logger.info("  toString(): " + keylessKey.toString());
        
        Logger.info("\n✓ KeylessPublicKey deserialization successful!");
    }
}

