package com.example.QuanLyLuong.controller;

import java.time.LocalDate;
import java.util.List;

import com.example.QuanLyLuong.common.EmployeeStatus;
import com.example.QuanLyLuong.dto.EmployeeImportResult;
import com.example.QuanLyLuong.entity.Department;
import com.example.QuanLyLuong.entity.Employee;
import com.example.QuanLyLuong.service.DepartmentService;
import com.example.QuanLyLuong.service.EmployeeService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword, Model model) {
        List<Employee> employees = (keyword != null && !keyword.isBlank())
                ? employeeService.searchByName(keyword)
                : employeeService.findAll();

        model.addAttribute("employees", employees);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageTitle", "Nhân viên");
        model.addAttribute("contentTemplate", "employee/list");
        return "layout/base";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        Employee employee = new Employee();
        employee.setDepartment(new Department());
        employee.setEmployeeCode(employeeService.generateNextEmployeeCode());
        employee.setJoinDate(LocalDate.now());
        prepareForm(model, employee, "Thêm nhân viên");
        return "layout/base";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Employee employee = employeeService.findById(id);
        if (employee.getDepartment() == null) {
            employee.setDepartment(new Department());
        }
        if (employee.getJoinDate() == null) {
            employee.setJoinDate(LocalDate.now());
        }
        prepareForm(model, employee, "Cập nhật nhân viên");
        return "layout/base";
    }

    @GetMapping("/import")
    public String importForm(Model model) {
        model.addAttribute("pageTitle", "Import nhân viên");
        model.addAttribute("contentTemplate", "employee/import");
        return "layout/base";
    }

    @PostMapping("/import")
    public String importExcel(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        EmployeeImportResult result = employeeService.importFromExcel(file);
        String summary = "Import nhân viên: thêm mới " + result.getImportedCount()
                + ", cập nhật " + result.getUpdatedCount()
                + ", bỏ qua " + result.getSkippedCount() + ".";

        if (result.getImportedCount() > 0 || result.getUpdatedCount() > 0) {
            redirectAttributes.addFlashAttribute("successMsg", summary);
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", summary);
        }

        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            String details = result.getMessages().stream()
                    .limit(5)
                    .reduce((left, right) -> left + " | " + right)
                    .orElse(null);
            if (details != null && !details.isBlank()) {
                redirectAttributes.addFlashAttribute("errorMsg", details);
            }
        }
        return "redirect:/employees";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Employee employee, RedirectAttributes redirectAttributes) {
        if (employee.getId() == null) {
            employeeService.save(employee);
            redirectAttributes.addFlashAttribute("successMsg", "Đã thêm nhân viên mới.");
        } else {
            employeeService.update(employee.getId(), employee);
            redirectAttributes.addFlashAttribute("successMsg", "Đã cập nhật thông tin nhân viên.");
        }
        return "redirect:/employees";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        employeeService.delete(id);
        redirectAttributes.addFlashAttribute("successMsg", "Đã xóa nhân viên.");
        return "redirect:/employees";
    }

    private void prepareForm(Model model, Employee employee, String pageTitle) {
        model.addAttribute("employee", employee);
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("statuses", EmployeeStatus.values());
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("contentTemplate", "employee/form");
    }
}
