package com.example.QuanLyLuong.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import com.example.QuanLyLuong.common.AttendanceSource;
import com.example.QuanLyLuong.dto.AttendanceImportResult;
import com.example.QuanLyLuong.entity.AttendanceLog;
import com.example.QuanLyLuong.entity.Timesheet;
import com.example.QuanLyLuong.service.AttendanceLogService;
import com.example.QuanLyLuong.service.TimesheetService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/timesheets")
@RequiredArgsConstructor
public class TimesheetController {

    private final TimesheetService timesheetService;
    private final AttendanceLogService attendanceLogService;

    @GetMapping
    public String list(@RequestParam(required = false) Integer month,
                       @RequestParam(required = false) Integer year,
                       Model model) {
        LocalDate now = LocalDate.now();
        int selectedMonth = month == null ? now.getMonthValue() : month;
        int selectedYear = year == null ? now.getYear() : year;

        model.addAttribute("timesheets", timesheetService.findAllByMonth(selectedMonth, selectedYear));
        model.addAttribute("month", selectedMonth);
        model.addAttribute("year", selectedYear);
        model.addAttribute("pageTitle", "Cham cong");
        model.addAttribute("contentTemplate", "timesheet/list");
        return "layout/base";
    }

    @GetMapping("/{id}/details")
    public String details(@PathVariable Long id,
                          @RequestParam(required = false) Integer month,
                          @RequestParam(required = false) Integer year,
                          Model model) {
        Timesheet timesheet = timesheetService.findById(id);
        int selectedMonth = month == null ? timesheet.getMonth() : month;
        int selectedYear = year == null ? timesheet.getYear() : year;
        YearMonth yearMonth = YearMonth.of(selectedYear, selectedMonth);
        List<AttendanceLog> logs = attendanceLogService.findByEmployeeAndMonth(timesheet.getEmployee().getId(), yearMonth);

        double totalOvertimeHours = logs.stream()
                .mapToDouble(log -> safeDouble(log.getOvertimeWeekdayHours()) + safeDouble(log.getOvertimeWeekendHours()) + safeDouble(log.getOvertimeHolidayHours()))
                .sum();

        model.addAttribute("timesheet", timesheet);
        model.addAttribute("logs", logs);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("year", selectedYear);
        model.addAttribute("totalOvertimeHours", totalOvertimeHours);
        model.addAttribute("pageTitle", "Chi tiet cham cong");
        model.addAttribute("contentTemplate", "timesheet/detail");
        return "layout/base";
    }

    @PostMapping("/import")
    public String importExcel(@RequestParam Integer month,
                              @RequestParam Integer year,
                              @RequestParam(defaultValue = "EXCEL_IMPORT") AttendanceSource source,
                              @RequestParam MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        AttendanceImportResult result = attendanceLogService.importFromExcel(file, YearMonth.of(year, month), source);
        redirectAttributes.addFlashAttribute(
                "successMsg",
                "Import xong: moi " + result.getImportedCount() + ", cap nhat " + result.getUpdatedCount() + ", bo qua " + result.getSkippedCount() + "."
        );
        if (result.getSkippedCount() > 0 && result.getMessages() != null && !result.getMessages().isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "errorMsg",
                    result.getMessages().stream().limit(3).collect(Collectors.joining(" | "))
            );
        }
        return "redirect:/timesheets?month=" + month + "&year=" + year;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam Integer month,
                         @RequestParam Integer year,
                         RedirectAttributes redirectAttributes) {
        timesheetService.delete(id);
        redirectAttributes.addFlashAttribute("successMsg", "Da xoa bang cham cong.");
        return "redirect:/timesheets?month=" + month + "&year=" + year;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }
}
