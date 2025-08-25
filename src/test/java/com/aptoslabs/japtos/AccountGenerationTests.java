package com.aptoslabs.japtos;

import com.aptoslabs.japtos.account.Account;
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.core.crypto.Ed25519PrivateKey;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.core.crypto.Signature;
import com.aptoslabs.japtos.utils.HexUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Aptos Java SDK
 */
public class AccountGenerationTests {

    private Ed25519Account account;
    private AccountAddress testAddress;

    @BeforeEach
    public void setUp() {
        account = Account.generate();
        testAddress = AccountAddress.zero();
    }

    /**
     * Test account generation
     */
    @Test
    public void testAccountGeneration() {
        Ed25519Account newAccount = Account.generate();

        assertNotNull(newAccount);
        assertNotNull(newAccount.getPrivateKey());
        assertNotNull(newAccount.getPublicKey());
        assertNotNull(newAccount.getAccountAddress());

        // Verify address length
        assertEquals(32, newAccount.getAccountAddress().toBytes().length);

        // Verify private key length
        assertEquals(32, newAccount.getPrivateKey().toBytes().length);

        // Verify public key length
        assertEquals(32, newAccount.getPublicKey().toBytes().length);
    }

    /**
     * Test account creation from private key
     */
    @Test
    public void testAccountFromPrivateKey() {
        Ed25519Account originalAccount = Account.generate();
        String privateKeyHex = originalAccount.getPrivateKey().toString();

        Ed25519Account restoredAccount = Ed25519Account.fromPrivateKey(Ed25519PrivateKey.fromHex(privateKeyHex));

        assertEquals(originalAccount.getAccountAddress(), restoredAccount.getAccountAddress());
        assertEquals(originalAccount.getPublicKey().toString(), restoredAccount.getPublicKey().toString());
        assertEquals(originalAccount.getPrivateKey().toString(), restoredAccount.getPrivateKey().toString());
    }

    /**
     * Test account address creation and validation
     */
    @Test
    public void testAccountAddress() {
        // Test from hex
        String hexAddress = "0x" + "0".repeat(64);
        AccountAddress address = AccountAddress.fromHex(hexAddress);
        assertEquals(hexAddress, address.toString());

        // Test zero address
        AccountAddress zeroAddress = AccountAddress.zero();
        assertTrue(zeroAddress.isZero());

        // Test invalid address length
        assertThrows(IllegalArgumentException.class, () -> {
            AccountAddress.fromHex("0x123");
        });
    }

    /**
     * Test message signing and verification
     */
    @Test
    public void testMessageSigning() {
        String message = "Hello, Aptos!";
        byte[] messageBytes = message.getBytes();

        // Sign message
        Signature signature = account.sign(messageBytes);
        assertNotNull(signature);
        assertEquals(64, signature.toBytes().length);

        // Verify signature
        assertTrue(account.verifySignature(messageBytes, signature));

        // Verify wrong message fails
        String wrongMessage = "Wrong message";
        byte[] wrongMessageBytes = wrongMessage.getBytes();
        assertFalse(account.verifySignature(wrongMessageBytes, signature));
    }

    /**
     * Test hex utilities
     */
    @Test
    public void testHexUtils() {
        byte[] originalBytes = {0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF};

        // Test bytes to hex
        String hex = HexUtils.bytesToHex(originalBytes);
        assertEquals("0123456789abcdef", hex);

        // Test hex to bytes
        byte[] convertedBytes = HexUtils.hexToBytes(hex);
        assertArrayEquals(originalBytes, convertedBytes);

        // Test with 0x prefix
        String hexWithPrefix = "0x" + hex;
        byte[] convertedBytesWithPrefix = HexUtils.hexToBytes(hexWithPrefix);
        assertArrayEquals(originalBytes, convertedBytesWithPrefix);

        // Test validation
        assertTrue(HexUtils.isValidHex(hex));
        assertTrue(HexUtils.isValidHex(hexWithPrefix));
        assertFalse(HexUtils.isValidHex("invalid"));
        assertFalse(HexUtils.isValidHex("123")); // odd length
    }

    /**
     * Test crypto key classes
     */
    @Test
    public void testCryptoKeys() {
        byte[] privateKeyBytes = new byte[32];
        Arrays.fill(privateKeyBytes, (byte) 1);

        byte[] publicKeyBytes = new byte[32];
        Arrays.fill(publicKeyBytes, (byte) 2);

        Ed25519PrivateKey privateKey = Ed25519PrivateKey.fromBytes(privateKeyBytes);
        Ed25519PublicKey publicKey = Ed25519PublicKey.fromBytes(publicKeyBytes);

        assertArrayEquals(privateKeyBytes, privateKey.toBytes());
        assertArrayEquals(publicKeyBytes, publicKey.toBytes());

        // Test invalid key lengths
        assertThrows(IllegalArgumentException.class, () -> {
            Ed25519PrivateKey.fromBytes(new byte[16]);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Ed25519PublicKey.fromBytes(new byte[16]);
        });
    }

    /**
     * Test signature class
     */
    @Test
    public void testSignature() {
        byte[] signatureBytes = new byte[64];
        Arrays.fill(signatureBytes, (byte) 1);

        Signature signature = Signature.fromBytes(signatureBytes);
        assertArrayEquals(signatureBytes, signature.toBytes());

        // Test invalid signature length
        assertThrows(IllegalArgumentException.class, () -> {
            Signature.fromBytes(new byte[32]);
        });
    }

    /**
     * Test basic SDK functionality
     */
    @Test
    public void testBasicFunctionality() {
        assertTrue(true); // Basic test passes
    }
}
