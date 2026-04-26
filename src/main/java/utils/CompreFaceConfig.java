package utils;

public final class CompreFaceConfig {
    private static final String DEFAULT_PROVIDER = "compreface";
    private static final String DEFAULT_BASE_URL = "http://localhost:8000";
    private static final String DEFAULT_API_KEY = "810092e7-85de-4f3f-97b2-099709ffa5a3";
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.75;

    private CompreFaceConfig() {
        // Utility class
    }

    public static String provider() {
        return read("FACE_RECOGNITION_PROVIDER", DEFAULT_PROVIDER).toLowerCase();
    }

    public static String baseUrl() {
        String value = read("COMPREFACE_BASE_URL", DEFAULT_BASE_URL);
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    public static String apiKey() {
        return read("COMPREFACE_API_KEY", DEFAULT_API_KEY);
    }

    public static double similarityThreshold() {
        String raw = read("COMPREFACE_SIMILARITY_THRESHOLD", Double.toString(DEFAULT_SIMILARITY_THRESHOLD));
        try {
            double value = Double.parseDouble(raw);
            if (value < 0.0 || value > 1.0) {
                return DEFAULT_SIMILARITY_THRESHOLD;
            }
            return value;
        } catch (NumberFormatException ignored) {
            return DEFAULT_SIMILARITY_THRESHOLD;
        }
    }

    public static boolean isConfigured() {
        return "compreface".equals(provider())
                && !baseUrl().isBlank()
                && !apiKey().isBlank();
    }

    private static String read(String key, String defaultValue) {
        String sysValue = System.getProperty(key);
        if (sysValue != null && !sysValue.trim().isEmpty()) {
            return sysValue.trim();
        }

        String envValue = System.getenv(key);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }

        return defaultValue;
    }
}

