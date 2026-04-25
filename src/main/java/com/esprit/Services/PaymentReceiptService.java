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
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
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
        Files.write(target.toPath(), pdfBytes);
        return target;
    }

    public byte[] generatePdfBytes(ReceiptData d) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 42, 36);
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, new BaseColor(27, 94, 32));
            Font section = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, new BaseColor(69, 90, 100));
            Font normal = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.DARK_GRAY);
            Font strong = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, new BaseColor(13, 27, 42));

            Paragraph title = new Paragraph("Bledna - Reçu de paiement", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(8f);
            doc.add(title);
            Paragraph code = new Paragraph("Code reçu : " + d.receiptCode(), strong);
            code.setAlignment(Element.ALIGN_CENTER);
            code.setSpacingAfter(12f);
            doc.add(code);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(6f);
            table.setSpacingAfter(10f);
            table.setWidths(new float[]{1.2f, 2.4f});

            addRow(table, "Événement", d.eventName(), section, normal);
            addRow(table, "Date événement", formatDate(d.eventDate()), section, normal);
            addRow(table, "Lieu", d.location(), section, normal);
            addRow(table, "Participant", d.participantName(), section, normal);
            addRow(table, "Email", d.email(), section, normal);
            addRow(table, "Date inscription", formatDate(d.registrationDate()), section, normal);
            addRow(table, "Paiement", d.paymentMethod(), section, normal);
            addRow(table, "Statut", d.status(), section, normal);
            addRow(table, "Montant payé", String.format(Locale.FRANCE, "%.2f TND", d.amount()), section, strong);
            doc.add(table);

            Paragraph qrTitle = new Paragraph("QR code de vérification", section);
            qrTitle.setAlignment(Element.ALIGN_CENTER);
            qrTitle.setSpacingBefore(8f);
            doc.add(qrTitle);

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

    private static String formatDate(LocalDateTime d) {
        return d != null ? d.format(DT) : "—";
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static void addRow(PdfPTable table, String key, String value, Font keyFont, Font valFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(key, keyFont));
        c1.setBorder(Rectangle.BOX);
        c1.setBorderColor(new BaseColor(220, 231, 240));
        c1.setPadding(8f);
        c1.setBackgroundColor(new BaseColor(248, 251, 253));
        PdfPCell c2 = new PdfPCell(new Phrase(value != null ? value : "—", valFont));
        c2.setBorder(Rectangle.BOX);
        c2.setBorderColor(new BaseColor(220, 231, 240));
        c2.setPadding(8f);
        table.addCell(c1);
        table.addCell(c2);
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
     * Record pour stocker les données du reçu.
     */
    public record ReceiptData(
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
