package com.example.QuanLyLuong.controller;

import java.util.List;

import com.example.QuanLyLuong.common.EmployeeStatus;
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
        model.addAttribute("pageTitle", "Nhan vien");
        model.addAttribute("contentTemplate", "employee/list");
        return "layout/base";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        Employee employee = new Employee();
        employee.setDepartment(new Department());
        prepareForm(model, employee, "Them nhan vien");
        return "layout/base";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Employee employee = employeeService.findById(id);
        if (employee.getDepartment() == null) {
            employee.setDepartment(new Department());
        }
        prepareForm(model, employee, "Cap nhat nhan vien");
        return "layout/base";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Employee employee, RedirectAttributes redirectAttributes) {
        if (employee.getId() == null) {
            employeeService.save(employee);
            redirectAttributes.addFlashAttribute("successMsg", "Da them nhan vien moi.");
        } else {
            employeeService.update(employee.getId(), employee);
            redirectAttributes.addFlashAttribute("successMsg", "Da cap nhat thong tin nhan vien.");
        }
        return "redirect:/employees";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        employeeService.delete(id);
        redirectAttributes.addFlashAttribute("successMsg", "Da xoa nhan vien.");
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
