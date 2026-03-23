package com.example.QuanLyLuong.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.stream.Collectors;

import com.example.QuanLyLuong.common.AttendanceSource;
import com.example.QuanLyLuong.dto.AttendanceImportResult;
import com.example.QuanLyLuong.dto.TimesheetSummary;
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
                       Model model) {
        LocalDate now = LocalDate.now();
        int selectedMonth = month == null ? now.getMonthValue() : month;
        int selectedYear = year == null ? now.getYear() : year;
        TimesheetSummary summary = timesheetService.getMonthSummary(selectedMonth, selectedYear);

        model.addAttribute("timesheets", timesheetService.findAllByMonth(selectedMonth, selectedYear));
        model.addAttribute("summary", summary);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("year", selectedYear);
        model.addAttribute("attendanceSources", AttendanceSource.values());
        model.addAttribute("pageTitle", "Cham cong");
        model.addAttribute("contentTemplate", "timesheet/list");
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
        model.addAttribute("attendanceLogs", employeeId == null ? java.util.List.of() : attendanceLogService.findByEmployeeAndMonth(employeeId, yearMonth));
        model.addAttribute("attendanceSources", AttendanceSource.values());
        model.addAttribute("salaryConfig", employeeId == null ? null : salaryConfigService.getEffectiveConfig(employeeId, yearMonth));
        model.addAttribute("pageTitle", "Cham cong chi tiet");
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
            redirectAttributes.addFlashAttribute("errorMsg", "Ngay cong va ngay phep khong duoc am.");
            return "redirect:/timesheets/new?employeeId=" + employeeId + "&month=" + month + "&year=" + year;
        }
        if (workDays + leaveDays > maxDays) {
            redirectAttributes.addFlashAttribute("errorMsg", "Tong ngay cong va ngay phep khong duoc vuot qua " + maxDays + " ngay.");
            return "redirect:/timesheets/new?employeeId=" + employeeId + "&month=" + month + "&year=" + year;
        }

        timesheetService.saveOrUpdate(employeeId, month, year, workDays, leaveDays, note);
        redirectAttributes.addFlashAttribute("successMsg", "Da luu bang tong hop cham cong.");
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
            redirectAttributes.addFlashAttribute("errorMsg", "Ngay cham cong phai nam trong thang dang thao tac.");
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
                source == null ? AttendanceSource.MANUAL : source,
                machineCode,
                note
        );
        redirectAttributes.addFlashAttribute("successMsg", "Da luu log cham cong ngay " + attendanceDate + ".");
        return "redirect:/timesheets/new?employeeId=" + employeeId + "&month=" + month + "&year=" + year;
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

    @PostMapping("/bulk-init")
    public String bulkInit(@RequestParam Integer month,
                           @RequestParam Integer year,
                           RedirectAttributes redirectAttributes) {
        int count = timesheetService.initForAllEmployees(month, year);
        redirectAttributes.addFlashAttribute("successMsg", "Da khoi tao " + count + " bang cham cong.");
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
}