package com.aptoslabs.japtos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Smoke test for the {@link App} demonstration entry point.
 *
 * <p>The demo exercises account generation, signing, hex utilities and address handling.
 * Running it end-to-end guards against regressions in the public "getting started" surface.</p>
 */
class AppTest {

    @Test
    @DisplayName("App.main runs the demo without throwing")
    void mainRunsCleanly() {
        assertDoesNotThrow(() -> App.main(new String[]{}));
    }
}
