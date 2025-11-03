package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

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
            // Convert BigInteger to byte array for U128 serialization
            byte[] bytes = value.toByteArray();
            if (bytes.length > 16) {
                throw new IllegalArgumentException("U128 value too large");
            }
            // Pad to 16 bytes if necessary
            byte[] padded = new byte[16];
            System.arraycopy(bytes, 0, padded, 16 - bytes.length, bytes.length);
            serializer.serializeU128(padded);
        }
        
        @Override
        public byte[] serializeForEntryFunction() throws IOException {
            Serializer serializer = new Serializer();
            byte[] bytes = value.toByteArray();
            byte[] padded = new byte[16];
            if (bytes[0] < 0) {
                // Handle negative sign extension for positive BigIntegers
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
            // Convert BigInteger to byte array for U256 serialization
            byte[] bytes = value.toByteArray();
            if (bytes.length > 32) {
                throw new IllegalArgumentException("U256 value too large");
            }
            // Pad to 32 bytes if necessary
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 32 - bytes.length, bytes.length);
            serializer.serializeU256(padded);
        }
        
        @Override
        public byte[] serializeForEntryFunction() throws IOException {
            Serializer serializer = new Serializer();
            byte[] bytes = value.toByteArray();
            byte[] padded = new byte[32];
            if (bytes[0] < 0) {
                // Handle negative sign extension for positive BigIntegers
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
