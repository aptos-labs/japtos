package com.aptoslabs.japtos;

import com.aptoslabs.japtos.account.Account;
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.core.crypto.Ed25519PrivateKey;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for validating Account.fromDerivationPath functionality.
 *
 * <p>This test class validates that the BIP44 derivation path implementation
 * correctly derives Ed25519 accounts from mnemonic seed phrases according
 * to the Aptos standard (coin type 637).</p>
 *
 * <p>The test uses a known mnemonic and validates against pre-computed
 * public and private keys derived using the TypeScript SDK to ensure
 * compatibility across implementations.</p>
 */
class AccountDerivedPathTests {

    /**
     * Test mnemonic phrase used for derivation validation.
     * This mnemonic was used with the TypeScript SDK to generate the expected keys.
     */
    private static final String TEST_MNEMONIC = "defense balance boat index fatal book remain champion cushion city escape huge";

    /**
     * Expected public key derived from the test mnemonic at path m/44'/637'/0'/0'/0'.
     * This value was generated using the TypeScript SDK for validation.
     */
    private static final String EXPECTED_PUBLIC_KEY = "0x82b5212477e1ce276ea404f4d1143b88b0716a36faea88de942e2d816bbd5bb2";

    /**
     * Expected private key derived from the test mnemonic at path m/44'/637'/0'/0'/0'.
     * This value was generated using the TypeScript SDK for validation.
     * Note: The prefix "ed25519-priv-" is TypeScript SDK specific and not used in Java.
     */
    private static final String EXPECTED_PRIVATE_KEY = "0xb769d8ddc5973c5c92b785e155243b94af32267284161fcd85b63a0a47384c8c";

    /**
     * Standard BIP44 derivation path for the first Aptos account.
     * Format: m/44'/637'/account'/change'/address_index'
     * - 44' = BIP44 purpose
     * - 637' = Aptos coin type
     * - 0' = First account
     * - 0' = First change index
     * - 0' = First address index
     */
    private static final String DERIVATION_PATH = "m/44'/637'/0'/0'/0'";

    @Test
    @DisplayName("Derive account from mnemonic and path - should match TypeScript SDK results")
    void testFromDerivationPath() {
        System.out.println("=== Testing Account.fromDerivationPath ===");
        System.out.println("Mnemonic: " + TEST_MNEMONIC);
        System.out.println("Path: " + DERIVATION_PATH);

        // Derive account using our Java implementation
        Ed25519Account account = Account.fromDerivationPath(DERIVATION_PATH, TEST_MNEMONIC);

        assertNotNull(account, "Derived account should not be null");
        assertNotNull(account.getPublicKey(), "Public key should not be null");
        assertNotNull(account.getPrivateKey(), "Private key should not be null");
        assertNotNull(account.getAccountAddress(), "Account address should not be null");

        // Validate the derived keys match the expected values from TypeScript SDK
        String actualPublicKey = account.getPublicKey().toString();
        String actualPrivateKey = account.getPrivateKey().toString();

        System.out.println("Expected public key:  " + EXPECTED_PUBLIC_KEY);
        System.out.println("Actual public key:    " + actualPublicKey);
        System.out.println("Expected private key: " + EXPECTED_PRIVATE_KEY);
        System.out.println("Actual private key:   " + actualPrivateKey);
        System.out.println("Account address:      " + account.getAccountAddress());

        assertEquals(EXPECTED_PUBLIC_KEY, actualPublicKey,
                "Public key should match the TypeScript SDK derived value");
        assertEquals(EXPECTED_PRIVATE_KEY, actualPrivateKey,
                "Private key should match the TypeScript SDK derived value");

        // Verify the account properties
        assertEquals(Account.SigningScheme.ED25519, account.getSigningScheme(),
                "Should use ED25519 signing scheme");

        // Verify public key derivation consistency
        Ed25519PublicKey derivedPublicKey = account.getPrivateKey().publicKey();
        assertEquals(account.getPublicKey().toString(), derivedPublicKey.toString(),
                "Public key derived from private key should match account public key");

        System.out.println("✓ All validations passed!");
    }

    @Test
    @DisplayName("Test direct Ed25519PrivateKey.fromDerivationPath")
    void testPrivateKeyFromDerivationPath() {
        System.out.println("=== Testing Ed25519PrivateKey.fromDerivationPath ===");

        // Test direct private key derivation
        Ed25519PrivateKey privateKey = Ed25519PrivateKey.fromDerivationPath(DERIVATION_PATH, TEST_MNEMONIC);

        assertNotNull(privateKey, "Derived private key should not be null");
        assertEquals(EXPECTED_PRIVATE_KEY, privateKey.toString(),
                "Private key should match expected value");

        // Verify public key derivation
        Ed25519PublicKey publicKey = privateKey.publicKey();
        assertEquals(EXPECTED_PUBLIC_KEY, publicKey.toString(),
                "Derived public key should match expected value");

        System.out.println("Private key: " + privateKey);
        System.out.println("Public key:  " + publicKey);
        System.out.println("✓ Direct private key derivation test passed!");
    }

