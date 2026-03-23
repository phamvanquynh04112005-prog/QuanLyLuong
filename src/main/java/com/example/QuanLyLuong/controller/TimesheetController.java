package com.example.QuanLyLuong.controller;

import java.time.LocalDate;
import java.time.YearMonth;

import com.example.QuanLyLuong.dto.TimesheetSummary;
import com.example.QuanLyLuong.entity.Timesheet;
import com.example.QuanLyLuong.service.EmployeeService;
import com.example.QuanLyLuong.service.TimesheetService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/timesheets")
@RequiredArgsConstructor
public class TimesheetController {

    private final TimesheetService timesheetService;
    private final EmployeeService employeeService;

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

        Timesheet timesheet = employeeId == null
                ? new Timesheet()
                : timesheetService.findByEmployeeAndMonth(employeeId, selectedMonth, selectedYear).orElseGet(Timesheet::new);

        model.addAttribute("timesheet", timesheet);
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("year", selectedYear);
        model.addAttribute("standardWorkDays", 26);
        model.addAttribute("pageTitle", "Nhap cham cong");
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
        redirectAttributes.addFlashAttribute("successMsg", "Da luu bang cham cong.");
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
