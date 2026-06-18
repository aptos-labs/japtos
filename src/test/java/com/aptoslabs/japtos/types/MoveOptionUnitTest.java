package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.core.AccountAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MoveOption}, covering Some/None semantics, conversions,
 * the factory helpers, and the vector-style entry-function serialization for every type.
 */
class MoveOptionUnitTest {

    @Test
    @DisplayName("of() with a value is Some; empty()/null is None")
    void someAndNone() {
        MoveOption<TransactionArgument.U64> some = MoveOption.of(new TransactionArgument.U64(7));
        assertTrue(some.isSome());
        assertFalse(some.isNone());
        assertEquals(7L, some.unwrap().getValue());

        MoveOption<TransactionArgument.U64> none = MoveOption.empty();
        assertTrue(none.isNone());
        assertFalse(none.isSome());

        MoveOption<TransactionArgument.U64> fromNull = new MoveOption<>(null);
        assertTrue(fromNull.isNone());
    }

    @Test
    @DisplayName("unwrap throws on None, unwrapOr returns the default")
    void unwrapBehaviour() {
        MoveOption<TransactionArgument.U64> none = MoveOption.empty();
        assertThrows(IllegalStateException.class, none::unwrap);

        TransactionArgument.U64 def = new TransactionArgument.U64(99);
        assertSame(def, none.unwrapOr(def));

        MoveOption<TransactionArgument.U64> some = MoveOption.of(new TransactionArgument.U64(1));
        assertEquals(1L, some.unwrapOr(def).getValue());
    }

    @Test
    @DisplayName("toOptional, map and fromOptional behave like Optional")
    void conversionsAndMap() {
        MoveOption<TransactionArgument.U64> some = MoveOption.of(new TransactionArgument.U64(5));
        assertEquals(Optional.of(5L), some.toOptional().map(TransactionArgument.U64::getValue));
        assertTrue(MoveOption.<TransactionArgument.U64>empty().toOptional().isEmpty());

        MoveOption<TransactionArgument.U64> mapped =
                some.map(v -> new TransactionArgument.U64(v.getValue() * 2));
        assertEquals(10L, mapped.unwrap().getValue());
        assertTrue(MoveOption.<TransactionArgument.U64>empty()
                .map(v -> new TransactionArgument.U64(0)).isNone());

        MoveOption<TransactionArgument.U64> fromOpt =
                MoveOption.fromOptional(Optional.of(3L), TransactionArgument.U64::new);
        assertEquals(3L, fromOpt.unwrap().getValue());
        assertTrue(MoveOption.fromOptional(Optional.<Long>empty(), TransactionArgument.U64::new).isNone());
    }

    @Test
    @DisplayName("serialize() is unsupported; serializeForEntryFunction encodes a 0/1 length vector")
    void serializationContract() throws IOException {
        MoveOption<TransactionArgument.U64> none = MoveOption.empty();
        assertThrows(UnsupportedOperationException.class,
                () -> none.serialize(new com.aptoslabs.japtos.bcs.Serializer()));

        // None -> single 0x00 length byte
        assertArrayEquals(new byte[]{0x00}, none.serializeForEntryFunction());

        // Some(U64=1) -> length 1 + 8 little-endian bytes
        MoveOption<TransactionArgument.U64> some = MoveOption.of(new TransactionArgument.U64(1));
        byte[] bytes = some.serializeForEntryFunction();
        assertEquals(9, bytes.length);
        assertEquals(0x01, bytes[0]);
        assertEquals(0x01, bytes[1]);
    }

    @Test
    @DisplayName("Every typed factory produces a serializable Some and toString reflects state")
    void typedFactories() throws IOException {
        assertEquals(2, MoveOption.u8((byte) 1).serializeForEntryFunction().length);   // len + 1
        assertEquals(3, MoveOption.u16((short) 1).serializeForEntryFunction().length); // len + 2
        assertEquals(5, MoveOption.u32(1).serializeForEntryFunction().length);         // len + 4
        assertEquals(9, MoveOption.u64(1L).serializeForEntryFunction().length);        // len + 8
        assertEquals(17, MoveOption.u128(BigInteger.ONE).serializeForEntryFunction().length); // len + 16
        assertEquals(33, MoveOption.u256(BigInteger.ONE).serializeForEntryFunction().length); // len + 32
        assertEquals(2, MoveOption.bool(true).serializeForEntryFunction().length);     // len + 1
        assertEquals(33, MoveOption.address(AccountAddress.zero()).serializeForEntryFunction().length);
        // String "ab" -> 1 (option len) + 1 (string len) + 2 bytes = 4
        assertEquals(4, MoveOption.string("ab").serializeForEntryFunction().length);
        // u8Vector {1,2} -> 1 (option len) + 1 (vec len) + 2 = 4
        assertEquals(4, MoveOption.u8Vector(new byte[]{1, 2}).serializeForEntryFunction().length);

        // Null inputs collapse to None.
        assertTrue(MoveOption.u64(null).isNone());
        assertTrue(MoveOption.string(null).isNone());
        assertTrue(MoveOption.address(null).isNone());

        assertEquals("None", MoveOption.empty().toString());
        assertTrue(MoveOption.u64(1L).toString().startsWith("Some("));
    }
}
