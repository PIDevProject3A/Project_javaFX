package com.esprit.services;

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

            Font appTag = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, new BaseColor(96, 125, 139));
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 28, Font.BOLD, new BaseColor(27, 94, 32));
            Font codeFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, BaseColor.DARK_GRAY);
            Font eventFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, new BaseColor(13, 27, 42));
            Font normal = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.DARK_GRAY);
            Font amountFont = new Font(Font.FontFamily.HELVETICA, 30, Font.BOLD, new BaseColor(13, 27, 42));
            Font qrInfo = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(96, 125, 139));

            Paragraph app = new Paragraph("BLEDNA", appTag);
            app.setAlignment(Element.ALIGN_LEFT);
            app.setSpacingAfter(4f);
            doc.add(app);

            Paragraph title = new Paragraph("Reçu de paiement", titleFont);
            title.setAlignment(Element.ALIGN_LEFT);
            title.setSpacingAfter(12f);
            doc.add(title);

            PdfPTable card = new PdfPTable(1);
            card.setWidthPercentage(100f);
            PdfPCell cardCell = new PdfPCell();
            cardCell.setPadding(16f);
            cardCell.setBorderColor(new BaseColor(219, 229, 239));
            cardCell.setBorderWidth(1f);
            cardCell.setBackgroundColor(new BaseColor(248, 250, 252));

            Paragraph code = new Paragraph("Code reçu : " + d.receiptCode(), codeFont);
            code.setSpacingAfter(12f);
            cardCell.addElement(code);

            Paragraph event = new Paragraph(nullSafe(d.eventName()), eventFont);
            event.setSpacingAfter(8f);
            cardCell.addElement(event);

            Paragraph eventMeta = new Paragraph(
                    "\uD83D\uDCC5 " + formatDate(d.eventDate()) + "    \uD83D\uDCCD " + nullSafe(d.location()),
                    normal);
            eventMeta.setSpacingAfter(12f);
            cardCell.addElement(eventMeta);

            PdfPTable info = new PdfPTable(1);
            info.setWidthPercentage(100f);
            info.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            info.addCell(noBorder("Participant : " + nullSafe(d.participantName()), normal));
            info.addCell(noBorder("Email : " + nullSafe(d.email()), normal));
            info.addCell(noBorder("Date d'inscription : " + formatDate(d.registrationDate()), normal));
            info.addCell(noBorder("Méthode de paiement : " + nullSafe(d.paymentMethod()), normal));
            info.setSpacingAfter(8f);
            cardCell.addElement(info);

            Paragraph amount = new Paragraph(String.format(Locale.FRANCE, "Montant payé : %.2f TND", d.amount()), amountFont);
            amount.setSpacingBefore(6f);
            cardCell.addElement(amount);

            card.addCell(cardCell);
            card.setSpacingAfter(18f);
            doc.add(card);

            byte[] qrPng = createQrPng(d.qrPayload(), 260);
            com.itextpdf.text.Image qr = com.itextpdf.text.Image.getInstance(qrPng);
            qr.scaleToFit(140f, 140f);

            PdfPTable qrRow = new PdfPTable(new float[]{1f, 3f});
            qrRow.setWidthPercentage(100f);
            PdfPCell qrCell = new PdfPCell(qr, true);
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            qrCell.setPadding(0f);
            qrRow.addCell(qrCell);

            Paragraph note = new Paragraph("Présentez ce reçu à l'entrée\nScannez le QR code pour vérifier l'authenticité.", qrInfo);
            PdfPCell noteCell = new PdfPCell(new Phrase(note));
            noteCell.setBorder(Rectangle.NO_BORDER);
            noteCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            noteCell.setPaddingLeft(10f);
            qrRow.addCell(noteCell);
            doc.add(qrRow);

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

    private static PdfPCell noBorder(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(4f);
        return cell;
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

    //Record pour stocker les données du reçu (avec registrationId).

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

