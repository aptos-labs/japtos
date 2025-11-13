package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;

/**
 * Move Option wrapper for entry function arguments.
 *
 * This serializes to the raw BCS Option<T>:
 *  - 0x00 for None
 *  - 0x01 followed by the inner value's entry-function bytes
 */
public class MoveOption<T extends TransactionArgument> extends TransactionArgument {
    private final boolean present;
    private final T value;

    private MoveOption(boolean present, T value) {
        this.present = present;
        this.value = value;
    }

    public static <T extends TransactionArgument> MoveOption<T> empty() {
        return new MoveOption<>(false, null);
    }

    public static <T extends TransactionArgument> MoveOption<T> some(T value) {
        if (value == null) throw new IllegalArgumentException("MoveOption.some value cannot be null");
        return new MoveOption<>(true, value);
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        if (!present) {
            serializer.serializeBool(false);
            return;
        }
        serializer.serializeBool(true);
        // Serialize inner value as entry function bytes; special-case common primitives
        byte[] innerBytes;
        if (value instanceof TransactionArgument.AccountAddress) {
            innerBytes = ((TransactionArgument.AccountAddress) value).serializeForEntryFunction();
        } else if (value instanceof TransactionArgument.U64) {
            innerBytes = ((TransactionArgument.U64) value).serializeForEntryFunction();
        } else {
            // Fallback to BCS bytes
            innerBytes = value.bcsToBytes();
        }
        serializer.writeBytesDirect(innerBytes);
    }

    public boolean isPresent() { return present; }
    public T getValue() { return value; }
}
