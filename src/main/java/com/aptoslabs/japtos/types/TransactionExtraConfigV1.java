package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AccountAddress;

import java.io.IOException;

/**
 * TransactionExtraConfig V1, parity with TS SDK.
 * Encodes:
 *  - variant: V1 = 0 (ULEB128)
 *  - Option<AccountAddress> multisigAddress
 *  - Option<U64> replayProtectionNonce
 */
public class TransactionExtraConfigV1 implements TransactionExtraConfig {
    private static final int TRANSACTION_EXTRA_CONFIG_VARIANT_V1 = 0;

    private final AccountAddress multisigAddress; // optional
    private final Long replayProtectionNonce; // optional

    public TransactionExtraConfigV1(AccountAddress multisigAddress, Long replayProtectionNonce) {
        this.multisigAddress = multisigAddress;
        this.replayProtectionNonce = replayProtectionNonce;
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        serializer.serializeU32AsUleb128(TRANSACTION_EXTRA_CONFIG_VARIANT_V1);

        // Option<AccountAddress>
        if (multisigAddress == null) {
            serializer.serializeBool(false);
        } else {
            serializer.serializeBool(true);
            serializer.serializeAccountAddress(multisigAddress);
        }

        // Option<U64>
        if (replayProtectionNonce == null) {
            serializer.serializeBool(false);
        } else {
            serializer.serializeBool(true);
            serializer.serializeU64(replayProtectionNonce);
        }
    }
}
