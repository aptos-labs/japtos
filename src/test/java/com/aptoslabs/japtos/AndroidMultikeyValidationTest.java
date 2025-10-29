package com.aptoslabs.japtos;

import com.aptoslabs.japtos.account.Account;
import com.aptoslabs.japtos.account.MultiKeyAccount;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.utils.Bip39Utils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for validating Android multi-key wallet derivation
 */
class AndroidMultikeyValidationTest {

    @Test
    public void testAndroidMultisigGeneration() {
        String mnemonic = Bip39Utils.entropyToMnemonic("1fc0206d-548b-4e94-b1ed-8c8bbfe56c1b");


        Account passGenerationWallet = Account.fromDerivationPath("m/44'/637'/0'/0'/0'", mnemonic);

        Ed25519PublicKey systemWalletPublicKey = Ed25519PublicKey.fromHex("0x1d1261e01b8112ffb6908c76253460e4920b022ae299a0bc7024281bcc1ce061");


        // Set up the multi-signature configuration (1-of-2)
        List<Account> signers = List.of(passGenerationWallet);
        List<Ed25519PublicKey> publicKeys = Arrays.asList(
                (Ed25519PublicKey) passGenerationWallet.getPublicKey(),
                systemWalletPublicKey                                        // Second: systemPublicKey (like TypeScript SDK)
        );

        int threshold = 1;

        // Create the multi-signature account using the from() method
        MultiKeyAccount multi = MultiKeyAccount.from(signers, publicKeys, threshold);

        String actualAddress = multi.getAccountAddress().toString();
        String expectedAddress = "0xb335cf90bf63226d39aa31cdfa8e0aff98978579721b85b01fde6ce124942ccd";

        System.out.println("Final result - Expected: " + expectedAddress);
        System.out.println("Final result - Actual: " + actualAddress);

        assertEquals(expectedAddress, actualAddress);
    }


}
