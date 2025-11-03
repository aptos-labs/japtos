package com.aptoslabs.japtos;

import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.types.MoveOption;
import com.aptoslabs.japtos.types.TransactionArgument;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MoveOption functionality
 */
public class MoveOptionTest {
    
    @Test
    public void testMoveOptionMethods() {
        MoveOption<TransactionArgument.U64> some = MoveOption.u64(123L);
        assertTrue(some.isSome());
        assertFalse(some.isNone());
        
        MoveOption<TransactionArgument.U64> none = MoveOption.u64(null);
        assertFalse(none.isSome());
        assertTrue(none.isNone());
        
        assertEquals(123L, some.unwrap().getValue());
        assertThrows(IllegalStateException.class, () -> none.unwrap());
        
        assertEquals(123L, some.unwrapOr(new TransactionArgument.U64(456L)).getValue());
        assertEquals(456L, none.unwrapOr(new TransactionArgument.U64(456L)).getValue());
        
        assertTrue(some.toOptional().isPresent());
        assertEquals(123L, some.toOptional().get().getValue());
        assertFalse(none.toOptional().isPresent());
        
        MoveOption<TransactionArgument.U128> mapped = some.map(
            u64 -> new TransactionArgument.U128(BigInteger.valueOf(u64.getValue() * 2))
        );
        assertTrue(mapped.isSome());
        assertEquals(BigInteger.valueOf(246L), mapped.unwrap().getValue());
        
        MoveOption<TransactionArgument.U128> mappedNone = none.map(
            u64 -> new TransactionArgument.U128(BigInteger.valueOf(u64.getValue() * 2))
        );
        assertTrue(mappedNone.isNone());
        
        System.out.println("MoveOption method tests passed!");
    }
    
    @Test
    public void testMoveOptionSerialization() throws Exception {
        // Some(u64)
        MoveOption<TransactionArgument.U64> someU64 = MoveOption.u64(12345L);
        byte[] someU64Bytes = someU64.serializeForEntryFunction();
        // Should be: 01 (length=1) + 08 bytes for u64
        assertEquals(9, someU64Bytes.length);
        assertEquals(0x01, someU64Bytes[0]); // Length = 1
        
        // None for u64
        MoveOption<TransactionArgument.U64> noneU64 = MoveOption.u64(null);
        byte[] noneU64Bytes = noneU64.serializeForEntryFunction();
        // Should be: 00 (length=0)
        assertEquals(1, noneU64Bytes.length);
        assertEquals(0x00, noneU64Bytes[0]); // Length = 0
        
        // Some(String)
        MoveOption<TransactionArgument.String> someString = MoveOption.string("hello");
        byte[] someStringBytes = someString.serializeForEntryFunction();
        // Should be: 01 (length=1) + 05 (string length) + "hello"
        assertTrue(someStringBytes.length > 1);
        assertEquals(0x01, someStringBytes[0]); // Length = 1
        assertEquals(0x05, someStringBytes[1]); // String length = 5
        
        // None for String
        MoveOption<TransactionArgument.String> noneString = MoveOption.string(null);
        byte[] noneStringBytes = noneString.serializeForEntryFunction();
        assertEquals(1, noneStringBytes.length);
        assertEquals(0x00, noneStringBytes[0]); // Length = 0
        
        // Some(Bool)
        MoveOption<TransactionArgument.Bool> someBool = MoveOption.bool(true);
        byte[] someBoolBytes = someBool.serializeForEntryFunction();
        assertEquals(2, someBoolBytes.length);
        assertEquals(0x01, someBoolBytes[0]); // Length = 1
        assertEquals(0x01, someBoolBytes[1]); // true = 1
        
        // Some(AccountAddress)
        AccountAddress addr = AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001");
        MoveOption<TransactionArgument.AccountAddress> someAddr = MoveOption.address(addr);
        byte[] someAddrBytes = someAddr.serializeForEntryFunction();
        assertEquals(33, someAddrBytes.length); // 1 (length) + 32 (address)
        assertEquals(0x01, someAddrBytes[0]); // Length = 1
        
        System.out.println("MoveOption serialization tests passed!");
    }
    
    @Test
    public void testMoveOptionFactoryMethods() {
        assertNotNull(MoveOption.u8((byte) 1));
        assertNotNull(MoveOption.u8(null));
        
        assertNotNull(MoveOption.u16((short) 1));
        assertNotNull(MoveOption.u16(null));
        
        assertNotNull(MoveOption.u32(1));
        assertNotNull(MoveOption.u32(null));
        
        assertNotNull(MoveOption.u64(1L));
        assertNotNull(MoveOption.u64(null));
        
        assertNotNull(MoveOption.u128(BigInteger.ONE));
        assertNotNull(MoveOption.u128(null));
        
        assertNotNull(MoveOption.u256(BigInteger.ONE));
        assertNotNull(MoveOption.u256(null));
        
        assertNotNull(MoveOption.bool(true));
        assertNotNull(MoveOption.bool(null));
        
        assertNotNull(MoveOption.string("test"));
        assertNotNull(MoveOption.string(null));
        
        assertNotNull(MoveOption.address(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001")));
        assertNotNull(MoveOption.address(null));
        
        assertNotNull(MoveOption.u8Vector(new byte[]{1, 2, 3}));
        assertNotNull(MoveOption.u8Vector(null));
        
        System.out.println("MoveOption factory method tests passed!");
    }
}
