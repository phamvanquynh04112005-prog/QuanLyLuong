package com.example.QuanLyLuong.controller;

import com.example.QuanLyLuong.common.Role;
import com.example.QuanLyLuong.entity.Payroll;
import com.example.QuanLyLuong.entity.User;
import com.example.QuanLyLuong.service.PayrollService;
import com.example.QuanLyLuong.service.TimesheetService;
import com.example.QuanLyLuong.service.UserService;
import com.example.QuanLyLuong.service.export.ExcelExportService;
import com.example.QuanLyLuong.service.export.PdfExportService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    private final PayrollService payrollService;
    private final TimesheetService timesheetService;
    private final UserService userService;

    @GetMapping("/excel/payroll")
    public ResponseEntity<byte[]> exportPayrollExcel(@RequestParam Integer month, @RequestParam Integer year) throws Exception {
        byte[] data = excelExportService.exportPayrollToExcel(payrollService.findByMonth(month, year), month, year);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bang-luong-" + month + "-" + year + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/pdf/payroll")
    public ResponseEntity<byte[]> exportPayrollPdf(@RequestParam Integer month, @RequestParam Integer year) throws Exception {
        byte[] data = pdfExportService.exportPayrollListToPdf(payrollService.findByMonth(month, year), month, year);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bang-luong-" + month + "-" + year + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/excel/timesheet")
    public ResponseEntity<byte[]> exportTimesheetExcel(@RequestParam Integer month, @RequestParam Integer year) throws Exception {
        byte[] data = excelExportService.exportTimesheetToExcel(timesheetService.findAllByMonth(month, year), month, year);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bang-cong-" + month + "-" + year + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/pdf/payslip/{payrollId}")
    public ResponseEntity<byte[]> exportPayslip(@PathVariable Long payrollId, Authentication authentication) throws Exception {
        Payroll payroll = payrollService.findById(payrollId);
        User currentUser = userService.getAuthenticatedUser(authentication);
        if (currentUser.getRole() == Role.ROLE_EMPLOYEE
                && (currentUser.getEmployee() == null || !currentUser.getEmployee().getId().equals(payroll.getEmployee().getId()))) {
            throw new AccessDeniedException("Ban khong du quyen tai phieu luong nay.");
        }

        byte[] data = pdfExportService.exportPayslipToPdf(payroll);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=phieu-luong-" + payroll.getId() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }
}
