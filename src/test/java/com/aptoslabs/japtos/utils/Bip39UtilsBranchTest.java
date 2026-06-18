package com.aptoslabs.japtos.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Branch-focused tests for {@link Bip39Utils#entropyToMnemonic(String)}, covering the
 * short-input padding path, the long-input slicing path, and determinism.
 */
class Bip39UtilsBranchTest {

    @Test
    @DisplayName("Short inputs are right-padded to 16 bytes and yield a 12-word mnemonic")
    void shortInputIsPadded() {
        String mnemonic = Bip39Utils.entropyToMnemonic("a");
        assertEquals(12, mnemonic.split(" ").length);
        // Every word must come from the BIP-39 wordlist (lowercase ASCII letters only).
        for (String word : mnemonic.split(" ")) {
            assertTrue(word.matches("[a-z]+"), "unexpected word: " + word);
        }
    }

    @Test
    @DisplayName("Long inputs are sliced to the first 16 bytes (32 hex chars)")
    void longInputIsSliced() {
        // 20-character ASCII input -> 40 hex chars -> sliced to 32 -> 16 bytes.
        String mnemonic = Bip39Utils.entropyToMnemonic("abcdefghijklmnopqrst");
        assertEquals(12, mnemonic.split(" ").length);

        // Slicing means the suffix beyond 16 bytes does not affect the result.
        String samePrefix = Bip39Utils.entropyToMnemonic("abcdefghijklmnopXXXX");
        assertEquals(mnemonic, samePrefix);
    }

    @Test
    @DisplayName("entropyToMnemonic is deterministic and distinguishes different inputs")
    void deterministicAndDistinct() {
        assertEquals(Bip39Utils.entropyToMnemonic("seed-one"),
                Bip39Utils.entropyToMnemonic("seed-one"));
        assertNotEquals(Bip39Utils.entropyToMnemonic("seed-one"),
                Bip39Utils.entropyToMnemonic("seed-two"));
    }
}
