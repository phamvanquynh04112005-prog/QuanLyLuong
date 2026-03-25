package com.example.QuanLyLuong.service.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;

import com.example.QuanLyLuong.common.PaymentStatus;
import com.example.QuanLyLuong.entity.Employee;
import com.example.QuanLyLuong.entity.Payroll;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class PdfExportService {

    public byte[] exportPayrollListToPdf(List<Payroll> payrolls, int month, int year) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, outputStream);
        document.open();

        BaseFont baseFont = createBaseFont();
        Font titleFont = new Font(baseFont, 16, Font.BOLD);
        Font headerFont = new Font(baseFont, 10, Font.BOLD, BaseColor.WHITE);
        Font bodyFont = new Font(baseFont, 9, Font.NORMAL);
        Font totalFont = new Font(baseFont, 10, Font.BOLD);

        Paragraph title = new Paragraph("BANG LUONG THANG " + month + "/" + year, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(18);
        document.add(title);

        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[] {1f, 3f, 2.5f, 2.5f, 2.5f, 1.6f, 2.8f, 1.8f});

        String[] headers = {"STT", "Nh\u00e2n vi\u00ean", "Ph\u00f2ng ban", "Ch\u1ee9c v\u1ee5", "L\u01b0\u01a1ng c\u01a1 b\u1ea3n", "Ng\u00e0y c\u00f4ng", "L\u01b0\u01a1ng th\u1ef1c nh\u1eadn", "Tr\u1ea1ng th\u00e1i"};
        BaseColor headerBg = new BaseColor(21, 74, 145);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(headerBg);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }

        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        double totalSalary = 0.0;
        for (int i = 0; i < payrolls.size(); i++) {
            Payroll payroll = payrolls.get(i);
            BaseColor rowBg = i % 2 == 0 ? BaseColor.WHITE : new BaseColor(244, 247, 252);
            addCell(table, String.valueOf(i + 1), bodyFont, rowBg, Element.ALIGN_CENTER);
            addCell(table, payroll.getEmployee().getFullName(), bodyFont, rowBg, Element.ALIGN_LEFT);
            addCell(table, payroll.getEmployee().getDepartment() != null ? payroll.getEmployee().getDepartment().getName() : "", bodyFont, rowBg, Element.ALIGN_LEFT);
            addCell(table, safeValue(payroll.getEmployee().getPosition()), bodyFont, rowBg, Element.ALIGN_LEFT);
            addCell(table, numberFormat.format(payroll.getEmployee().getBaseSalary() == null ? 0.0 : payroll.getEmployee().getBaseSalary()) + " d", bodyFont, rowBg, Element.ALIGN_RIGHT);
            addCell(table, String.valueOf(payroll.getTimesheet() != null ? payroll.getTimesheet().getWorkDays() : 0), bodyFont, rowBg, Element.ALIGN_CENTER);
            addCell(table, numberFormat.format(payroll.getActualSalary() == null ? 0.0 : payroll.getActualSalary()) + " d", bodyFont, rowBg, Element.ALIGN_RIGHT);
            addCell(table, payroll.getPaymentStatus() == PaymentStatus.PAID ? "\u0110\u00e3 chi" : "Ch\u01b0a chi", bodyFont, rowBg, Element.ALIGN_CENTER);
            totalSalary += payroll.getActualSalary() == null ? 0.0 : payroll.getActualSalary();
        }

        PdfPCell emptyCell = new PdfPCell(new Phrase(""));
        emptyCell.setColspan(6);
        emptyCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(emptyCell);

        PdfPCell totalLabelCell = new PdfPCell(new Phrase("Tong cong", totalFont));
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabelCell.setPadding(6);
        table.addCell(totalLabelCell);

        PdfPCell totalValueCell = new PdfPCell(new Phrase(numberFormat.format(totalSalary) + " d", totalFont));
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValueCell.setPadding(6);
        table.addCell(totalValueCell);

        document.add(table);
        document.close();
        return outputStream.toByteArray();
    }

    public byte[] exportPayslipToPdf(Payroll payroll) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, outputStream);
        document.open();

        BaseFont baseFont = createBaseFont();
        Font titleFont = new Font(baseFont, 16, Font.BOLD);
        Font bodyFont = new Font(baseFont, 10, Font.NORMAL);
        Font labelFont = new Font(baseFont, 10, Font.BOLD);
        Font totalFont = new Font(baseFont, 11, Font.BOLD, new BaseColor(0, 102, 51));
        NumberFormat numberFormat = NumberFormat.getNumberInstance();

        Employee employee = payroll.getEmployee();

        Paragraph title = new Paragraph("PHIEU LUONG", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subTitle = new Paragraph("Thang " + payroll.getMonth() + "/" + payroll.getYear(), bodyFont);
        subTitle.setAlignment(Element.ALIGN_CENTER);
        subTitle.setSpacingAfter(18);
        document.add(subTitle);

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingAfter(16);
        addInfoRow(infoTable, "Ho va ten", employee.getFullName(), labelFont, bodyFont);
        addInfoRow(infoTable, "Phong ban", employee.getDepartment() != null ? employee.getDepartment().getName() : "", labelFont, bodyFont);
        addInfoRow(infoTable, "Chuc vu", safeValue(employee.getPosition()), labelFont, bodyFont);
        addInfoRow(infoTable, "Ngay vao lam", employee.getJoinDate() != null ? employee.getJoinDate().toString() : "", labelFont, bodyFont);
        addInfoRow(infoTable, "So nguoi phu thuoc", String.valueOf(employee.getDependentCount() == null ? 0 : employee.getDependentCount()), labelFont, bodyFont);
        document.add(infoTable);

        PdfPTable salaryTable = new PdfPTable(2);
        salaryTable.setWidthPercentage(100);
        salaryTable.setWidths(new float[] {3.2f, 1.8f});
        addSalaryRow(salaryTable, "Luong co ban hop dong", money(employee.getBaseSalary(), numberFormat), bodyFont, false);
        addSalaryRow(salaryTable, "Luong co ban theo gio cong", money(payroll.getBaseSalaryAmount(), numberFormat), bodyFont, false);
        addSalaryRow(salaryTable, "Tien OT", money(payroll.getOvertimePay(), numberFormat), bodyFont, false);
        addSalaryRow(salaryTable, "Tong phu cap", money(payroll.getTotalAllowance(), numberFormat), bodyFont, false);
        addSalaryRow(salaryTable, "Tong thuong", money(payroll.getTotalBonus(), numberFormat), bodyFont, false);
        addSalaryRow(salaryTable, "Luong gop", money(payroll.getGrossSalary(), numberFormat), bodyFont, false);
        addSalaryRow(salaryTable, "Bao hiem bat buoc", money(payroll.getInsuranceAmount(), numberFormat), bodyFont, false);
        addSalaryRow(salaryTable, "Giam tru nguoi phu thuoc", money(payroll.getDependentDeductionAmount(), numberFormat), bodyFont, false);
        addSalaryRow(salaryTable, "Thu nhap tinh thue", money(payroll.getTaxableIncome(), numberFormat), bodyFont, false);
        addSalaryRow(salaryTable, "Thue TNCN luy tien", money(payroll.getTaxAmount(), numberFormat), bodyFont, false);
        addSalaryRow(salaryTable, "Khau tru khac", money(payroll.getOtherDeductionAmount(), numberFormat), bodyFont, false);
        addSalaryRow(salaryTable, "Luong thuc nhan", money(payroll.getActualSalary(), numberFormat), totalFont, true);
        document.add(salaryTable);

        Paragraph status = new Paragraph(
                payroll.getPaymentStatus() == PaymentStatus.PAID
                        ? "Trang thai: Da chi ngay " + payroll.getPaymentDate()
                        : "Trang thai: Chua chi",
                bodyFont
        );
        status.setSpacingBefore(14);
        document.add(status);

        document.close();
        return outputStream.toByteArray();
    }

    private BaseFont createBaseFont() throws IOException, DocumentException {
        Path[] candidates = {
                resourcePath("fonts/arial.ttf"),
                Path.of("C:/Windows/Fonts/arial.ttf"),
                Path.of("C:/Windows/Fonts/tahoma.ttf"),
                Path.of("C:/Windows/Fonts/calibri.ttf")
        };

        for (Path candidate : candidates) {
            if (candidate != null && Files.exists(candidate)) {
                return BaseFont.createFont(candidate.toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
        }

        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }

    private Path resourcePath(String location) {
        try {
            return new ClassPathResource(location).getFile().toPath();
        } catch (IOException ex) {
            return null;
        }
    }

    private void addCell(PdfPTable table, String text, Font font, BaseColor background, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(background);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addInfoRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(6);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(6);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addSalaryRow(PdfPTable table, String label, String value, Font font, boolean total) {
        BaseColor background = total ? new BaseColor(233, 247, 237) : BaseColor.WHITE;
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBackgroundColor(background);
        labelCell.setPadding(6);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBackgroundColor(background);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(6);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String money(Double value, NumberFormat numberFormat) {
        return numberFormat.format(value == null ? 0.0 : value) + " d";
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }
}
