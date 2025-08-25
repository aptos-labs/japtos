package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;

/**
 * Interface for transaction payloads.
 */
public interface TransactionPayload extends Serializable {
    @Override
    void serialize(Serializer serializer) throws IOException;
}
