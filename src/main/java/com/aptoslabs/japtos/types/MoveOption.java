package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AccountAddress;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Represents a Move Option type, which can contain either a value (Some) or no value (None).
 * 
 * Move Option is internally represented as a vector with 0 or 1 elements,
 * matching the Move standard library implementation and TypeScript SDK.
 * 
 * @param <T> The type of the optional value, must extend TransactionArgument
 */
public class MoveOption<T extends TransactionArgument> extends TransactionArgument {
    private final List<T> vec;
    
    /**
     * Creates a MoveOption with a value (Some)
     * @param value The value to wrap, must not be null
     */
    public MoveOption(T value) {
        if (value == null) {
            this.vec = new ArrayList<>(0);
        } else {
            this.vec = new ArrayList<>(1);
            this.vec.add(value);
        }
    }
    
    /**
     * Creates an empty MoveOption (None)
     */
    public MoveOption() {
        this.vec = new ArrayList<>(0);
    }
    
    /**
     * Creates a MoveOption from a nullable value
     * @param value The value to wrap, can be null (resulting in None)
     * @return A MoveOption containing the value or empty if null
     */
    public static <T extends TransactionArgument> MoveOption<T> of(T value) {
        return new MoveOption<>(value);
    }
    
    /**
     * Creates an empty MoveOption (None)
     * @return An empty MoveOption
     */
    public static <T extends TransactionArgument> MoveOption<T> empty() {
        return new MoveOption<>();
    }
    
    /**
     * Creates a MoveOption from a Java Optional
     * 
     * @param optional The Optional to convert
     * @param mapper Function to convert the Optional's value to a TransactionArgument
     * @return A MoveOption containing the mapped value or empty
     */
    public static <T, U extends TransactionArgument> MoveOption<U> fromOptional(
            Optional<T> optional, Function<T, U> mapper) {
        return optional.map(mapper).map(MoveOption::of).orElse(empty());
    }
    
    /**
     * Checks if this MoveOption contains a value
     * 
     * @return true if this contains a value (Some), false if empty (None)
     */
    public boolean isSome() {
        return vec.size() == 1;
    }
    
    /**
     * Checks if this MoveOption is empty
     * 
     * @return true if this is empty (None), false if it contains a value (Some)
     */
    public boolean isNone() {
        return vec.isEmpty();
    }
    
    /**
     * Gets the contained value
     * 
     * @return The contained value
     * @throws IllegalStateException if this MoveOption is empty
     */
    public T unwrap() {
        if (isNone()) {
            throw new IllegalStateException("Called unwrap on a MoveOption with no value");
        }
        return vec.get(0);
    }
    
    /**
     * Gets the contained value or returns a default
     * 
     * @param defaultValue The default value to return if empty
     * @return The contained value or the default
     */
    public T unwrapOr(T defaultValue) {
        return isSome() ? vec.get(0) : defaultValue;
    }
    
    /**
     * Converts this MoveOption to a Java Optional
     * 
     * @return An Optional containing the value or empty
     */
    public Optional<T> toOptional() {
        return isSome() ? Optional.of(vec.get(0)) : Optional.empty();
    }
    
    /**
     * Maps the contained value using the provided function
     * 
     * @param mapper The function to apply to the contained value
     * @return A new MoveOption with the mapped value or empty if this is empty
     */
    public <U extends TransactionArgument> MoveOption<U> map(Function<T, U> mapper) {
        return isSome() ? of(mapper.apply(vec.get(0))) : empty();
    }
    
    @Override
    public void serialize(Serializer serializer) throws IOException {
        // MoveOption serializes as a vector: length followed by elements
        // For script arguments, we need the type tag prefix
        throw new UnsupportedOperationException(
            "MoveOption should use serializeForEntryFunction for entry functions");
    }
    
