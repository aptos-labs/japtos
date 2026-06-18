package com.aptoslabs.japtos.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the standalone {@link AptosClientException} type.
 */
class AptosClientExceptionTest {

    @Test
    @DisplayName("Message-only constructor")
    void messageOnly() {
        AptosClientException ex = new AptosClientException("failed");
        assertEquals("failed", ex.getMessage());
        assertNull(ex.getCause());
        assertTrue(ex instanceof Exception);
    }

    @Test
    @DisplayName("Message + cause constructor")
    void messageAndCause() {
        Throwable cause = new IllegalStateException("root");
        AptosClientException ex = new AptosClientException("failed", cause);
        assertEquals("failed", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    @DisplayName("Cause-only constructor")
    void causeOnly() {
        Throwable cause = new IllegalStateException("root");
        AptosClientException ex = new AptosClientException(cause);
        assertSame(cause, ex.getCause());
    }
}
