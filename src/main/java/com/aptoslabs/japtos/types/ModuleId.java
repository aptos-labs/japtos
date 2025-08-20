package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AccountAddress;

import java.io.IOException;

/**
 * Represents a module identifier.

 */
public class ModuleId implements Serializable {
    private final AccountAddress address;
    private final Identifier name;
    
    public ModuleId(AccountAddress address, Identifier name) {
        this.address = address;
        this.name = name;
    }
    
    @Override
    public void serialize(Serializer serializer) throws IOException {
        serializer.serializeAccountAddress(address);
        name.serialize(serializer);
    }
    
    public AccountAddress getAddress() {
        return address;
    }
    
    public Identifier getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return address.toString() + "::" + name.toString();
    }
} 