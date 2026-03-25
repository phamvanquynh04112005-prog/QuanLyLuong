package com.example.QuanLyLuong.controller;

import com.example.QuanLyLuong.common.Role;
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
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("pageTitle", "Tai khoan he thong");
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
}