    /**
     * Serializes this MoveOption for use in entry functions.
     * The format is: ULEB128 length (0 or 1) followed by the element if present
     * 
     * @return The BCS serialized bytes
     */
    public byte[] serializeForEntryFunction() throws IOException {
        Serializer serializer = new Serializer();
        
        // Serialize as vector: length first
        serializer.serializeU32AsUleb128(vec.size());
        
        // Then serialize the element if present
        if (isSome()) {
            T value = vec.get(0);
            // Each element serializes its raw value for entry functions
            if (value instanceof TransactionArgument.AccountAddress) {
                serializer.serializeAccountAddress(((TransactionArgument.AccountAddress) value).getValue());
            } else if (value instanceof TransactionArgument.U64) {
                serializer.serializeU64(((TransactionArgument.U64) value).getValue());
            } else if (value instanceof TransactionArgument.U128) {
                BigInteger val = ((TransactionArgument.U128) value).getValue();
                byte[] bytes = val.toByteArray();
                byte[] padded = new byte[16];
                if (bytes[0] < 0) {
                    Arrays.fill(padded, (byte) 0);
                }
                int srcPos = Math.max(0, bytes.length - 16);
                int destPos = Math.max(0, 16 - bytes.length);
                int length = Math.min(bytes.length, 16);
                System.arraycopy(bytes, srcPos, padded, destPos, length);
                
                // Convert to little-endian
                byte[] littleEndian = new byte[16];
                for (int i = 0; i < 16; i++) {
                    littleEndian[i] = padded[15 - i];
                }
                serializer.serializeU128(littleEndian);
            } else if (value instanceof TransactionArgument.U8) {
                serializer.serializeU8(((TransactionArgument.U8) value).getValue());
            } else if (value instanceof TransactionArgument.U16) {
                serializer.serializeU16(((TransactionArgument.U16) value).getValue());
            } else if (value instanceof TransactionArgument.U32) {
                serializer.serializeU32(((TransactionArgument.U32) value).getValue());
            } else if (value instanceof TransactionArgument.U256) {
                BigInteger val = ((TransactionArgument.U256) value).getValue();
                byte[] bytes = val.toByteArray();
                byte[] padded = new byte[32];
                if (bytes[0] < 0) {
                    Arrays.fill(padded, (byte) 0);
                }
                int srcPos = Math.max(0, bytes.length - 32);
                int destPos = Math.max(0, 32 - bytes.length);
                int length = Math.min(bytes.length, 32);
                System.arraycopy(bytes, srcPos, padded, destPos, length);
                
                // Convert to little-endian
                byte[] littleEndian = new byte[32];
                for (int i = 0; i < 32; i++) {
                    littleEndian[i] = padded[31 - i];
                }
                serializer.serializeU256(littleEndian);
            } else if (value instanceof TransactionArgument.Bool) {
                serializer.serializeBool(((TransactionArgument.Bool) value).getValue());
            } else if (value instanceof TransactionArgument.String) {
                // Move String serializes as length-prefixed bytes
                byte[] strBytes = ((TransactionArgument.String) value).getValue();
                serializer.serializeBytes(strBytes);
            } else if (value instanceof TransactionArgument.U8Vector) {
                byte[] bytes = ((TransactionArgument.U8Vector) value).getValue();
                serializer.serializeBytes(bytes);
            } else {
                throw new IllegalArgumentException("Unsupported MoveOption type: " + value.getClass());
            }
        }
        
        return serializer.toByteArray();
    }
    
    // Factory methods for common types
    
    /**
     * Creates a MoveOption<U8> from a Byte value
     * 
     * @param value The byte value, can be null
     * @return MoveOption containing U8 or empty
     */
    public static MoveOption<TransactionArgument.U8> u8(Byte value) {
        return value != null ? of(new TransactionArgument.U8(value)) : empty();
    }
    
    /**
     * Creates a MoveOption<U16> from a Short value
     * 
     * @param value The short value, can be null
     * @return MoveOption containing U16 or empty
     */
    public static MoveOption<TransactionArgument.U16> u16(Short value) {
        return value != null ? of(new TransactionArgument.U16(value)) : empty();
    }
    
    /**
     * Creates a MoveOption<U32> from an Integer value
     * 
     * @param value The integer value, can be null
     * @return MoveOption containing U32 or empty
     */
    public static MoveOption<TransactionArgument.U32> u32(Integer value) {
        return value != null ? of(new TransactionArgument.U32(value)) : empty();
    }
    
    /**
     * Creates a MoveOption<U64> from a Long value
     * 
     * @param value The long value, can be null
     * @return MoveOption containing U64 or empty
     */
    public static MoveOption<TransactionArgument.U64> u64(Long value) {
        return value != null ? of(new TransactionArgument.U64(value)) : empty();
    }
    
    /**
     * Creates a MoveOption<U128> from a BigInteger value
     * 
     * @param value The BigInteger value, can be null
     * @return MoveOption containing U128 or empty
     */
    public static MoveOption<TransactionArgument.U128> u128(BigInteger value) {
        return value != null ? of(new TransactionArgument.U128(value)) : empty();
    }
    
    /**
     * Creates a MoveOption<U256> from a BigInteger value
     * 
     * @param value The BigInteger value, can be null
     * @return MoveOption containing U256 or empty
     */
    public static MoveOption<TransactionArgument.U256> u256(BigInteger value) {
        return value != null ? of(new TransactionArgument.U256(value)) : empty();
    }
    
    /**
     * Creates a MoveOption<Bool> from a Boolean value
     * 
     * @param value The boolean value, can be null
     * @return MoveOption containing Bool or empty
     */
    public static MoveOption<TransactionArgument.Bool> bool(Boolean value) {
        return value != null ? of(new TransactionArgument.Bool(value)) : empty();
    }
    
    /**
     * Creates a MoveOption<String> from a Java String value
     * 
     * @param value The string value, can be null
     * @return MoveOption containing String or empty
     */
    public static MoveOption<TransactionArgument.String> string(java.lang.String value) {
        return value != null ? of(new TransactionArgument.String(value)) : empty();
    }
    
    /**
     * Creates a MoveOption<AccountAddress> from an AccountAddress value
     * 
     * @param value The account address, can be null
     * @return MoveOption containing AccountAddress or empty
     */
    public static MoveOption<TransactionArgument.AccountAddress> address(com.aptoslabs.japtos.core.AccountAddress value) {
        return value != null ? of(new TransactionArgument.AccountAddress(value)) : empty();
    }
    
    /**
     * Creates a MoveOption<U8Vector> from a byte array
     * 
     * @param value The byte array, can be null
     * @return MoveOption containing U8Vector or empty
     */
    public static MoveOption<TransactionArgument.U8Vector> u8Vector(byte[] value) {
        return value != null ? of(new TransactionArgument.U8Vector(value)) : empty();
    }
    
    @Override
    public java.lang.String toString() {
        return isSome() ? "Some(" + vec.get(0) + ")" : "None";
    }
}
