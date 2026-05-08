package com.esprit.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompreFaceConfigTest {

    @AfterEach
    void clearOverrides() {
        System.clearProperty("FACE_RECOGNITION_PROVIDER");
        System.clearProperty("COMPREFACE_BASE_URL");
        System.clearProperty("COMPREFACE_API_KEY");
        System.clearProperty("COMPREFACE_SIMILARITY_THRESHOLD");
    }

    @Test
    void shouldUseLocalDefaultsWhenNoOverrideExists() {
        assertEquals("compreface", CompreFaceConfig.provider());
        assertEquals("http://localhost:8000", CompreFaceConfig.baseUrl());
        assertEquals("810092e7-85de-4f3f-97b2-099709ffa5a3", CompreFaceConfig.apiKey());
        assertEquals(0.75, CompreFaceConfig.similarityThreshold());
        assertTrue(CompreFaceConfig.isConfigured());
    }

    @Test
    void shouldReadSystemPropertyOverrides() {
        System.setProperty("COMPREFACE_BASE_URL", "http://127.0.0.1:9000/");
        System.setProperty("COMPREFACE_API_KEY", "my-test-key");
        System.setProperty("COMPREFACE_SIMILARITY_THRESHOLD", "0.85");

        assertEquals("http://127.0.0.1:9000", CompreFaceConfig.baseUrl());
        assertEquals("my-test-key", CompreFaceConfig.apiKey());
        assertEquals(0.85, CompreFaceConfig.similarityThreshold());
    }
}


