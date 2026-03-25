package com.example.QuanLyLuong.controller;

import java.time.LocalDate;
import java.util.List;

import com.example.QuanLyLuong.entity.SalaryConfig;
import com.example.QuanLyLuong.service.EmployeeService;
import com.example.QuanLyLuong.service.SalaryConfigService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/salary-configs")
@RequiredArgsConstructor
public class SalaryConfigController {

    private final SalaryConfigService salaryConfigService;
    private final EmployeeService employeeService;

    @GetMapping
    public String list(@RequestParam(required = false) Long employeeId,
                       @RequestParam(required = false) String keyword,
                       Model model) {
        List<SalaryConfig> salaryConfigs = employeeId == null
                ? salaryConfigService.findLatestForAllEmployees(keyword)
                : salaryConfigService.findHistoryByEmployee(employeeId);

        model.addAttribute("salaryConfigs", salaryConfigs);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("historyView", employeeId != null);
        model.addAttribute("pageTitle", "C\u1ea5u h\u00ecnh l\u01b0\u01a1ng");
        model.addAttribute("contentTemplate", "salary-config/list");
        return "layout/base";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long employeeId, Model model) {
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("defaultDate", LocalDate.now());
        model.addAttribute("pageTitle", "Th\u00eam c\u1ea5u h\u00ecnh l\u01b0\u01a1ng");
        model.addAttribute("contentTemplate", "salary-config/form");
        return "layout/base";
    }

    @PostMapping("/save")
    public String save(@RequestParam Long employeeId,
                       @RequestParam(required = false) Double allowance,
                       @RequestParam(required = false) Double deduction,
                       @RequestParam(required = false) Integer standardWorkDays,
                       @RequestParam(required = false) Double standardWorkHoursPerDay,
                       @RequestParam(required = false) Double overtimeWeekdayMultiplier,
                       @RequestParam(required = false) Double overtimeWeekendMultiplier,
                       @RequestParam(required = false) Double overtimeHolidayMultiplier,
                       @RequestParam(required = false) Double socialInsuranceRate,
                       @RequestParam(required = false) Double healthInsuranceRate,
                       @RequestParam(required = false) Double unemploymentInsuranceRate,
                       @RequestParam(required = false) Double personalDeduction,
                       @RequestParam(required = false) Double dependentDeductionPerPerson,
                       @RequestParam(required = false) String description,
                       @RequestParam(required = false) LocalDate effectiveDate,
                       RedirectAttributes redirectAttributes) {
        salaryConfigService.save(
                employeeId,
                allowance,
                deduction,
                standardWorkDays,
                standardWorkHoursPerDay,
                overtimeWeekdayMultiplier,
                overtimeWeekendMultiplier,
                overtimeHolidayMultiplier,
                socialInsuranceRate,
                healthInsuranceRate,
                unemploymentInsuranceRate,
                personalDeduction,
                dependentDeductionPerPerson,
                description,
                effectiveDate
        );
        redirectAttributes.addFlashAttribute("successMsg", "\u0110\u00e3 l\u01b0u c\u1ea5u h\u00ecnh l\u01b0\u01a1ng.");
        return "redirect:/salary-configs";
    }
}
