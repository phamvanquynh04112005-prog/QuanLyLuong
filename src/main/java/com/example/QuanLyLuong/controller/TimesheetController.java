package com.example.QuanLyLuong.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import com.example.QuanLyLuong.common.AttendanceSource;
import com.example.QuanLyLuong.dto.AttendanceImportResult;
import com.example.QuanLyLuong.entity.AttendanceLog;
import com.example.QuanLyLuong.entity.Timesheet;
import com.example.QuanLyLuong.service.AttendanceLogService;
import com.example.QuanLyLuong.service.EmployeeService;
import com.example.QuanLyLuong.service.SalaryConfigService;
import com.example.QuanLyLuong.service.TimesheetService;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
    private final EmployeeService employeeService;
    private final AttendanceLogService attendanceLogService;
    private final SalaryConfigService salaryConfigService;

    @GetMapping
    public String list(@RequestParam(required = false) Integer month,
                       @RequestParam(required = false) Integer year,
                       @RequestParam(required = false, defaultValue = "month") String periodType,
                       Model model) {
        LocalDate now = LocalDate.now();
        int selectedMonth = month == null ? now.getMonthValue() : month;
        int selectedYear = year == null ? now.getYear() : year;
        String normalizedPeriodType = normalizePeriodType(periodType);

        List<Timesheet> timesheets = timesheetService.findAllByPeriod(selectedMonth, selectedYear, normalizedPeriodType);

        model.addAttribute("timesheets", timesheets);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("year", selectedYear);
        model.addAttribute("periodType", normalizedPeriodType);
        model.addAttribute("periodTitle", buildPeriodTitle(selectedMonth, selectedYear, normalizedPeriodType));
        model.addAttribute("pageTitle", "Ch\u1ea5m c\u00f4ng");
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
                .mapToDouble(log -> safeDouble(log.getOvertimeWeekdayHours())
                        + safeDouble(log.getOvertimeWeekendHours())
                        + safeDouble(log.getOvertimeHolidayHours()))
                .sum();

        model.addAttribute("timesheet", timesheet);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("year", selectedYear);
        model.addAttribute("logs", logs);
        model.addAttribute("totalOvertimeHours", round2(totalOvertimeHours));
        model.addAttribute("pageTitle", "Chi tiet cham cong");
        model.addAttribute("contentTemplate", "timesheet/detail");
        return "layout/base";
    }

    @GetMapping("/new")
    public String form(@RequestParam(required = false) Long employeeId,
                       @RequestParam(required = false) Integer month,
                       @RequestParam(required = false) Integer year,
                       Model model) {
        LocalDate now = LocalDate.now();
        int selectedMonth = month == null ? now.getMonthValue() : month;
        int selectedYear = year == null ? now.getYear() : year;
        YearMonth yearMonth = YearMonth.of(selectedYear, selectedMonth);

        Timesheet timesheet = employeeId == null
                ? new Timesheet()
                : timesheetService.findByEmployeeAndMonth(employeeId, selectedMonth, selectedYear).orElseGet(Timesheet::new);

        model.addAttribute("timesheet", timesheet);
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("year", selectedYear);
        model.addAttribute("attendanceLogs", employeeId == null
                ? List.of()
                : attendanceLogService.findByEmployeeAndMonth(employeeId, yearMonth));
        model.addAttribute("attendanceSources", AttendanceSource.values());
        model.addAttribute("salaryConfig", employeeId == null ? null : salaryConfigService.getEffectiveConfig(employeeId, yearMonth));
        model.addAttribute("pageTitle", "Ch\u1ea5m c\u00f4ng chi ti\u1ebft");
        model.addAttribute("contentTemplate", "timesheet/form");
        return "layout/base";
    }

    @PostMapping("/save")
    public String save(@RequestParam Long employeeId,
                       @RequestParam Integer month,
                       @RequestParam Integer year,
                       @RequestParam Integer workDays,
                       @RequestParam(defaultValue = "0") Integer leaveDays,
                       @RequestParam(required = false) String note,
                       RedirectAttributes redirectAttributes) {
        int maxDays = YearMonth.of(year, month).lengthOfMonth();
        if (workDays < 0 || leaveDays < 0) {
            redirectAttributes.addFlashAttribute("errorMsg", "Ng\u00e0y c\u00f4ng v\u00e0 ng\u00e0y ph\u00e9p kh\u00f4ng \u0111\u01b0\u1ee3c \u00e2m.");
            return "redirect:/timesheets/new?employeeId=" + employeeId + "&month=" + month + "&year=" + year;
        }
        if (workDays + leaveDays > maxDays) {
            redirectAttributes.addFlashAttribute("errorMsg", "T\u1ed5ng ng\u00e0y c\u00f4ng v\u00e0 ng\u00e0y ph\u00e9p kh\u00f4ng \u0111\u01b0\u1ee3c v\u01b0\u1ee3t qu\u00e1 " + maxDays + " ng\u00e0y.");
            return "redirect:/timesheets/new?employeeId=" + employeeId + "&month=" + month + "&year=" + year;
        }

        timesheetService.saveOrUpdate(employeeId, month, year, workDays, leaveDays, note);
        redirectAttributes.addFlashAttribute("successMsg", "\u0110\u00e3 l\u01b0u b\u1ea3ng t\u1ed5ng h\u1ee3p ch\u1ea5m c\u00f4ng.");
        return "redirect:/timesheets/new?employeeId=" + employeeId + "&month=" + month + "&year=" + year;
    }

    @PostMapping("/logs/save")
    public String saveLog(@RequestParam Long employeeId,
                          @RequestParam Integer month,
                          @RequestParam Integer year,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate attendanceDate,
                          @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime checkInTime,
                          @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime checkOutTime,
                          @RequestParam(required = false) Double regularHours,
                          @RequestParam(required = false) Double overtimeWeekdayHours,
                          @RequestParam(required = false) Double overtimeWeekendHours,
                          @RequestParam(required = false) Double overtimeHolidayHours,
                          @RequestParam(required = false) Integer lateMinutes,
                          @RequestParam(required = false) AttendanceSource source,
                          @RequestParam(required = false) String machineCode,
                          @RequestParam(required = false) String note,
                          RedirectAttributes redirectAttributes) {
        YearMonth selectedMonth = YearMonth.of(year, month);
        if (!YearMonth.from(attendanceDate).equals(selectedMonth)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Ng\u00e0y ch\u1ea5m c\u00f4ng ph\u1ea3i n\u1eb1m trong th\u00e1ng \u0111ang thao t\u00e1c.");
            return "redirect:/timesheets/new?employeeId=" + employeeId + "&month=" + month + "&year=" + year;
        }

        attendanceLogService.saveLog(
                employeeId,
                attendanceDate,
                checkInTime,
                checkOutTime,
                regularHours,
                overtimeWeekdayHours,
                overtimeWeekendHours,
                overtimeHolidayHours,
                lateMinutes,
                source == null ? AttendanceSource.EXCEL_IMPORT : source,
                machineCode,
                note
        );
        redirectAttributes.addFlashAttribute("successMsg", "\u0110\u00e3 l\u01b0u log ch\u1ea5m c\u00f4ng ng\u00e0y " + attendanceDate + ".");
        return "redirect:/timesheets/new?employeeId=" + employeeId + "&month=" + month + "&year=" + year;
    }

    @PostMapping("/import")
    public String importExcel(@RequestParam Integer month,
                              @RequestParam Integer year,
                              @RequestParam(defaultValue = "month") String periodType,
                              @RequestParam(defaultValue = "EXCEL_IMPORT") AttendanceSource source,
                              @RequestParam MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        AttendanceImportResult result = attendanceLogService.importFromExcel(file, YearMonth.of(year, month), source);
        redirectAttributes.addFlashAttribute(
                "successMsg",
                "Import xong: m\u1edbi " + result.getImportedCount() + ", c\u1eadp nh\u1eadt " + result.getUpdatedCount() + ", b\u1ecf qua " + result.getSkippedCount() + "."
        );
        if (result.getSkippedCount() > 0 && result.getMessages() != null && !result.getMessages().isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "errorMsg",
                    result.getMessages().stream().limit(3).collect(Collectors.joining(" | "))
            );
        }
        String normalizedPeriodType = normalizePeriodType(periodType);
        return "redirect:/timesheets?month=" + month + "&year=" + year + "&periodType=" + normalizedPeriodType;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam Integer month,
                         @RequestParam Integer year,
                         @RequestParam(defaultValue = "month") String periodType,
                         RedirectAttributes redirectAttributes) {
        timesheetService.delete(id);
        redirectAttributes.addFlashAttribute("successMsg", "\u0110\u00e3 x\u00f3a b\u1ea3ng ch\u1ea5m c\u00f4ng.");
        String normalizedPeriodType = normalizePeriodType(periodType);
        return "redirect:/timesheets?month=" + month + "&year=" + year + "&periodType=" + normalizedPeriodType;
    }

    private String normalizePeriodType(String periodType) {
        if (periodType == null || periodType.isBlank()) {
            return "month";
        }
        String normalized = periodType.trim().toLowerCase();
        return switch (normalized) {
            case "quarter", "year" -> normalized;
            default -> "month";
        };
    }

    private String buildPeriodTitle(int month, int year, String periodType) {
        return switch (periodType) {
            case "quarter" -> "Ch\u1ea5m c\u00f4ng qu\u00fd " + (((month - 1) / 3) + 1) + "/" + year;
            case "year" -> "Ch\u1ea5m c\u00f4ng n\u0103m " + year;
            default -> "Ch\u1ea5m c\u00f4ng th\u00e1ng " + month + "/" + year;
        };
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
