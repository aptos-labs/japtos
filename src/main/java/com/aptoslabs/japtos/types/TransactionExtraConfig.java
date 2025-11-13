package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;

/**
 * Extra configuration for transactions; used by orderless transactions.
 */
public interface TransactionExtraConfig extends com.aptoslabs.japtos.bcs.Serializable {
    @Override
    void serialize(Serializer serializer) throws IOException;
}
