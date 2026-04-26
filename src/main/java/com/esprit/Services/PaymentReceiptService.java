package com.esprit.Services;

import com.esprit.entities.Event;
import com.esprit.entities.Registration;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PaymentReceiptService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Construit les données du reçu à partir d'une inscription et d'un événement.
     */
    public ReceiptData buildReceiptData(Registration r, Event e) {
        String eventName = (e != null && e.getName() != null && !e.getName().isBlank())
                ? e.getName()
                : (r.getEventName() != null && !r.getEventName().isBlank() ? r.getEventName() : "—");
        LocalDateTime eventDate = (e != null && e.getEventDate() != null) ? e.getEventDate() : null;
        String location = (e != null && e.getLocation() != null && !e.getLocation().isBlank())
                ? e.getLocation()
                : "—";
        String fullName = (r.getFullName() != null && !r.getFullName().isBlank()) ? r.getFullName() : "—";
        String email = (r.getEmail() != null && !r.getEmail().isBlank()) ? r.getEmail() : "—";
        String payment = (r.getPaymentMethod() != null && !r.getPaymentMethod().isBlank()) ? r.getPaymentMethod() : "—";
        LocalDateTime regDate = (r.getRegistrationDate() != null) ? r.getRegistrationDate() : r.getPaymentDate();
        String receiptCode = "BLD-REG-" + (r.getId() != 0 ? r.getId() : System.currentTimeMillis());
        String qrPayload = String.join("|",
                "receipt=" + receiptCode,
                "event=" + nullSafe(eventName),
                "date=" + (eventDate != null ? eventDate.format(DT) : "—"),
                "name=" + nullSafe(fullName),
                "email=" + nullSafe(email),
                "amount=" + String.format(Locale.US, "%.2f", r.getAmount()),
                "payment=" + payment);

        return new ReceiptData(
                r.getId(),
                receiptCode,
                eventName,
                eventDate,
                location,
                fullName,
                email,
                regDate,
                r.getAmount(),
                payment,
                (r.getStatus() != null ? r.getStatus() : "—"),
                qrPayload
        );
    }

    /**
     * Génère une image JavaFX du QR code à partir du payload.
     */
    public Image createQrFxImage(String payload, int size) throws Exception {
        byte[] png = createQrPng(payload, size);
        return new Image(new ByteArrayInputStream(png));
    }

    /**
     * Génère les bytes du PDF du reçu.
     */
    public byte[] generatePdfBytes(ReceiptData d) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 42, 36);
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.DARK_GRAY);
            Font normal = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.DARK_GRAY);
            Font strong = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, new BaseColor(27, 94, 32));

            Paragraph title = new Paragraph("Bledna - Reçu de paiement", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(14f);
            doc.add(title);

            doc.add(new Paragraph("Code reçu : " + d.receiptCode(), strong));
            doc.add(new Paragraph("Événement : " + d.eventName(), normal));
            doc.add(new Paragraph("Date événement : " + formatDate(d.eventDate()), normal));
            doc.add(new Paragraph("Lieu : " + d.location(), normal));
            doc.add(new Paragraph("Participant : " + d.participantName(), normal));
            doc.add(new Paragraph("Email : " + d.email(), normal));
            doc.add(new Paragraph("Date inscription : " + formatDate(d.registrationDate()), normal));
            doc.add(new Paragraph(String.format(Locale.FRANCE, "Montant payé : %.2f TND", d.amount()), strong));
            doc.add(new Paragraph("Méthode paiement : " + d.paymentMethod(), normal));
            doc.add(new Paragraph("Statut : " + d.status(), normal));
            doc.add(Chunk.NEWLINE);

            byte[] qrPng = createQrPng(d.qrPayload(), 260);
            com.itextpdf.text.Image qr = com.itextpdf.text.Image.getInstance(qrPng);
            qr.setAlignment(Element.ALIGN_CENTER);
            doc.add(qr);


            Paragraph note = new Paragraph("QR code de vérification du reçu", normal);
            note.setAlignment(Element.ALIGN_CENTER);
            note.setSpacingBefore(8f);
            doc.add(note);

            doc.close();
        } catch (Exception ex) {
            if (doc.isOpen()) doc.close();
            throw ex;
        }
        return baos.toByteArray();
    }

    /**
     * Exporte le reçu PDF avec QR code, en utilisant un FileChooser JavaFX.
     */
    public File exportPdfReceipt(Window owner, ReceiptData d) throws Exception {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le reçu PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("recu_paiement_" + d.receiptCode() + ".pdf");
        File target = chooser.showSaveDialog(owner);
        if (target == null) {
            return null;
        }

        byte[] pdfBytes = generatePdfBytes(d);
        try (FileOutputStream fos = new FileOutputStream(target)) {
            fos.write(pdfBytes);
        }
        return target;
    }

    private static String formatDate(LocalDateTime d) {
        return d != null ? d.format(DT) : "—";
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Génère un QR code PNG à partir du payload.
     */
    private static byte[] createQrPng(String payload, int size) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, size, size, hints);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /**
     * Record pour stocker les données du reçu (avec registrationId).
     */
    public record ReceiptData(
            int registrationId,
            String receiptCode,
            String eventName,
            LocalDateTime eventDate,
            String location,
            String participantName,
            String email,
            LocalDateTime registrationDate,
            double amount,
            String paymentMethod,
            String status,
            String qrPayload
    ) {
    }
}
