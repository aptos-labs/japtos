package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;
import java.util.List;

/**
 * Script payload for transactions
 */
public class ScriptPayload implements TransactionPayload {
    private final byte[] code;
    private final List<TransactionArgument> arguments;
    private final List<TypeTag> typeArguments;

    public ScriptPayload(byte[] code, List<TransactionArgument> arguments, List<TypeTag> typeArguments) {
        this.code = code;
        this.arguments = arguments;
        this.typeArguments = typeArguments;
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        // Serialize payload type (0 for script) as ULEB128
        serializer.serializeU32AsUleb128(0);

        // Serialize code
        serializer.serializeBytes(code);

        // Serialize type arguments
        serializer.serializeU32AsUleb128(typeArguments.size());
        for (TypeTag typeTag : typeArguments) {
            typeTag.serialize(serializer);
        }

        // Serialize arguments
        serializer.serializeU32AsUleb128(arguments.size());
        for (TransactionArgument argument : arguments) {
            argument.serialize(serializer);
        }
    }

    public byte[] getCode() {
        return code;
    }

    public List<TransactionArgument> getArguments() {
        return arguments;
    }

    public List<TypeTag> getTypeArguments() {
        return typeArguments;
    }
}
