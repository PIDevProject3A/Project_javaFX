package com.esprit.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecaptchaConfigTest {

    @Test
    void shouldExposeExactConfiguredKeys() {
        assertEquals("6LeZxcssAAAAANfhI0SWytATwDHIW8i7HuF8cLBF", RecaptchaConfig.siteKey());
        assertEquals("6LeZxcssAAAAAA30qrr9t9t1xTxizN_--JgaG8Hr", RecaptchaConfig.secret());
    }

    @Test
    void shouldAlwaysBeConfiguredWithFixedKeys() {
        assertFalse(RecaptchaConfig.siteKey().isBlank());
        assertFalse(RecaptchaConfig.secret().isBlank());
        assertTrue(RecaptchaConfig.isConfigured());
    }
}





