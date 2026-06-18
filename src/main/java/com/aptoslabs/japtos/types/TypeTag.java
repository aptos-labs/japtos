package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;

/**
 * Represents a type tag in the Aptos Move type system.
 *
 * <p>TypeTag provides a runtime representation of Move types, enabling type
 * checking, serialization, and generic programming within the Aptos ecosystem.
 * It corresponds directly to Move's type system and is used extensively in
 * transaction payloads, function calls, and resource operations.</p>
 *
 * <p>The type system includes:</p>
 * <ul>
 *   <li><strong>Primitive Types</strong> - bool, u8, u16, u32, u64, u128, u256</li>
 *   <li><strong>Special Types</strong> - address, signer</li>
 *   <li><strong>Container Types</strong> - vector&lt;T&gt;</li>
 *   <li><strong>User Types</strong> - struct types defined in Move modules</li>
 * </ul>
 *
 * <p>Each TypeTag variant encodes a specific type with a unique identifier
 * used during BCS serialization. The serialization format includes both
 * the type discriminator and any nested type information.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Primitive types
 * TypeTag boolType = new TypeTag.Bool();
 * TypeTag u64Type = new TypeTag.U64();
 * TypeTag addressType = new TypeTag.Address();
 *
 * // Container types
 * TypeTag vectorU8 = new TypeTag.Vector(new TypeTag.U8());
 *
 * // Struct types
 * StructTag coinStruct = new StructTag(
 *     AccountAddress.fromHex("0x1"),
 *     new Identifier("coin"),
 *     new Identifier("Coin"),
 *     Arrays.asList(new TypeTag.Address())
 * );
 * TypeTag coinType = new TypeTag.Struct(coinStruct);
 * }</pre>
 *
 * @see StructTag
 * @see TransactionPayload
 * @see EntryFunctionPayload
 * @see Serializable
 * @since 1.0.0
 */
public abstract class TypeTag implements Serializable {
    public abstract void serialize(Serializer serializer) throws IOException;

    /**
     * Represents the Move 'bool' primitive type.
     *
     * <p>Serialized with type discriminator 0.</p>
     */
    public static class Bool extends TypeTag {
        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 0); // Bool tag
        }
    }

    /**
     * Represents the Move 'u8' primitive type (8-bit unsigned integer).
     *
     * <p>Serialized with type discriminator 1.</p>
     */
    public static class U8 extends TypeTag {
        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 1); // U8 tag
        }
    }

    /**
     * Represents the Move 'u64' primitive type (64-bit unsigned integer).
     *
     * <p>Serialized with type discriminator 2.</p>
     */
    public static class U64 extends TypeTag {
        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 2); // U64 tag
        }
    }

    /**
     * Represents the Move 'u128' primitive type (128-bit unsigned integer).
     *
     * <p>Serialized with type discriminator 3.</p>
     */
    public static class U128 extends TypeTag {
        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 3); // U128 tag
        }
    }

    /**
     * Represents the Move 'address' primitive type.
     *
     * <p>Used for account addresses and other 32-byte identifiers.
     * Serialized with type discriminator 4.</p>
     */
    public static class Address extends TypeTag {
        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 4); // Address tag
        }
    }

    /**
     * Represents the Move 'signer' primitive type.
     *
     * <p>Used for transaction authorization and access control.
     * Serialized with type discriminator 5.</p>
     */
    public static class Signer extends TypeTag {
        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 5); // Signer tag
        }
    }

    /**
     * Represents the Move 'vector&lt;T&gt;' container type.
     *
     * <p>A generic container type that holds a sequence of elements
     * of the specified element type. Serialized with type discriminator 6
     * followed by the element type.</p>
     */
    public static class Vector extends TypeTag {
        private final TypeTag elementType;

        /**
         * Creates a Vector type tag with the specified element type.
         *
         * @param elementType the type of elements contained in this vector
         */
        public Vector(TypeTag elementType) {
            this.elementType = elementType;
        }

        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 6); // Vector tag
            elementType.serialize(serializer);
        }

        /**
         * Returns the element type of this vector.
         *
         * @return the type tag for vector elements
         */
        public TypeTag getElementType() {
            return elementType;
        }
    }

    /**
     * Represents a user-defined struct type from a Move module.
     *
     * <p>Struct types are defined in Move modules and can have generic
     * type parameters. Serialized with type discriminator 7 followed
     * by the complete struct tag information.</p>
     *
     * @see StructTag
     */
    public static class Struct extends TypeTag {
        private final StructTag structTag;

        /**
         * Creates a Struct type tag with the specified struct tag.
         *
         * @param structTag the struct tag containing module, name, and type arguments
         */
        public Struct(StructTag structTag) {
            this.structTag = structTag;
        }

        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 7); // Struct tag
            structTag.serialize(serializer);
        }

        /**
         * Returns the struct tag for this struct type.
         *
         * @return the struct tag containing module information and type arguments
         */
        public StructTag getStructTag() {
            return structTag;
        }
    }

    /**
     * U16 type tag
     */
    public static class U16 extends TypeTag {
        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 8); // U16 tag
        }
    }

    /**
     * U32 type tag
     */
    public static class U32 extends TypeTag {
        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 9); // U32 tag
        }
    }

    /**
     * U256 type tag
     */
    public static class U256 extends TypeTag {
        @Override
        public void serialize(Serializer serializer) throws IOException {
            serializer.serializeU8((byte) 10); // U256 tag
        }
    }
}
