package com.example.QuanLyLuong.controller;

import java.time.LocalDate;
import java.util.List;

import com.example.QuanLyLuong.dto.DepartmentReportItem;
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
                                   Model model) {
        LocalDate now = LocalDate.now();
        int selectedMonth = month == null ? now.getMonthValue() : month;
        int selectedYear = year == null ? now.getYear() : year;
        List<DepartmentReportItem> reportItems = reportService.reportByDepartment(selectedMonth, selectedYear);
        double total = reportItems.stream().mapToDouble(DepartmentReportItem::getTotalSalary).sum();

        model.addAttribute("reportItems", reportItems);
        model.addAttribute("overallTotal", total);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("year", selectedYear);
        model.addAttribute("pageTitle", "Bao cao phong ban");
        model.addAttribute("contentTemplate", "report/department");
        return "layout/base";
    }
}
