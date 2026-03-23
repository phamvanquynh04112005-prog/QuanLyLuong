package com.example.QuanLyLuong.controller;

import java.time.LocalDate;
import java.util.List;

import com.example.QuanLyLuong.dto.DashboardStats;
import com.example.QuanLyLuong.entity.Payroll;
import com.example.QuanLyLuong.service.DashboardService;
import com.example.QuanLyLuong.service.PayrollService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DashboardService dashboardService;
    private final PayrollService payrollService;

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        DashboardStats stats = dashboardService.buildStats(month, year);
        List<Payroll> recentPayrolls = payrollService.findByMonth(month, year).stream().limit(5).toList();

        model.addAttribute("stats", stats);
        model.addAttribute("recentPayrolls", recentPayrolls);
        model.addAttribute("currentMonth", month);
        model.addAttribute("currentYear", year);
        model.addAttribute("pageTitle", "Tong quan");
        model.addAttribute("contentTemplate", "dashboard");
        return "layout/base";
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("pageTitle", "Khong du quyen");
        model.addAttribute("errorTitle", "Truy cap bi tu choi");
        model.addAttribute("errorMessage", "Tai khoan hien tai khong du quyen thuc hien thao tac nay.");
        return "error";
    }
}
