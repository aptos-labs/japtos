package com.aptoslabs.japtos.types;

import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AccountAddress;

import java.io.IOException;
import java.util.List;

/**
 * Represents a struct tag.
 */
public class StructTag implements Serializable {
    private final AccountAddress address;
    private final Identifier module;
    private final Identifier name;
    private final List<TypeTag> typeArguments;

    public StructTag(AccountAddress address, Identifier module, Identifier name, List<TypeTag> typeArguments) {
        this.address = address;
        this.module = module;
        this.name = name;
        this.typeArguments = typeArguments;
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        serializer.serializeAccountAddress(address);
        module.serialize(serializer);
        name.serialize(serializer);

        serializer.serializeU32AsUleb128(typeArguments.size());
        for (TypeTag typeTag : typeArguments) {
            typeTag.serialize(serializer);
        }
    }

    public AccountAddress getAddress() {
        return address;
    }

    public Identifier getModule() {
        return module;
    }

    public Identifier getName() {
        return name;
    }

    public List<TypeTag> getTypeArguments() {
        return typeArguments;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(address.toString()).append("::").append(module.toString()).append("::").append(name.toString());

        if (!typeArguments.isEmpty()) {
            sb.append("<");
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(typeArguments.get(i).toString());
            }
            sb.append(">");
        }

        return sb.toString();
    }
} 