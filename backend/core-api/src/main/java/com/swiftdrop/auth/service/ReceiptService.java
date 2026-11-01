package com.swiftdrop.auth.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

@Service
public class ReceiptService {

    public byte[] generateDeliveryReceipt(String trackingNumber, String senderName, String receiverName,
            String destinationCity, double weightKg, double deliveryFee) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Document Header
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(30, 58, 95));
            Paragraph title = new Paragraph("SwiftDrop Delivery Receipt", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            Paragraph subTitle = new Paragraph("Official Payment & Delivery Confirmation", subFont);
            subTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subTitle);
            document.add(Chunk.NEWLINE);

            // Details Table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);

            addTableCell(table, "Tracking Number:", trackingNumber, true);
            addTableCell(table, "Date & Time:", LocalDateTime.now().toString(), false);
            addTableCell(table, "Sender Name:", senderName, false);
            addTableCell(table, "Recipient Name:", receiverName, false);
            addTableCell(table, "Destination City:", destinationCity, false);
            addTableCell(table, "Parcel Weight:", weightKg + " kg", false);
            addTableCell(table, "Total Fee Paid (LKR):", String.format("%.2f LKR", deliveryFee), true);

            document.add(table);
            document.add(Chunk.NEWLINE);

            // Footer Note
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.DARK_GRAY);
            Paragraph footer = new Paragraph(
                    "Thank you for choosing SwiftDrop Courier Services. For inquiries, visit swiftdrop.lk", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while generating PDF receipt", e);
        }

        return out.toByteArray();
    }

    private void addTableCell(PdfPTable table, String label, String value, boolean isBold) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(30, 58, 95));
        Font valueFont = FontFactory.getFont(isBold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 10,
                Color.BLACK);

        PdfPCell cell1 = new PdfPCell(new Phrase(label, labelFont));
        cell1.setPadding(8);
        cell1.setBackgroundColor(new Color(243, 244, 246));

        PdfPCell cell2 = new PdfPCell(new Phrase(value, valueFont));
        cell2.setPadding(8);

        table.addCell(cell1);
        table.addCell(cell2);
    }
}