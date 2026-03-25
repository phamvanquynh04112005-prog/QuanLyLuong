package com.example.QuanLyLuong.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ReportService reportService;

    @GetMapping("/department")
    public String departmentReport(@RequestParam(required = false) Integer month,
                                   @RequestParam(required = false) Integer year,
                                   @RequestParam(required = false, defaultValue = "month") String periodType,
                                   @RequestParam(required = false) String date,
                                   @RequestParam(required = false) Integer quarter,
                                   @RequestParam(required = false, defaultValue = "desc") String sort,
                                   Model model) {
        LocalDate now = LocalDate.now();
        String normalizedPeriodType = normalizePeriodType(periodType);
        String normalizedSort = "asc".equalsIgnoreCase(sort) ? "asc" : "desc";
        int selectedMonth = month == null ? now.getMonthValue() : clampMonth(month);
        int selectedYear = year == null ? now.getYear() : year;
        int selectedQuarter = quarter == null ? monthToQuarter(selectedMonth) : Math.max(1, Math.min(quarter, 4));
        LocalDate selectedDate = parseDateOrDefault(date, now);
        int selectedRangeMonths;
        String periodSummary;
        String comparisonSummary;

        switch (normalizedPeriodType) {
            case "day" -> {
                selectedMonth = selectedDate.getMonthValue();
                selectedYear = selectedDate.getYear();
                selectedQuarter = monthToQuarter(selectedMonth);
                selectedRangeMonths = 6;
                YearMonth previous = previousMonth(selectedMonth, selectedYear);
                periodSummary = "Ng\u00e0y " + selectedDate.format(DAY_FORMATTER) + " (d\u1eef li\u1ec7u th\u00e1ng " + selectedMonth + "/" + selectedYear + ")";
                comparisonSummary = "So s\u00e1nh v\u1edbi th\u00e1ng " + previous.getMonthValue() + "/" + previous.getYear();
            }
            case "quarter" -> {
                selectedMonth = selectedQuarter * 3;
                selectedDate = LocalDate.of(selectedYear, selectedMonth, 1);
                selectedRangeMonths = 3;
                int previousQuarterMonth = selectedMonth - 3 <= 0 ? 12 + (selectedMonth - 3) : selectedMonth - 3;
                int previousQuarterYear = selectedMonth - 3 <= 0 ? selectedYear - 1 : selectedYear;
                periodSummary = "Qu\u00fd " + selectedQuarter + "/" + selectedYear;
                comparisonSummary = "So s\u00e1nh v\u1edbi qu\u00fd " + monthToQuarter(previousQuarterMonth) + "/" + previousQuarterYear;
            }
            case "year" -> {
                selectedMonth = 12;
                selectedQuarter = 4;
                selectedDate = LocalDate.of(selectedYear, 1, 1);
                selectedRangeMonths = 12;
                periodSummary = "N\u0103m " + selectedYear;
                comparisonSummary = "So s\u00e1nh v\u1edbi n\u0103m " + (selectedYear - 1);
            }
            default -> {
                selectedRangeMonths = 6;
                YearMonth previous = previousMonth(selectedMonth, selectedYear);
                periodSummary = "Th\u00e1ng " + selectedMonth + "/" + selectedYear;
                comparisonSummary = "So s\u00e1nh v\u1edbi th\u00e1ng " + previous.getMonthValue() + "/" + previous.getYear();
            }
        }

        PayrollReportDashboard reportDashboard = reportService.buildDepartmentReport(
                selectedMonth,
                selectedYear,
                selectedRangeMonths,
                normalizedSort
        );

        model.addAttribute("reportDashboard", reportDashboard);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("year", selectedYear);
        model.addAttribute("periodType", normalizedPeriodType);
        model.addAttribute("date", selectedDate.toString());
        model.addAttribute("quarter", selectedQuarter);
        model.addAttribute("sort", normalizedSort);
        model.addAttribute("periodSummary", periodSummary);
        model.addAttribute("comparisonSummary", comparisonSummary);
        model.addAttribute("rangeMonths", selectedRangeMonths);
        model.addAttribute("pageTitle", "B\u00e1o c\u00e1o l\u01b0\u01a1ng n\u00e2ng cao");
        model.addAttribute("contentTemplate", "report/department");
        return "layout/base";
    }

    private String normalizePeriodType(String periodType) {
        if (periodType == null || periodType.isBlank()) {
            return "month";
        }
        String normalized = periodType.trim().toLowerCase();
        return switch (normalized) {
            case "day", "quarter", "year" -> normalized;
            default -> "month";
        };
    }

    private LocalDate parseDateOrDefault(String date, LocalDate fallback) {
        if (date == null || date.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException exception) {
            return fallback;
        }
    }

    private int clampMonth(int month) {
        return Math.max(1, Math.min(12, month));
    }

    private int monthToQuarter(int month) {
        return ((month - 1) / 3) + 1;
    }

    private YearMonth previousMonth(int month, int year) {
        return YearMonth.of(year, month).minusMonths(1);
    }
}
