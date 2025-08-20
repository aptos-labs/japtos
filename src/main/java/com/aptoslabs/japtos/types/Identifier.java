package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;

/**
 * Represents an identifier.

 */
public class Identifier implements Serializable {
    private final String value;
    
    public Identifier(String value) {
        this.value = value;
    }
    
    @Override
    public void serialize(Serializer serializer) throws IOException {
        serializer.serializeString(value);
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Identifier that = (Identifier) obj;
        return value.equals(that.value);
    }
    
    @Override
    public int hashCode() {
        return value.hashCode();
    }
} 