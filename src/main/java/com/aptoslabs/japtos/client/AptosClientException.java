package com.aptoslabs.japtos.client;

/**
 * Exception thrown by AptosClient operations
 */
public class AptosClientException extends Exception {
    
    public AptosClientException(String message) {
        super(message);
    }
    
    public AptosClientException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public AptosClientException(Throwable cause) {
        super(cause);
    }
} 