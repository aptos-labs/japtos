package com.aptoslabs.japtos.account;

import com.aptoslabs.japtos.core.crypto.Ed25519PrivateKey;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.core.crypto.PublicKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Branch-focused tests for {@link MultiKeyAccount} and {@link MultiEd25519Account}
 * input validation and signer-index sorting.
 */
class MultiAccountBranchTest {

    @Test
    @DisplayName("MultiKey rejects non-Ed25519 signers")
    void multiKeyRejectsNonEd25519Signer() {
        Ed25519Account ed = Account.generate();
        MultiKeyAccount notAnEd25519Signer = MultiKeyAccount.fromPrivateKeys(
                List.of(Ed25519PrivateKey.generate(), Ed25519PrivateKey.generate()), 2);
        List<PublicKey> pubKeys = List.of(ed.getPublicKey());
        assertThrows(IllegalArgumentException.class,
                () -> MultiKeyAccount.fromPublicKeysAndSigners(pubKeys, List.of(notAnEd25519Signer), 1));
    }

    @Test
    @DisplayName("MultiKey rejects a signer whose key is absent from the key set")
    void multiKeyRejectsUnknownSigner() {
        Ed25519Account inSet = Account.generate();
        Ed25519Account stranger = Account.generate();
        List<PublicKey> pubKeys = List.of(inSet.getPublicKey());
        assertThrows(IllegalArgumentException.class,
                () -> MultiKeyAccount.fromPublicKeysAndSigners(pubKeys, List.of(stranger), 1));
    }

    @Test
    @DisplayName("MultiKey sorts signers by ascending public-key index")
    void multiKeySortsSigners() {
        Ed25519Account a0 = Account.generate();
        Ed25519Account a1 = Account.generate();
        List<Ed25519PublicKey> pubKeys = List.of(a0.getPublicKey(), a1.getPublicKey());
        // Provide signers out of order (index 1 before index 0) to exercise the sort branch.
        MultiKeyAccount multi = MultiKeyAccount.from(List.of(a1, a0), pubKeys, 2);
        assertEquals(List.of(0, 1), multi.getSignerIndices());
    }

    @Test
    @DisplayName("MultiEd25519 rejects non-Ed25519 signers")
    void multiEd25519RejectsNonEd25519Signer() {
        Ed25519Account ed = Account.generate();
        MultiEd25519Account notEd = MultiEd25519Account.fromPrivateKeys(
                List.of(Ed25519PrivateKey.generate(), Ed25519PrivateKey.generate()), 2);
        List<Ed25519PublicKey> pubKeys = List.of(ed.getPublicKey());
        assertThrows(IllegalArgumentException.class,
                () -> MultiEd25519Account.from(List.of(notEd), pubKeys, 1));
    }

    @Test
    @DisplayName("MultiEd25519 sorts signers by ascending public-key index")
    void multiEd25519SortsSigners() {
        Ed25519Account a0 = Account.generate();
        Ed25519Account a1 = Account.generate();
        List<Ed25519PublicKey> pubKeys = List.of(a0.getPublicKey(), a1.getPublicKey());
        MultiEd25519Account multi = MultiEd25519Account.from(List.of(a1, a0), pubKeys, 2);
        assertEquals(List.of(0, 1), multi.getSignerIndices());
    }

    @Test
    @DisplayName("MultiKey getPublicKey returns the first Ed25519 key amongst mixed keys")
    void multiKeyGetPublicKeySkipsNonEd25519() {
        // Build a key list where a keyless key precedes the Ed25519 key, so getPublicKey()
        // must skip the keyless entry. The signer is the Ed25519 account at index 1.
        Ed25519Account ed = Account.generate();
        com.aptoslabs.japtos.core.crypto.KeylessPublicKey keyless =
                new com.aptoslabs.japtos.core.crypto.KeylessPublicKey("iss", new byte[32]);
        List<PublicKey> pubKeys = new ArrayList<>();
        pubKeys.add(keyless);
        pubKeys.add(ed.getPublicKey());

        MultiKeyAccount multi = MultiKeyAccount.fromPublicKeysAndSigners(pubKeys, List.of(ed), 1);
        assertEquals(ed.getPublicKey(), multi.getPublicKey());
    }
}
