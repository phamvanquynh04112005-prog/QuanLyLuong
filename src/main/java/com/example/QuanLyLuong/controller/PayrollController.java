package com.example.QuanLyLuong.controller;

import java.time.LocalDate;
import java.util.List;

import com.example.QuanLyLuong.common.PaymentStatus;
import com.example.QuanLyLuong.dto.PayrollSearchCriteria;
import com.example.QuanLyLuong.dto.PayrollSearchSummary;
import com.example.QuanLyLuong.entity.Employee;
import com.example.QuanLyLuong.entity.Payroll;
import com.example.QuanLyLuong.service.DepartmentService;
import com.example.QuanLyLuong.service.PayrollService;
import com.example.QuanLyLuong.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payrolls")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final UserService userService;
    private final DepartmentService departmentService;

    @GetMapping
    public String list(@RequestParam(required = false) Integer month,
                       @RequestParam(required = false) Integer year,
                       Model model) {
        LocalDate now = LocalDate.now();
        int selectedMonth = month == null ? now.getMonthValue() : month;
        int selectedYear = year == null ? now.getYear() : year;

        model.addAttribute("payrolls", payrollService.findByMonth(selectedMonth, selectedYear));
        model.addAttribute("totalSalary", payrollService.getTotalPayrollAmount(selectedMonth, selectedYear));
        model.addAttribute("month", selectedMonth);
        model.addAttribute("year", selectedYear);
        model.addAttribute("pageTitle", "Bang luong");
        model.addAttribute("contentTemplate", "payroll/list");
        return "layout/base";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) Integer month,
                         @RequestParam(required = false) Integer year,
                         @RequestParam(required = false) Long departmentId,
                         @RequestParam(required = false) PaymentStatus paymentStatus,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(required = false) Double minSalary,
                         @RequestParam(required = false) Double maxSalary,
                         Model model) {
        PayrollSearchCriteria criteria = new PayrollSearchCriteria();
        criteria.setMonth(month);
        criteria.setYear(year);
        criteria.setDepartmentId(departmentId);
        criteria.setPaymentStatus(paymentStatus);
        criteria.setKeyword(keyword);
        criteria.setMinSalary(minSalary);
        criteria.setMaxSalary(maxSalary);

        List<Payroll> payrolls = payrollService.searchPayrolls(criteria);
        PayrollSearchSummary summary = payrollService.buildSearchSummary(payrolls);

        model.addAttribute("criteria", criteria);
        model.addAttribute("payrolls", payrolls);
        model.addAttribute("summary", summary);
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("paymentStatuses", PaymentStatus.values());
        model.addAttribute("pageTitle", "Tra cuu phieu luong");
        model.addAttribute("contentTemplate", "payroll/search");
        return "layout/base";
    }

    @PostMapping("/calculate")
    public String calculate(@RequestParam Integer month,
                            @RequestParam Integer year,
                            RedirectAttributes redirectAttributes) {
        int total = payrollService.calculateForAll(month, year).size();
        redirectAttributes.addFlashAttribute("successMsg", "Da tinh luong cho " + total + " nhan vien.");
        return "redirect:/payrolls?month=" + month + "&year=" + year;
    }

    @PostMapping("/pay/{id}")
    public String pay(@PathVariable Long id,
                      @RequestParam Integer month,
                      @RequestParam Integer year,
                      RedirectAttributes redirectAttributes) {
        payrollService.markAsPaid(id);
        redirectAttributes.addFlashAttribute("successMsg", "Da cap nhat trang thai chi luong.");
        return "redirect:/payrolls?month=" + month + "&year=" + year;
    }

    @GetMapping("/my")
    public String myPayroll(Authentication authentication, Model model) {
        Employee employee = userService.getEmployeeFromAuthentication(authentication);
        model.addAttribute("payrolls", employee == null ? java.util.List.of() : payrollService.findByEmployee(employee.getId()));
        model.addAttribute("employee", employee);
        model.addAttribute("pageTitle", "Phieu luong cua toi");
        model.addAttribute("contentTemplate", "payroll/my");
        return "layout/base";
    }
}