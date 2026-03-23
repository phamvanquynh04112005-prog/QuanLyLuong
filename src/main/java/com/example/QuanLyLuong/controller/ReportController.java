package com.example.QuanLyLuong.controller;

import java.time.LocalDate;

import com.example.QuanLyLuong.dto.PayrollReportDashboard;
import com.example.QuanLyLuong.service.ReportService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/department")
    public String departmentReport(@RequestParam(required = false) Integer month,
                                   @RequestParam(required = false) Integer year,
                                   @RequestParam(required = false) Integer rangeMonths,
                                   Model model) {
        LocalDate now = LocalDate.now();
        int selectedMonth = month == null ? now.getMonthValue() : month;
        int selectedYear = year == null ? now.getYear() : year;
        int selectedRangeMonths = rangeMonths == null ? 6 : rangeMonths;

        PayrollReportDashboard reportDashboard = reportService.buildDepartmentReport(
                selectedMonth,
                selectedYear,
                selectedRangeMonths
        );

        model.addAttribute("reportDashboard", reportDashboard);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("year", selectedYear);
        model.addAttribute("rangeMonths", selectedRangeMonths);
        model.addAttribute("pageTitle", "Bao cao luong nang cao");
        model.addAttribute("contentTemplate", "report/department");
        return "layout/base";
    }
}