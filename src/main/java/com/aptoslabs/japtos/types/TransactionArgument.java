// Copyright © Aptos Foundation
// SPDX-License-Identifier: Apache-2.0

package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

/**
 * Represents a transaction argument.
 */
public abstract class TransactionArgument implements Serializable {
    public abstract void serialize(Serializer serializer) throws IOException;
    
    /**
     * Serializes this argument for use in entry functions.
     * By default, this creates a new serializer and calls bcsToBytes(),
     * but subclasses can override for custom behavior.
     * 
     * @return The BCS serialized bytes for entry function use
     */
    public byte[] serializeForEntryFunction() throws IOException {
        return bcsToBytes();
    }

    /**
     * Converts a non-negative integer to a fixed-width little-endian byte array as required by BCS.
     *
     * <p>BCS encodes all integers little-endian, so both the tagged ({@link #serialize}) and the
     * entry-function ({@link #serializeForEntryFunction}) forms of u128/u256 use this layout.</p>
     *
     * @param value the unsigned value to encode
     * @param width the target width in bytes (16 for u128, 32 for u256)
     * @return the little-endian encoding of {@code value}
     * @throws IllegalArgumentException if {@code value} is negative or does not fit in {@code width} bytes
     */
    static byte[] toLittleEndianBytes(BigInteger value, int width) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Unsigned integer cannot be negative");
        }
        if (value.bitLength() > width * 8) {
            throw new IllegalArgumentException("Value too large for u" + (width * 8));
        }
        byte[] be = value.toByteArray(); // big-endian, may include a leading sign byte
        byte[] le = new byte[width];
        for (int i = 0; i < width && i < be.length; i++) {
            le[i] = be[be.length - 1 - i];
        }
        return le;
    }

    /**
     * U8 argument
     */
    public static class U8 extends TransactionArgument {
        private final byte value;

        public U8(byte value) {
            this.value = value;
        }

        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 0); // U8 tag
            serializer.serializeU8(value);
        }
        
        @Override
        public byte[] serializeForEntryFunction() throws IOException {
            Serializer serializer = new Serializer();
            serializer.serializeU8(value);
            return serializer.toByteArray();
        }

        public byte getValue() {
            return value;
        }
    }

    /**
     * U64 argument
     */
    public static class U64 extends TransactionArgument {
        private final long value;

        public U64(long value) {
            this.value = value;
        }

        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 1); // U64 tag
            serializer.serializeU64(value);
        }

        /**
         * Serialize for entry function (without type tag, just the raw bytes)
         */
        public byte[] serializeForEntryFunction() throws IOException {
            Serializer serializer = new Serializer();
            serializer.serializeU64(value);
            return serializer.toByteArray();
        }

        public long getValue() {
            return value;
        }
    }

    /**
     * U128 argument
     */
    public static class U128 extends TransactionArgument {
        private final BigInteger value;

        public U128(BigInteger value) {
            this.value = value;
        }

        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 2); // U128 tag
            // BCS encodes integers little-endian, identical to serializeForEntryFunction().
            serializer.serializeU128(toLittleEndianBytes(value, 16));
        }
        
        @Override
        public byte[] serializeForEntryFunction() throws IOException {
            Serializer serializer = new Serializer();
            serializer.serializeU128(toLittleEndianBytes(value, 16));
            return serializer.toByteArray();
        }

        public BigInteger getValue() {
            return value;
        }
    }

    /**
     * Account address argument
     */
    public static class AccountAddress extends TransactionArgument {
        private final com.aptoslabs.japtos.core.AccountAddress value;

        public AccountAddress(com.aptoslabs.japtos.core.AccountAddress value) {
            this.value = value;
        }

        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 3); // AccountAddress tag
            serializer.serializeAccountAddress(value);
        }

        /**
         * Serialize for entry function (without type tag, just the raw bytes)
         */
        public byte[] serializeForEntryFunction() throws IOException {
            Serializer serializer = new Serializer();
            serializer.serializeAccountAddress(value);
            return serializer.toByteArray();
        }

        public com.aptoslabs.japtos.core.AccountAddress getValue() {
            return value;
        }
    }

    /**
     * U8Vector argument
     */
    public static class U8Vector extends TransactionArgument {
        private final byte[] value;

        public U8Vector(byte[] value) {
            this.value = value;
        }

        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 4); // U8Vector tag
            serializer.serializeBytes(value);
        }
        
        @Override
        public byte[] serializeForEntryFunction() throws IOException {
            Serializer serializer = new Serializer();
            serializer.serializeBytes(value);
            return serializer.toByteArray();
        }

        public byte[] getValue() {
            return value;
        }
    }

    /**
     * U64Vector argument for vector<u64> type
     * Serializes as a uleb128 length followed by u64 values
     */
    public static class U64Vector extends TransactionArgument {
        private final List<Long> values;

        public U64Vector(List<Long> values) {
            this.values = values;
        }

        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 9); // U64Vector tag
            // Serialize vector length as ULEB128
            serializer.serializeU32AsUleb128(values.size());
            // Serialize each u64 value
            for (Long value : values) {
                serializer.serializeU64(value);
            }
        }
        
        @Override
        public byte[] serializeForEntryFunction() throws IOException {
            Serializer serializer = new Serializer();
            // Serialize vector length as ULEB128
            serializer.serializeU32AsUleb128(values.size());
            // Serialize each u64 value
            for (Long value : values) {
                serializer.serializeU64(value);
            }
            return serializer.toByteArray();
        }

        public List<Long> getValue() {
            return values;
        }
    }

    /**
     * Bool argument
     */
    public static class Bool extends TransactionArgument {
        private final boolean value;

        public Bool(boolean value) {
            this.value = value;
        }

        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 5); // Bool tag
            serializer.serializeBool(value);
        }
        
        @Override
        public byte[] serializeForEntryFunction() throws IOException {
            Serializer serializer = new Serializer();
            serializer.serializeBool(value);
            return serializer.toByteArray();
        }

        public boolean getValue() {
            return value;
        }
    }

    /**
     * U16 argument
     */
    public static class U16 extends TransactionArgument {
        private final short value;

        public U16(short value) {
            this.value = value;
        }

        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 6); // U16 tag
            serializer.serializeU16(value);
        }
        
        @Override
        public byte[] serializeForEntryFunction() throws IOException {
            Serializer serializer = new Serializer();
            serializer.serializeU16(value);
            return serializer.toByteArray();
        }

        public short getValue() {
            return value;
        }
    }

    /**
     * U32 argument
     */
    public static class U32 extends TransactionArgument {
        private final int value;

        public U32(int value) {
            this.value = value;
        }

        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 7); // U32 tag
            serializer.serializeU32(value);
        }
        
        @Override
        public byte[] serializeForEntryFunction() throws IOException {
            Serializer serializer = new Serializer();
            serializer.serializeU32(value);
            return serializer.toByteArray();
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * U256 argument
     */
    public static class U256 extends TransactionArgument {
        private final BigInteger value;

        public U256(BigInteger value) {
            this.value = value;
        }

        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 8); // U256 tag
            // BCS encodes integers little-endian, identical to serializeForEntryFunction().
            serializer.serializeU256(toLittleEndianBytes(value, 32));
        }
        
        @Override
        public byte[] serializeForEntryFunction() throws IOException {
            Serializer serializer = new Serializer();
            serializer.serializeU256(toLittleEndianBytes(value, 32));
            return serializer.toByteArray();
        }

        public BigInteger getValue() {
            return value;
        }
    }

    /**
     * String argument for Move String type
     * Move String serializes as a uleb128 length followed by the bytes
     */
    public static class String extends TransactionArgument {
        private final byte[] value;

        public String(byte[] value) {
            this.value = value;
        }

        public String(java.lang.String value) {
            this.value = value.getBytes();
        }

        @Override
        public void serialize(Serializer serializer) throws IOException {
            // For entry functions, Move String serializes exactly like vector<u8>
            // Just serialize the bytes directly with uleb128 length prefix
            // This matches the correct BCS format for Move String in entry functions
            serializer.serializeBytes(value);
        }
        
        @Override
        public byte[] serializeForEntryFunction() throws IOException {
            // Move String serializes as length-prefixed bytes
            Serializer serializer = new Serializer();
            serializer.serializeBytes(value);
            return serializer.toByteArray();
        }

        public byte[] getValue() {
            return value;
        }

        public java.lang.String getStringValue() {
            return new java.lang.String(value);
        }
    }
}
