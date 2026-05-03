package org.example.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EnvConfig {
    private static final Map<String, String> ENV_CACHE = new HashMap<String, String>();

    static {
        try {
            Path envFile = Paths.get(".env");
            if (Files.exists(envFile)) {
                List<String> lines = Files.readAllLines(envFile);
                for (String line : lines) {
                    if (line != null && !line.trim().isEmpty() && !line.startsWith("#")) {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            ENV_CACHE.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not read .env file - " + e.getMessage());
        }
    }

    private EnvConfig() {
    }

    public static String getRequired(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = ENV_CACHE.get(key);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return value.trim();
    }

    public static String getOptional(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = ENV_CACHE.get(key);
        }
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
