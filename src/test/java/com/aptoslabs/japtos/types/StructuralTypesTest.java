package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AccountAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Identifier}, {@link ModuleId} and {@link StructTag}.
 */
class StructuralTypesTest {

    @Test
    @DisplayName("Identifier serializes as a Move String and exposes value/equality")
    void identifier() throws IOException {
        Identifier id = new Identifier("coin");
        assertEquals("coin", id.getValue());
        assertEquals("coin", id.toString());

        Serializer s = new Serializer();
        id.serialize(s);
        assertArrayEquals(new byte[]{0x04, 'c', 'o', 'i', 'n'}, s.toByteArray());

        assertEquals(new Identifier("coin"), id);
        assertEquals(new Identifier("coin").hashCode(), id.hashCode());
        assertNotEquals(new Identifier("other"), id);
        assertNotEquals(id, "coin");
        assertEquals(id, id);
    }

    @Test
    @DisplayName("ModuleId serializes address then name and renders address::name")
    void moduleId() throws IOException {
        AccountAddress addr = AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001");
        ModuleId module = new ModuleId(addr, new Identifier("coin"));
        assertEquals(addr, module.getAddress());
        assertEquals("coin", module.getName().getValue());
        assertEquals(addr + "::coin", module.toString());

        Serializer s = new Serializer();
        module.serialize(s);
        // 32 (address) + 1 len + 4 chars = 37
        assertEquals(37, s.toByteArray().length);
    }

    @Test
    @DisplayName("StructTag renders the canonical type string and serializes nested type args")
    void structTag() throws IOException {
        AccountAddress addr = AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001");
        StructTag noArgs = new StructTag(addr, new Identifier("coin"),
                new Identifier("Coin"), List.of());
        assertEquals(addr + "::coin::Coin", noArgs.toString());
        assertEquals(addr, noArgs.getAddress());
        assertEquals("coin", noArgs.getModule().getValue());
        assertEquals("Coin", noArgs.getName().getValue());
        assertTrue(noArgs.getTypeArguments().isEmpty());

        StructTag withArgs = new StructTag(addr, new Identifier("table"),
                new Identifier("Table"), List.of(new TypeTag.U64(), new TypeTag.Bool()));
        assertTrue(withArgs.toString().contains("<"));
        assertTrue(withArgs.toString().contains(", "));
        assertEquals(2, withArgs.getTypeArguments().size());

        Serializer s = new Serializer();
        withArgs.serialize(s);
        // address 32 + ("table"=6) + ("Table"=6) + count(1) + U64(1) + Bool(1) = 47
        assertEquals(47, s.toByteArray().length);
    }
}
