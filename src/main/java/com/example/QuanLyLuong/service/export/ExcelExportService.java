package com.example.QuanLyLuong.service.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import com.example.QuanLyLuong.common.PaymentStatus;
import com.example.QuanLyLuong.entity.Payroll;
import com.example.QuanLyLuong.entity.Timesheet;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExcelExportService {

    public byte[] exportPayrollToExcel(List<Payroll> payrolls, int month, int year) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Bảng lương " + month + "-" + year);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);

            CellStyle moneyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            moneyStyle.setDataFormat(format.getFormat("#,##0"));
            moneyStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle totalStyle = workbook.createCellStyle();
            totalStyle.cloneStyleFrom(moneyStyle);
            totalStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BANG LUONG THANG " + month + "/" + year);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            String[] headers = {"STT", "Nhân viên", "Phòng ban", "Chức vụ", "Lương cơ bản", "Ngày công", "Lương thực nhận", "Trạng thái"};
            Row headerRow = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            double totalSalary = 0.0;
            int rowNum = 3;
            for (int i = 0; i < payrolls.size(); i++) {
                Payroll payroll = payrolls.get(i);
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(payroll.getEmployee().getFullName());
                row.createCell(2).setCellValue(payroll.getEmployee().getDepartment() != null ? payroll.getEmployee().getDepartment().getName() : "");
                row.createCell(3).setCellValue(payroll.getEmployee().getPosition() != null ? payroll.getEmployee().getPosition() : "");

                Cell baseSalaryCell = row.createCell(4);
                baseSalaryCell.setCellValue(payroll.getEmployee().getBaseSalary() == null ? 0.0 : payroll.getEmployee().getBaseSalary());
                baseSalaryCell.setCellStyle(moneyStyle);

                row.createCell(5).setCellValue(payroll.getTimesheet() != null ? payroll.getTimesheet().getWorkDays() : 0);

                Cell actualSalaryCell = row.createCell(6);
                actualSalaryCell.setCellValue(payroll.getActualSalary() == null ? 0.0 : payroll.getActualSalary());
                actualSalaryCell.setCellStyle(moneyStyle);

                row.createCell(7).setCellValue(payroll.getPaymentStatus() == PaymentStatus.PAID ? "Đã chi" : "Chưa chi");
                totalSalary += payroll.getActualSalary() == null ? 0.0 : payroll.getActualSalary();
            }

            Row totalRow = sheet.createRow(rowNum);
            totalRow.createCell(5).setCellValue("Tong cong");
            Cell totalCell = totalRow.createCell(6);
            totalCell.setCellValue(totalSalary);
            totalCell.setCellStyle(totalStyle);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    public byte[] exportTimesheetToExcel(List<Timesheet> timesheets, int month, int year) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Bang cong " + month + "-" + year);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);

            CellStyle numberStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            numberStyle.setDataFormat(format.getFormat("#,##0.00"));
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle totalStyle = workbook.createCellStyle();
            totalStyle.cloneStyleFrom(numberStyle);
            totalStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BANG CONG THANG " + month + "/" + year);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            String[] headers = {"STT", "Nhan vien", "Phong ban", "Ngay cong", "Nghi phep", "Gio thuong", "Gio tang ca", "So log", "Ghi chu"};
            Row headerRow = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            double totalWorkDays = 0.0;
            double totalLeaveDays = 0.0;
            double totalRegularHours = 0.0;
            double totalOvertimeHours = 0.0;

            int rowNum = 3;
            for (int i = 0; i < timesheets.size(); i++) {
                Timesheet timesheet = timesheets.get(i);
                double overtimeHours = safeDouble(timesheet.getOvertimeWeekdayHours())
                        + safeDouble(timesheet.getOvertimeWeekendHours())
                        + safeDouble(timesheet.getOvertimeHolidayHours());

                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(timesheet.getEmployee().getFullName());
                row.createCell(2).setCellValue(timesheet.getEmployee().getDepartment() != null ? timesheet.getEmployee().getDepartment().getName() : "");
                row.createCell(3).setCellValue(timesheet.getWorkDays() == null ? 0 : timesheet.getWorkDays());
                row.createCell(4).setCellValue(timesheet.getLeaveDays() == null ? 0 : timesheet.getLeaveDays());

                Cell regularHoursCell = row.createCell(5);
                regularHoursCell.setCellValue(safeDouble(timesheet.getRegularHours()));
                regularHoursCell.setCellStyle(numberStyle);

                Cell overtimeHoursCell = row.createCell(6);
                overtimeHoursCell.setCellValue(overtimeHours);
                overtimeHoursCell.setCellStyle(numberStyle);

                row.createCell(7).setCellValue(timesheet.getImportedLogCount() == null ? 0 : timesheet.getImportedLogCount());
                row.createCell(8).setCellValue(timesheet.getNote() == null ? "" : timesheet.getNote());

                totalWorkDays += timesheet.getWorkDays() == null ? 0 : timesheet.getWorkDays();
                totalLeaveDays += timesheet.getLeaveDays() == null ? 0 : timesheet.getLeaveDays();
                totalRegularHours += safeDouble(timesheet.getRegularHours());
                totalOvertimeHours += overtimeHours;
            }

            Row totalRow = sheet.createRow(rowNum);
            totalRow.createCell(2).setCellValue("Tong cong");

            Cell totalWorkDaysCell = totalRow.createCell(3);
            totalWorkDaysCell.setCellValue(totalWorkDays);
            totalWorkDaysCell.setCellStyle(totalStyle);

            Cell totalLeaveDaysCell = totalRow.createCell(4);
            totalLeaveDaysCell.setCellValue(totalLeaveDays);
            totalLeaveDaysCell.setCellStyle(totalStyle);

            Cell totalRegularHoursCell = totalRow.createCell(5);
            totalRegularHoursCell.setCellValue(totalRegularHours);
            totalRegularHoursCell.setCellStyle(totalStyle);

            Cell totalOvertimeHoursCell = totalRow.createCell(6);
            totalOvertimeHoursCell.setCellValue(totalOvertimeHours);
            totalOvertimeHoursCell.setCellStyle(totalStyle);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }
}

