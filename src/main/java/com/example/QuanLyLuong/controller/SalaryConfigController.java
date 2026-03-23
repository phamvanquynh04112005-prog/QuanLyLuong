package com.example.QuanLyLuong.controller;

import java.time.LocalDate;
import java.util.List;

import com.example.QuanLyLuong.entity.Employee;
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
    public String list(@RequestParam(required = false) Long employeeId, Model model) {
        List<SalaryConfig> salaryConfigs = employeeId == null
                ? salaryConfigService.findLatestForAllEmployees()
                : salaryConfigService.findHistoryByEmployee(employeeId);

        model.addAttribute("salaryConfigs", salaryConfigs);
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("historyView", employeeId != null);
        model.addAttribute("pageTitle", "Cau hinh luong");
        model.addAttribute("contentTemplate", "salary-config/list");
        return "layout/base";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long employeeId, Model model) {
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("defaultDate", LocalDate.now());
        model.addAttribute("pageTitle", "Them cau hinh luong");
        model.addAttribute("contentTemplate", "salary-config/form");
        return "layout/base";
    }

    @PostMapping("/save")
    public String save(@RequestParam Long employeeId,
                       @RequestParam(required = false) Double allowance,
                       @RequestParam(required = false) Double deduction,
                       @RequestParam(required = false) String description,
                       @RequestParam(required = false) LocalDate effectiveDate,
                       RedirectAttributes redirectAttributes) {
        salaryConfigService.save(employeeId, allowance, deduction, description, effectiveDate);
        redirectAttributes.addFlashAttribute("successMsg", "Da luu cau hinh luong.");
        return "redirect:/salary-configs";
    }
}
