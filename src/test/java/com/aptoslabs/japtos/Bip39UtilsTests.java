package com.aptoslabs.japtos;

import com.aptoslabs.japtos.utils.Logger;

import com.aptoslabs.japtos.utils.Bip39Utils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for validating BIP39 mnemonic generation functionality.
 */
public class Bip39UtilsTests {

    @Test
    @DisplayName("Test entropy to mnemonic conversion")
    void testEntropyToMnemonic() {
        // Test entropy string (with hyphens)
        String entropy = "9b4c9e83-a06e-4704-bc5f-b6a55d0dbb89";

        // Expected mnemonic phrase
        String expectedMnemonic = "defense balance boat index fatal book remain champion cushion city escape huge";

        // Convert entropy to mnemonic
        String actualMnemonic = Bip39Utils.entropyToMnemonic(entropy);

        // Verify the result
        assertEquals(expectedMnemonic, actualMnemonic,
                "Mnemonic generation should match expected phrase");

        Logger.info("Input entropy: " + entropy);
        Logger.info("Generated mnemonic: " + actualMnemonic);
    }

    @Test
    @DisplayName("Test entropy to mnemonic conversion")
    void testEntropyToMnemonic2() {
        // Test entropy string (with hyphens)
        String entropy = "c63839ab-c50d-4c30-a47a-01d9df5b6426";

        // Expected mnemonic phrase
        String expectedMnemonic = "glimpse ranch sock grid noodle rain remain grit corn cannon escape shop";

        // Convert entropy to mnemonic
        String actualMnemonic = Bip39Utils.entropyToMnemonic(entropy);

        // Verify the result
        assertEquals(expectedMnemonic, actualMnemonic,
                "Mnemonic generation should match expected phrase");

        Logger.info("Input entropy: " + entropy);
        Logger.info("Generated mnemonic: " + actualMnemonic);
    }

}
