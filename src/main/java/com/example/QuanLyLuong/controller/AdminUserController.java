package com.example.QuanLyLuong.controller;

import java.util.List;

import com.example.QuanLyLuong.common.Role;
import com.example.QuanLyLuong.dto.AccountProvisionResult;
import com.example.QuanLyLuong.dto.BulkAccountProvisionResult;
import com.example.QuanLyLuong.entity.User;
import com.example.QuanLyLuong.service.EmployeeService;
import com.example.QuanLyLuong.service.UserService;

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
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final EmployeeService employeeService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "false") boolean missingOnly, Model model) {
        model.addAttribute("employees", userService.findEmployeesForAccountManagement(missingOnly));
        model.addAttribute("standaloneUsers", userService.findStandaloneUsers());
        model.addAttribute("missingOnly", missingOnly);
        model.addAttribute("missingAccountCount", userService.countEmployeesWithoutAccount());
        model.addAttribute("pageTitle", "Quản lý tài khoản");
        model.addAttribute("contentTemplate", "admin/user-list");
        return "layout/base";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        prepareForm(model, new User(), null, "Tạo tài khoản");
        return "layout/base";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        Long employeeId = user.getEmployee() != null ? user.getEmployee().getId() : null;
        prepareForm(model, user, employeeId, "Cập nhật tài khoản");
        return "layout/base";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam(required = false) Long employeeId,
                       @RequestParam String username,
                       @RequestParam(required = false) String password,
                       @RequestParam Role role,
                       @RequestParam(defaultValue = "false") boolean enabled,
                       RedirectAttributes redirectAttributes) {
        if (id == null) {
            if (password == null || password.isBlank()) {
                throw new IllegalArgumentException("Mật khẩu không được để trống khi tạo mới.");
            }
            userService.createUser(employeeId, username, password, role, enabled);
            redirectAttributes.addFlashAttribute("successMsg", "Đã tạo tài khoản mới.");
        } else {
            userService.updateUser(id, employeeId, username, password, role, enabled);
            redirectAttributes.addFlashAttribute("successMsg", "Đã cập nhật tài khoản.");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/provision-all")
    public String provisionAllMissingAccounts(RedirectAttributes redirectAttributes) {
        BulkAccountProvisionResult result = userService.provisionAccountsForEmployeesWithoutAccount();
        if (result.getCreatedCount() == 0) {
            redirectAttributes.addFlashAttribute("successMsg", "Tất cả nhân viên đã có tài khoản.");
            return "redirect:/admin/users";
        }

        redirectAttributes.addFlashAttribute(
                "successMsg",
                "Đã cấp " + result.getCreatedCount() + " tài khoản mới. Gửi email thành công "
                        + result.getEmailedCount() + "/" + result.getCreatedCount() + " nhân viên."
        );
        if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMsg", summarizeWarnings(result.getWarnings()));
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/provision/{employeeId}")
    public String provisionSingleAccount(@PathVariable Long employeeId, RedirectAttributes redirectAttributes) {
        AccountProvisionResult result = userService.provisionAccountForEmployee(employeeId);
        String username = result.getUser() != null ? result.getUser().getUsername() : "";
        String employeeName = result.getEmployee() != null ? result.getEmployee().getFullName() : "nhân viên";

        redirectAttributes.addFlashAttribute(
                "successMsg",
                result.isEmailSent()
                        ? "Đã cấp tài khoản " + username + " và gửi email cho " + employeeName + "."
                        : "Đã cấp tài khoản " + username + " cho " + employeeName + "."
        );
        if (!result.isEmailSent()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Không gửi được email tài khoản: " + result.getEmailError());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User updatedUser = userService.toggleEnabled(id);
        redirectAttributes.addFlashAttribute(
                "successMsg",
                Boolean.TRUE.equals(updatedUser.getEnabled())
                        ? "Đã mở khóa tài khoản."
                        : "Đã khóa tài khoản."
        );
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.delete(id);
        redirectAttributes.addFlashAttribute("successMsg", "Đã xóa tài khoản.");
        return "redirect:/admin/users";
    }

    private void prepareForm(Model model, User user, Long employeeId, String pageTitle) {
        model.addAttribute("user", user);
        model.addAttribute("employeeId", employeeId);
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("roles", Role.values());
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("contentTemplate", "admin/user-form");
    }

    private String summarizeWarnings(List<String> warnings) {
        return warnings.stream()
                .limit(5)
                .reduce((left, right) -> left + " | " + right)
                .orElse(null);
    }
}
