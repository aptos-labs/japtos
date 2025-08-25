package com.aptoslabs.japtos.transaction.authenticator;

import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;

/**
 * TransactionAuthenticator for SingleSender that wraps an AccountAuthenticator.
 * <p>
 * According to the TS SDK, MultiKey is an AccountAuthenticator variant and
 * must be wrapped inside a TransactionAuthenticator.SingleSender (variant 4).
 */
public class TransactionAuthenticatorSingleSender implements Serializable {
    private static final int TRANSACTION_AUTHENTICATOR_VARIANT_SINGLE_SENDER = 4;

    private final AccountAuthenticator senderAuthenticator;

    public TransactionAuthenticatorSingleSender(AccountAuthenticator senderAuthenticator) {
        this.senderAuthenticator = senderAuthenticator;
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        // TransactionAuthenticator variant: 4 = SingleSender
        serializer.serializeU32AsUleb128(TRANSACTION_AUTHENTICATOR_VARIANT_SINGLE_SENDER);
        // Then write the embedded AccountAuthenticator (e.g., MultiKey)
        senderAuthenticator.serialize(serializer);
    }
}

