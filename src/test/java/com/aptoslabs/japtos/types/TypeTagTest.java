package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AccountAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TypeTag} discriminators and nested serialization.
 */
class TypeTagTest {

    private byte[] ser(TypeTag tag) throws IOException {
        Serializer s = new Serializer();
        tag.serialize(s);
        return s.toByteArray();
    }

    @Test
    @DisplayName("Primitive type tags use their documented discriminators")
    void primitiveDiscriminators() throws IOException {
        assertArrayEquals(new byte[]{0}, ser(new TypeTag.Bool()));
        assertArrayEquals(new byte[]{1}, ser(new TypeTag.U8()));
        assertArrayEquals(new byte[]{2}, ser(new TypeTag.U64()));
        assertArrayEquals(new byte[]{3}, ser(new TypeTag.U128()));
        assertArrayEquals(new byte[]{4}, ser(new TypeTag.Address()));
        assertArrayEquals(new byte[]{5}, ser(new TypeTag.Signer()));
        assertArrayEquals(new byte[]{8}, ser(new TypeTag.U16()));
        assertArrayEquals(new byte[]{9}, ser(new TypeTag.U32()));
        assertArrayEquals(new byte[]{10}, ser(new TypeTag.U256()));
    }

    @Test
    @DisplayName("Vector tag (6) is followed by its element type tag")
    void vectorTag() throws IOException {
        TypeTag.Vector vec = new TypeTag.Vector(new TypeTag.U8());
        assertTrue(vec.getElementType() instanceof TypeTag.U8);
        assertArrayEquals(new byte[]{6, 1}, ser(vec));
    }

    @Test
    @DisplayName("Struct tag (7) is followed by the full struct tag encoding")
    void structTag() throws IOException {
        StructTag struct = new StructTag(
                AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"),
                new Identifier("coin"),
                new Identifier("Coin"),
                List.of());
        TypeTag.Struct tag = new TypeTag.Struct(struct);
        assertSame(struct, tag.getStructTag());

        byte[] bytes = ser(tag);
        assertEquals(7, bytes[0]);
        // 1 (tag) + 32 (address) + (1+4 "coin") + (1+4 "Coin") + 1 (type args count) = 44
        assertEquals(44, bytes.length);
    }
}
