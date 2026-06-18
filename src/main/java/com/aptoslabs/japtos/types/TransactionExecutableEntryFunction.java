package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * TransactionExecutable variant for EntryFunction, parity with TS SDK.
 *
 * You can construct it from raw parts (module/function/types/args) or reuse an EntryFunctionPayload
 * by stripping its TransactionPayload enum tag and writing only the entry function body.
 */
public class TransactionExecutableEntryFunction implements TransactionExecutable {
    private static final int TRANSACTION_EXECUTABLE_VARIANT_ENTRY_FUNCTION = 1; // matches TS

    private final ModuleId moduleId;
    private final Identifier functionName;
    private final List<TypeTag> typeArguments;
    private final List<TransactionArgument> arguments;
    private final EntryFunctionPayload prebuiltPayload; // optional reuse

    public TransactionExecutableEntryFunction(
            ModuleId moduleId,
            Identifier functionName,
            List<TypeTag> typeArguments,
            List<TransactionArgument> arguments
    ) {
        this.moduleId = moduleId;
        this.functionName = functionName;
        this.typeArguments = typeArguments;
        this.arguments = arguments;
        this.prebuiltPayload = null;
    }

    private TransactionExecutableEntryFunction(EntryFunctionPayload payload) {
        this.moduleId = null;
        this.functionName = null;
        this.typeArguments = null;
        this.arguments = null;
        this.prebuiltPayload = payload;
    }

    public static TransactionExecutableEntryFunction fromEntryFunctionPayload(EntryFunctionPayload payload) {
        return new TransactionExecutableEntryFunction(payload);
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        // Variant: EntryFunction = 1
        serializer.serializeU32AsUleb128(TRANSACTION_EXECUTABLE_VARIANT_ENTRY_FUNCTION);

        if (prebuiltPayload != null) {
            // Reuse EntryFunctionPayload bytes, dropping its TransactionPayload variant (EntryFunction = 2)
            byte[] bytes = prebuiltPayload.bcsToBytes();
            if (bytes.length == 0) {
                throw new IOException("EntryFunctionPayload serialized empty");
            }
            if (bytes[0] != 0x02) {
                throw new IOException("Expected EntryFunction variant (0x02), got: 0x" + String.format("%02X", bytes[0]));
            }
            // Variant 2 is single-byte in ULEB128, validated above
            int offset = 1;
            byte[] inner = Arrays.copyOfRange(bytes, offset, bytes.length);
            serializer.writeBytesDirect(inner);
            return;
        }

        // Serialize EntryFunction body
        moduleId.serialize(serializer);
        functionName.serialize(serializer);
        serializer.serializeU32AsUleb128(typeArguments.size());
        for (TypeTag typeTag : typeArguments) {
            typeTag.serialize(serializer);
        }
        serializer.serializeU32AsUleb128(arguments.size());
        for (TransactionArgument argument : arguments) {
            // Use serializeForEntryFunction() to get raw bytes without variant tags
            byte[] argBytes = argument.serializeForEntryFunction();
            serializer.serializeBytes(argBytes);
        }
    }
}
