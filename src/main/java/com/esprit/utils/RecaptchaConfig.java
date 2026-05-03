package com.esprit.utils;

public final class RecaptchaConfig {
    private static final String SITE_KEY = "6LeZxcssAAAAANfhI0SWytATwDHIW8i7HuF8cLBF";
    private static final String SECRET = "6LeZxcssAAAAAA30qrr9t9t1xTxizN_--JgaG8Hr";

    private RecaptchaConfig() {
        // Utility class
    }

    public static String siteKey() {
        return SITE_KEY;
    }

    public static String secret() {
        return SECRET;
    }

    public static boolean isConfigured() {
        return !siteKey().isBlank() && !secret().isBlank();
    }
}




