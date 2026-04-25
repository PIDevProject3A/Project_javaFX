package com.esprit.Services;

import com.esprit.entities.Event;
import com.esprit.entities.Registration;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.io.InputStream;
import java.util.Properties;

public class StripePaymentService {


    private static final String DEFAULT_CURRENCY = "eur";
    private static final String SUCCESS_URL = "https://example.com/payment-success";
    private static final String CANCEL_URL = "https://example.com/payment-cancel";

    // ⚡ taux approximatif (à améliorer plus tard si tu veux)
    private static final double TND_TO_EUR_RATE = 3.3;

    private final Properties localConfig = loadLocalStripeConfig();

    public StripePaymentService() {
        String key = resolveStripeSecretKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "Clé Stripe introuvable. Configurez : ENV / VM option / stripe.properties");
        }
        Stripe.apiKey = key;
    }

    public CheckoutSessionInfo createCheckoutSession(Registration registration, Event event) throws StripeException {

        String currency = resolveCurrency();
        if (currency == null || currency.isBlank()) {
            currency = DEFAULT_CURRENCY;
        }

    
        double amountTnd = registration.getAmount();

        double amountEur = convertTndToEur(amountTnd);

        long amountMinor = toMinorAmount(amountEur, currency);
        

        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName("Inscription : " + (event.getName() != null ? event.getName() : "Événement"))
                        .build();

        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(currency.toLowerCase())
                        .setUnitAmount(amountMinor)
                        .setProductData(productData)
                        .build();

        SessionCreateParams.LineItem lineItem =
                SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(priceData)
                        .build();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(SUCCESS_URL)
                .setCancelUrl(CANCEL_URL)
                .addLineItem(lineItem)
                .putMetadata("registration_id", String.valueOf(registration.getId()))
                .putMetadata("event_id", String.valueOf(registration.getEventId()))
                .putMetadata("user_id", String.valueOf(registration.getUserId()))
                .build();

        Session session = Session.create(params);
        return new CheckoutSessionInfo(session.getId(), session.getUrl());
    }

    public boolean isCheckoutSessionPaid(String sessionId) throws StripeException {
        Session session = Session.retrieve(sessionId);
        return "paid".equalsIgnoreCase(session.getPaymentStatus());
    }

    /**
     * Stripe peut mettre un court délai avant de refléter payment_status=paid.
     * On poll quelques secondes pour fiabiliser la confirmation côté desktop.
     */
    public boolean waitForCheckoutSessionPaid(String sessionId, int maxAttempts, long sleepMillis)
            throws StripeException, InterruptedException {
        for (int i = 0; i < Math.max(1, maxAttempts); i++) {
            if (isCheckoutSessionPaid(sessionId)) {
                return true;
            }
            Thread.sleep(Math.max(200L, sleepMillis));
        }
        return false;
    }


    private double convertTndToEur(double amountTnd) {
        return amountTnd / TND_TO_EUR_RATE;
    }


    private static long toMinorAmount(double amount, String currency) {
        String c = currency == null ? "" : currency.toLowerCase();

        // Stripe : EUR = 100, TND = 1000 (mais TND interdit ici)
        int factor = ("bhd".equals(c) || "jod".equals(c) || "kwd".equals(c)) ? 1000 : 100;

        return Math.round(amount * factor);
    }

    private String resolveStripeSecretKey() {
        String key = System.getenv("STRIPE_SECRET_KEY");
        if (key != null && !key.isBlank()) return key.trim();

        key = System.getProperty("stripe.secret.key");
        if (key != null && !key.isBlank()) return key.trim();

        key = localConfig.getProperty("stripe.secret.key");
        if (key != null && !key.isBlank()) return key.trim();

        return null;
    }

    private String resolveCurrency() {
        String currency = System.getenv("STRIPE_CURRENCY");
        if (currency != null && !currency.isBlank()) return currency.trim();

        currency = System.getProperty("stripe.currency");
        if (currency != null && !currency.isBlank()) return currency.trim();

        currency = localConfig.getProperty("stripe.currency");
        if (currency != null && !currency.isBlank()) return currency.trim();

        return DEFAULT_CURRENCY;
    }

    private static Properties loadLocalStripeConfig() {
        Properties props = new Properties();
        try (InputStream in = StripePaymentService.class.getClassLoader()
                .getResourceAsStream("stripe.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception ignored) {}
        return props;
    }

    public record CheckoutSessionInfo(String sessionId, String checkoutUrl) {}

}
