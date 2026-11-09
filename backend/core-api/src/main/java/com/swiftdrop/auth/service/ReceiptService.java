package com.swiftdrop.auth.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.swiftdrop.auth.dto.ReceiptDataDto;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReceiptService {

    private static final Color NAVY_PRIMARY = new Color(30, 58, 95);
    private static final Color ACCENT_BLUE = new Color(43, 76, 126);
    private static final Color TABLE_HEADER_BG = new Color(240, 244, 250);
    private static final Color LIGHT_ROW_BG = new Color(249, 250, 251);
    private static final Color BORDER_COLOR = new Color(229, 231, 235);
    private static final Color TEXT_DARK = new Color(31, 41, 55);
    private static final Color TEXT_MUTED = new Color(107, 114, 128);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Builds a styled A4 PDF delivery receipt using OpenPDF based on rich metadata.
     */
    public byte[] generateDeliveryReceipt(ReceiptDataDto data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Header & Brand Title
            addHeader(document, data);

            // 2. Metadata Banner (Tracking, Status, Issue Date)
            addMetadataBanner(document, data);

            document.add(new Paragraph(" "));

            // 3. Sender & Recipient Address Grid (2 columns)
            addAddressSection(document, data);

            document.add(new Paragraph(" "));

            // 4. Parcel & Service Attributes
            addParcelDetailsSection(document, data);

            document.add(new Paragraph(" "));

            // 5. Status Milestone History (if available)
            if (data.getStatusHistory() != null && !data.getStatusHistory().isEmpty()) {
                addStatusHistorySection(document, data.getStatusHistory());
                document.add(new Paragraph(" "));
            }

            // 6. Fee Breakdown Table (LKR)
            addFeeBreakdownSection(document, data);

            document.add(new Paragraph(" "));

            // 7. Footer & Legal Disclaimer
            addFooter(document);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while generating PDF delivery receipt", e);
        }

        return out.toByteArray();
    }

    /**
     * Legacy overloaded method to preserve backward compatibility with existing callers and tests.
     */
    public byte[] generateDeliveryReceipt(String trackingNumber, String senderName, String receiverName,
                                          String destinationCity, double weightKg, double deliveryFee) {
        ReceiptDataDto dto = ReceiptDataDto.builder()
                .receiptId("RCPT-" + trackingNumber)
                .trackingNumber(trackingNumber)
                .issueDate(LocalDateTime.now().format(DATE_FORMATTER))
                .senderName(senderName != null ? senderName : "Valued Customer")
                .receiverName(receiverName != null ? receiverName : "Recipient")
                .dropoffAddress(destinationCity)
                .weightKg(BigDecimal.valueOf(weightKg))
                .serviceType("Standard Delivery")
                .currentStatus("DELIVERED")
                .deliveryFee(BigDecimal.valueOf(deliveryFee))
                .build();

        return generateDeliveryReceipt(dto);
    }

    // ---------------------------------------------------------------------------------------------
    // SECTION BUILDERS
    // ---------------------------------------------------------------------------------------------

    private void addHeader(Document document, ReceiptDataDto data) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{65, 35});

        // Left Header: Company Branding
        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, NAVY_PRIMARY);
        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_MUTED);

        Paragraph brandPara = new Paragraph("SWIFTDROP COURIER SERVICES", brandFont);
        brandPara.add(new Chunk("\nOfficial Delivery Receipt & Tax Invoice", subFont));
        brandPara.add(new Chunk("\nWeb: swiftdrop.lk | Hotline: +94 11 234 5678", FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_MUTED)));

        PdfPCell leftCell = new PdfPCell(brandPara);
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPaddingBottom(8f);
        headerTable.addCell(leftCell);

        // Right Header: Receipt Reference & Issue Date
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, NAVY_PRIMARY);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_DARK);

        Paragraph rightPara = new Paragraph();
        rightPara.setAlignment(Element.ALIGN_RIGHT);
        rightPara.add(new Chunk("Receipt ID: ", labelFont));
        rightPara.add(new Chunk((data.getReceiptId() != null ? data.getReceiptId() : "N/A") + "\n", valueFont));
        rightPara.add(new Chunk("Issue Date: ", labelFont));
        rightPara.add(new Chunk((data.getIssueDate() != null ? data.getIssueDate() : LocalDateTime.now().format(DATE_FORMATTER)) + "\n", valueFont));
        rightPara.add(new Chunk("Currency: ", labelFont));
        rightPara.add(new Chunk("LKR (Sri Lankan Rupee)", valueFont));

        PdfPCell rightCell = new PdfPCell(rightPara);
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setPaddingBottom(8f);
        headerTable.addCell(rightCell);

        document.add(headerTable);
    }

    private void addMetadataBanner(Document document, ReceiptDataDto data) throws DocumentException {
        PdfPTable banner = new PdfPTable(3);
        banner.setWidthPercentage(100);
        banner.setWidths(new float[]{35, 35, 30});

        PdfPCell cell1 = createBannerCell("TRACKING NUMBER", data.getTrackingNumber(), true);
        PdfPCell cell2 = createBannerCell("DELIVERY STATUS", data.getCurrentStatus() != null ? data.getCurrentStatus() : "IN_TRANSIT", true);
        PdfPCell cell3 = createBannerCell("SERVICE LEVEL", data.getServiceType() != null ? data.getServiceType() : "Standard Parcel", false);

        banner.addCell(cell1);
        banner.addCell(cell2);
        banner.addCell(cell3);

        document.add(banner);
    }

    private PdfPCell createBannerCell(String label, String value, boolean highlight) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXT_MUTED);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, highlight ? NAVY_PRIMARY : TEXT_DARK);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", labelFont));
        p.add(new Chunk(value != null ? value : "N/A", valueFont));

        PdfPCell cell = new PdfPCell(p);
        cell.setBackgroundColor(TABLE_HEADER_BG);
        cell.setPadding(8f);
        cell.setBorderColor(BORDER_COLOR);
        return cell;
    }

    private void addAddressSection(Document document, ReceiptDataDto data) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50, 50});

        // Sender Column Header
        PdfPCell senderHeader = createHeaderCell("SENDER / PICKUP DETAILS");
        PdfPCell receiverHeader = createHeaderCell("RECIPIENT / DROP-OFF DETAILS");
        table.addCell(senderHeader);
        table.addCell(receiverHeader);

        // Sender Body
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_DARK);
        Font regFont = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_DARK);

        Paragraph senderPara = new Paragraph();
        senderPara.add(new Chunk("Name: ", boldFont));
        senderPara.add(new Chunk((data.getSenderName() != null ? data.getSenderName() : "N/A") + "\n", regFont));
        if (data.getSenderPhone() != null && !data.getSenderPhone().isBlank()) {
            senderPara.add(new Chunk("Phone: ", boldFont));
            senderPara.add(new Chunk(data.getSenderPhone() + "\n", regFont));
        }
        senderPara.add(new Chunk("Pickup Address:\n", boldFont));
        senderPara.add(new Chunk(data.getPickupAddress() != null ? data.getPickupAddress() : "Address on file", regFont));

        PdfPCell senderCell = new PdfPCell(senderPara);
        senderCell.setPadding(8f);
        senderCell.setBorderColor(BORDER_COLOR);
        table.addCell(senderCell);

        // Receiver Body
        Paragraph receiverPara = new Paragraph();
        receiverPara.add(new Chunk("Name: ", boldFont));
        receiverPara.add(new Chunk((data.getReceiverName() != null ? data.getReceiverName() : "N/A") + "\n", regFont));
        if (data.getReceiverPhone() != null && !data.getReceiverPhone().isBlank()) {
            receiverPara.add(new Chunk("Phone: ", boldFont));
            receiverPara.add(new Chunk(data.getReceiverPhone() + "\n", regFont));
        }
        receiverPara.add(new Chunk("Drop-off Address:\n", boldFont));
        receiverPara.add(new Chunk(data.getDropoffAddress() != null ? data.getDropoffAddress() : "Destination on file", regFont));

        PdfPCell receiverCell = new PdfPCell(receiverPara);
        receiverCell.setPadding(8f);
        receiverCell.setBorderColor(BORDER_COLOR);
        table.addCell(receiverCell);

        document.add(table);
    }

    private void addParcelDetailsSection(Document document, ReceiptDataDto data) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{25, 25, 25, 25});

        table.addCell(createSmallCell("PARCEL WEIGHT", (data.getWeightKg() != null ? data.getWeightKg() : "0.0") + " kg"));
        table.addCell(createSmallCell("PARCEL SIZE", data.getSize() != null ? data.getSize() : "STANDARD"));
        table.addCell(createSmallCell("TRACKING PREFIX", "SD (Verified)"));
        table.addCell(createSmallCell("PAYMENT STATUS", "PAID"));

        document.add(table);
    }

    private PdfPCell createSmallCell(String label, String value) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, TEXT_MUTED);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_DARK);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", labelFont));
        p.add(new Chunk(value, valueFont));

        PdfPCell cell = new PdfPCell(p);
        cell.setPadding(6f);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBackgroundColor(LIGHT_ROW_BG);
        return cell;
    }

    private void addStatusHistorySection(Document document, List<ReceiptDataDto.StatusTimelineEntry> history) throws DocumentException {
        Font secTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, NAVY_PRIMARY);
        document.add(new Paragraph("STATUS HISTORY & TIMELINE", secTitleFont));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{25, 30, 20, 25});

        table.addCell(createHeaderCell("Status"));
        table.addCell(createHeaderCell("Date & Time"));
        table.addCell(createHeaderCell("Changed By"));
        table.addCell(createHeaderCell("Notes"));

        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_DARK);

        for (ReceiptDataDto.StatusTimelineEntry entry : history) {
            PdfPCell c1 = new PdfPCell(new Phrase(entry.getStatus() != null ? entry.getStatus() : "-", bodyFont));
            PdfPCell c2 = new PdfPCell(new Phrase(entry.getChangedAt() != null ? entry.getChangedAt() : "-", bodyFont));
            PdfPCell c3 = new PdfPCell(new Phrase(entry.getChangedByRole() != null ? entry.getChangedByRole() : "-", bodyFont));
            PdfPCell c4 = new PdfPCell(new Phrase(entry.getNote() != null ? entry.getNote() : "-", bodyFont));

            styleRowCells(c1, c2, c3, c4);
            table.addCell(c1);
            table.addCell(c2);
            table.addCell(c3);
            table.addCell(c4);
        }

        document.add(table);
    }

    private void addFeeBreakdownSection(Document document, ReceiptDataDto data) throws DocumentException {
        Font secTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, NAVY_PRIMARY);
        document.add(new Paragraph("PAYMENT & CHARGES BREAKDOWN", secTitleFont));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{70, 30});

        table.addCell(createHeaderCell("Description"));
        PdfPCell amountHeader = createHeaderCell("Amount (LKR)");
        amountHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(amountHeader);

        BigDecimal total = data.getDeliveryFee() != null ? data.getDeliveryFee() : BigDecimal.ZERO;
        // Estimated base rate vs handling split
        BigDecimal base = total.multiply(BigDecimal.valueOf(0.85)).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal handling = total.subtract(base);

        addChargeRow(table, "Base Delivery Charge & Weight Tariff", String.format("%.2f", base.doubleValue()));
        addChargeRow(table, "Service Handling & Operational Levy", String.format("%.2f", handling.doubleValue()));

        // Total Paid Cell
        Font totalLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, NAVY_PRIMARY);
        Font totalValueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, NAVY_PRIMARY);

        PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL AMOUNT PAID", totalLabelFont));
        totalLabelCell.setPadding(8f);
        totalLabelCell.setBackgroundColor(TABLE_HEADER_BG);
        totalLabelCell.setBorderColor(BORDER_COLOR);

        PdfPCell totalValueCell = new PdfPCell(new Phrase(String.format("Rs. %.2f LKR", total.doubleValue()), totalValueFont));
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValueCell.setPadding(8f);
        totalValueCell.setBackgroundColor(TABLE_HEADER_BG);
        totalValueCell.setBorderColor(BORDER_COLOR);

        table.addCell(totalLabelCell);
        table.addCell(totalValueCell);

        document.add(table);
    }

    private void addChargeRow(PdfPTable table, String description, String amount) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_DARK);

        PdfPCell descCell = new PdfPCell(new Phrase(description, font));
        descCell.setPadding(6f);
        descCell.setBorderColor(BORDER_COLOR);

        PdfPCell amtCell = new PdfPCell(new Phrase(amount, font));
        amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        amtCell.setPadding(6f);
        amtCell.setBorderColor(BORDER_COLOR);

        table.addCell(descCell);
        table.addCell(amtCell);
    }

    private void addFooter(Document document) throws DocumentException {
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_MUTED);
        Paragraph footer = new Paragraph(
                "This is a computer-generated delivery confirmation and payment receipt issued by SwiftDrop.\n" +
                "For inquiries, support, or complaints, please visit https://swiftdrop.lk or email support@swiftdrop.lk",
                footerFont
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private PdfPCell createHeaderCell(String text) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, headerFont));
        cell.setBackgroundColor(ACCENT_BLUE);
        cell.setPadding(6f);
        cell.setBorderColor(ACCENT_BLUE);
        return cell;
    }

    private void styleRowCells(PdfPCell... cells) {
        for (PdfPCell cell : cells) {
            cell.setPadding(5f);
            cell.setBorderColor(BORDER_COLOR);
        }
    }
}
