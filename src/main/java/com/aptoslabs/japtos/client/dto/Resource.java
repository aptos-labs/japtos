package com.aptoslabs.japtos.client.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Represents an account resource from the Aptos API
 */
public class Resource {
    @SerializedName("type")
    private final String type;
    
    @SerializedName("data")
    private final Object data;
    
    public Resource(String type, Object data) {
        this.type = type;
        this.data = data;
    }
    
    public String getType() {
        return type;
    }
    
    public Object getData() {
        return data;
    }
} 