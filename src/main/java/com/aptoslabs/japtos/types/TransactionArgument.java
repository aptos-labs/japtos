package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;
import java.math.BigInteger;

/**
 * Represents a transaction argument.
 */
public abstract class TransactionArgument implements Serializable {
    public abstract void serialize(Serializer serializer) throws IOException;

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

        public byte[] getValue() {
            return value;
        }
    }

    /**
     * U64 vector argument - serialized as a BCS vector<u64> for entry function usage.
     */
    public static class U64Vector extends TransactionArgument {
        private final java.util.List<Long> values;

        public U64Vector(java.util.List<Long> values) {
            this.values = values == null ? java.util.Collections.emptyList() : values;
        }

        @Override
        public void serialize(Serializer serializer) throws IOException {
            // For entry function, we want the raw vector<u64> bytes
            serializer.serializeU32AsUleb128(values.size());
            for (Long v : values) {
                serializer.serializeU64(v == null ? 0L : v);
            }
        }

        public java.util.List<Long> getValues() { return values; }
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

        public byte[] getValue() {
            return value;
        }

        public java.lang.String getStringValue() {
            return new java.lang.String(value);
        }
    }
}
