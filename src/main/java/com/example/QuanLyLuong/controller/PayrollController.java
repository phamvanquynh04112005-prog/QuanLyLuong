package com.example.QuanLyLuong.controller;

import java.time.LocalDate;
import java.util.List;

import com.example.QuanLyLuong.common.PaymentStatus;
import com.example.QuanLyLuong.common.Role;
import com.example.QuanLyLuong.dto.PayrollSearchCriteria;
import com.example.QuanLyLuong.dto.PayrollSearchSummary;
import com.example.QuanLyLuong.entity.Employee;
import com.example.QuanLyLuong.entity.Payroll;
import com.example.QuanLyLuong.entity.User;
import com.example.QuanLyLuong.service.DepartmentService;
import com.example.QuanLyLuong.service.PayrollMailService;
import com.example.QuanLyLuong.service.PayrollService;
import com.example.QuanLyLuong.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
    private final PayrollMailService payrollMailService;
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
        model.addAttribute("pageTitle", "Bảng lương");
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
        model.addAttribute("pageTitle", "Tra cứu phiếu lương");
        model.addAttribute("contentTemplate", "payroll/search");
        return "layout/base";
    }

    @PostMapping("/calculate")
    public String calculate(@RequestParam Integer month,
                            @RequestParam Integer year,
                            RedirectAttributes redirectAttributes) {
        int total = payrollService.calculateForAll(month, year).size();
        redirectAttributes.addFlashAttribute("successMsg", "Đã tính lương cho " + total + " nhân viên.");
        return "redirect:/payrolls?month=" + month + "&year=" + year;
    }

    @PostMapping("/pay/{id}")
    public String pay(@PathVariable Long id,
                      @RequestParam Integer month,
                      @RequestParam Integer year,
                      RedirectAttributes redirectAttributes) {
        payrollService.markAsPaid(id);
        redirectAttributes.addFlashAttribute("successMsg", "Đã cập nhật trạng thái chi lương.");
        return "redirect:/payrolls?month=" + month + "&year=" + year;
    }

    @PostMapping("/send-mail/{id}")
    public String sendMail(@PathVariable Long id,
                           @RequestParam Integer month,
                           @RequestParam Integer year,
                           RedirectAttributes redirectAttributes) {
        Payroll payroll = payrollService.findById(id);
        return sendMailInternal(payroll, month, year, redirectAttributes, "/payrolls?month=" + month + "&year=" + year);
    }

    @GetMapping("/send-mail/{id}")
    public String sendMailFallback(@PathVariable Long id,
                                   @RequestParam(required = false) Integer month,
                                   @RequestParam(required = false) Integer year,
                                   RedirectAttributes redirectAttributes) {
        Payroll payroll = payrollService.findById(id);
        int resolvedMonth = month != null ? month : payroll.getMonth();
        int resolvedYear = year != null ? year : payroll.getYear();
        return sendMailInternal(payroll, resolvedMonth, resolvedYear, redirectAttributes,
                "/payrolls?month=" + resolvedMonth + "&year=" + resolvedYear);
    }

    @GetMapping("/my")
    public String myPayroll(Authentication authentication, Model model) {
        Employee employee = userService.getEmployeeFromAuthentication(authentication);
        model.addAttribute("payrolls", employee == null ? java.util.List.of() : payrollService.findByEmployee(employee.getId()));
        model.addAttribute("employee", employee);
        model.addAttribute("pageTitle", "Phiếu lương của tôi");
        model.addAttribute("contentTemplate", "payroll/my");
        return "layout/base";
    }

    @GetMapping("/my/{id}")
    public String myPayrollDetail(@PathVariable Long id,
                                  Authentication authentication,
                                  Model model) {
        Payroll payroll = requireAccessiblePayroll(authentication, id);
        model.addAttribute("payroll", payroll);
        model.addAttribute("employee", payroll.getEmployee());
        model.addAttribute("pageTitle", "Chi tiết phiếu lương");
        model.addAttribute("contentTemplate", "payroll/detail");
        return "layout/base";
    }

    @PostMapping("/my/send-mail/{id}")
    public String sendMyPayrollMail(@PathVariable Long id,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        Payroll payroll = requireAccessiblePayroll(authentication, id);
        return sendMailInternal(payroll, payroll.getMonth(), payroll.getYear(), redirectAttributes, "/payrolls/my");
    }

    private String sendMailInternal(Payroll payroll,
                                    Integer month,
                                    Integer year,
                                    RedirectAttributes redirectAttributes,
                                    String redirectUrl) {
        if (payroll.getPaymentStatus() != PaymentStatus.PAID) {
            redirectAttributes.addFlashAttribute("errorMsg", "Chỉ gửi mail sau khi bảng lương đã ở trạng thái Đã chi.");
            return "redirect:" + redirectUrl;
        }
        try {
            payrollMailService.sendPayslipEmail(payroll);
            String recipient = payroll.getEmployee() != null ? payroll.getEmployee().getEmail() : "";
            redirectAttributes.addFlashAttribute("successMsg", "Đã gửi phiếu lương qua email: " + recipient);
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("errorMsg", "Gửi mail thất bại: " + exception.getMessage());
        }
        return "redirect:" + redirectUrl;
    }

    private Payroll requireAccessiblePayroll(Authentication authentication, Long payrollId) {
        Payroll payroll = payrollService.findById(payrollId);
        User currentUser = userService.getAuthenticatedUser(authentication);
        boolean hasPayrollAdminScope = currentUser.getRole() == Role.ROLE_ACCOUNTANT;
        if (!hasPayrollAdminScope
                && (currentUser.getEmployee() == null || payroll.getEmployee() == null
                || !currentUser.getEmployee().getId().equals(payroll.getEmployee().getId()))) {
            throw new AccessDeniedException("Bạn không đủ quyền truy cập phiếu lương này.");
        }
        return payroll;
    }
}