    @Test
    @DisplayName("Test multiple derivation paths")
    void testMultipleDerivationPaths() {
        System.out.println("=== Testing Multiple Derivation Paths ===");

        // Test different account indices
        String[] paths = {
                "m/44'/637'/0'/0'/0'",  // First account, first address
                "m/44'/637'/0'/0'/1'",  // First account, second address
                "m/44'/637'/1'/0'/0'",  // Second account, first address
        };

        Ed25519Account[] accounts = new Ed25519Account[paths.length];

        for (int i = 0; i < paths.length; i++) {
            accounts[i] = Account.fromDerivationPath(paths[i], TEST_MNEMONIC);
            assertNotNull(accounts[i], "Account " + i + " should not be null");

            System.out.println("Path " + paths[i] + ":");
            System.out.println("  Public key: " + accounts[i].getPublicKey().toString());
            System.out.println("  Address:    " + accounts[i].getAccountAddress());
        }

        // Verify all accounts are different
        for (int i = 0; i < accounts.length; i++) {
            for (int j = i + 1; j < accounts.length; j++) {
                assertNotEquals(accounts[i].getPublicKey().toString(),
                        accounts[j].getPublicKey().toString(),
                        "Accounts " + i + " and " + j + " should have different public keys");
                assertNotEquals(accounts[i].getAccountAddress().toString(),
                        accounts[j].getAccountAddress().toString(),
                        "Accounts " + i + " and " + j + " should have different addresses");
            }
        }

        System.out.println("✓ Multiple derivation paths test passed!");
    }

    @Test
    @DisplayName("Test invalid derivation paths")
    void testInvalidDerivationPaths() {
        System.out.println("=== Testing Invalid Derivation Paths ===");

        String[] invalidPaths = {
                "m/44'/637'/0'/0/0",     // Missing hardened marker on last component
                "m/44'/636'/0'/0'/0'",   // Wrong coin type
                "m/44/637'/0'/0'/0'",    // Missing hardened marker on purpose
                "m/44'/637'",           // Too short
                "invalid/path",         // Completely invalid
                "",                     // Empty path
        };

        for (String invalidPath : invalidPaths) {
            System.out.println("Testing invalid path: " + invalidPath);

            Exception exception = assertThrows(IllegalArgumentException.class, () -> {
                Account.fromDerivationPath(invalidPath, TEST_MNEMONIC);
            }, "Should throw IllegalArgumentException for invalid path: " + invalidPath);

            System.out.println("  ✓ Correctly rejected: " + exception.getMessage());
        }

        System.out.println("✓ Invalid path validation test passed!");
    }

    @Test
    @DisplayName("Test deterministic derivation")
    void testDeterministicDerivation() {
        System.out.println("=== Testing Deterministic Derivation ===");

        // Derive the same account multiple times and verify consistency
        Ed25519Account account1 = Account.fromDerivationPath(DERIVATION_PATH, TEST_MNEMONIC);
        Ed25519Account account2 = Account.fromDerivationPath(DERIVATION_PATH, TEST_MNEMONIC);
        Ed25519Account account3 = Account.fromDerivationPath(DERIVATION_PATH, TEST_MNEMONIC);

        // All accounts should be identical
        assertEquals(account1.getPrivateKey().toString(), account2.getPrivateKey().toString(),
                "Private keys should be identical for same path and mnemonic");
        assertEquals(account1.getPublicKey().toString(), account2.getPublicKey().toString(),
                "Public keys should be identical for same path and mnemonic");
        assertEquals(account1.getAccountAddress().toString(), account2.getAccountAddress().toString(),
                "Account addresses should be identical for same path and mnemonic");

        assertEquals(account1.getPrivateKey().toString(), account3.getPrivateKey().toString(),
                "Private keys should be identical for same path and mnemonic (third derivation)");
        assertEquals(account1.getPublicKey().toString(), account3.getPublicKey().toString(),
                "Public keys should be identical for same path and mnemonic (third derivation)");
        assertEquals(account1.getAccountAddress().toString(), account3.getAccountAddress().toString(),
                "Account addresses should be identical for same path and mnemonic (third derivation)");

        System.out.println("Derived account consistently:");
        System.out.println("  Public key: " + account1.getPublicKey().toString());
        System.out.println("  Address:    " + account1.getAccountAddress());
        System.out.println("✓ Deterministic derivation test passed!");
    }
}
