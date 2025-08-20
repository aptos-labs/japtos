package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;

/**
 * Entry function payload.

 */
public class EntryFunctionPayload implements TransactionPayload {
    private final ModuleId moduleId;
    private final Identifier functionName;
    private final List<TypeTag> typeArguments;
    private final List<TransactionArgument> arguments;
    
    public EntryFunctionPayload(
        ModuleId moduleId,
        Identifier functionName,
        List<TypeTag> typeArguments,
        List<TransactionArgument> arguments
    ) {
        this.moduleId = moduleId;
        this.functionName = functionName;
        this.typeArguments = typeArguments;
        this.arguments = arguments;
    }
    
    @Override
    public void serialize(Serializer serializer) throws IOException {
        // Serialize payload type (2 for entry function) as ULEB128
        serializer.serializeU32AsUleb128(2);
        
        // Serialize module ID
        moduleId.serialize(serializer);
        
        // Serialize function name
        functionName.serialize(serializer);
        
        // Serialize type arguments
        serializer.serializeU32AsUleb128(typeArguments.size());
        for (TypeTag typeTag : typeArguments) {
            typeTag.serialize(serializer);
        }
        
        // Serialize arguments
        // In Aptos TS SDK, entry function args are serialized as length-prefixed bytes
        serializer.serializeU32AsUleb128(arguments.size());
        for (TransactionArgument argument : arguments) {
            // Serialize the argument as length-prefixed bytes (like serializeForEntryFunction in TS SDK)
            if (argument instanceof TransactionArgument.AccountAddress) {
                byte[] argBytes = ((TransactionArgument.AccountAddress) argument).serializeForEntryFunction();
                serializer.serializeBytes(argBytes);
            } else if (argument instanceof TransactionArgument.U64) {
                byte[] argBytes = ((TransactionArgument.U64) argument).serializeForEntryFunction();
                serializer.serializeBytes(argBytes);
            } else {
                byte[] argBytes = argument.bcsToBytes();
                serializer.serializeBytes(argBytes);
            }
        }
    }
    
    public ModuleId getModuleId() {
        return moduleId;
    }
    
    public Identifier getFunctionName() {
        return functionName;
    }
    
    public List<TypeTag> getTypeArguments() {
        return typeArguments;
    }
    
    public List<TransactionArgument> getArguments() {
        return arguments;
    }
    
    /**
     * Factory method to create entry function payload
     */
    public static EntryFunctionPayload create(
        ModuleId moduleId,
        Identifier functionName,
        List<TypeTag> typeArguments,
        List<TransactionArgument> arguments
    ) {
        return new EntryFunctionPayload(moduleId, functionName, typeArguments, arguments);
    }
    
    /**
     * Factory method to create entry function payload with varargs
     */
    public static EntryFunctionPayload create(
        ModuleId moduleId,
        Identifier functionName,
        List<TypeTag> typeArguments,
        TransactionArgument... arguments
    ) {
        return new EntryFunctionPayload(moduleId, functionName, typeArguments, Arrays.asList(arguments));
    }
    
    @Override
    public String toString() {
        return "EntryFunctionPayload{" +
            "moduleId=" + moduleId +
            ", functionName=" + functionName +
            ", typeArguments=" + typeArguments +
            ", arguments=" + arguments +
            '}';
    }
}
