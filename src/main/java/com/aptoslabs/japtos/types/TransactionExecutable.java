package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;

/**
 * Abstraction for a transaction executable used by orderless transactions.
 */
public interface TransactionExecutable extends com.aptoslabs.japtos.bcs.Serializable {
    @Override
    void serialize(Serializer serializer) throws IOException;
}
