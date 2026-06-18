package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;

/**
 * Orderless transaction inner payload V1 implemented as a TransactionPayload.
 *
 * Serialization:
 *  TransactionPayload variant (Payload = 4)
 *  TransactionInnerPayload variant (V1 = 0)
 *  TransactionExecutable
 *  TransactionExtraConfig
 */
public class TransactionInnerPayloadV1 implements TransactionPayload {
    private static final int TRANSACTION_PAYLOAD_VARIANT_PAYLOAD = 4; // matches TS
    private static final int TRANSACTION_INNER_PAYLOAD_VARIANT_V1 = 0; // matches TS

    private final TransactionExecutable executable;
    private final TransactionExtraConfig extraConfig;

    public TransactionInnerPayloadV1(TransactionExecutable executable, TransactionExtraConfig extraConfig) {
        this.executable = executable;
        this.extraConfig = extraConfig;
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        serializer.serializeU32AsUleb128(TRANSACTION_PAYLOAD_VARIANT_PAYLOAD);
        serializer.serializeU32AsUleb128(TRANSACTION_INNER_PAYLOAD_VARIANT_V1);
        executable.serialize(serializer);
        extraConfig.serialize(serializer);
    }
}
