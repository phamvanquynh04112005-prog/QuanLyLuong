package com.example.QuanLyLuong.controller;

import java.time.LocalDate;
import java.util.List;

import com.example.QuanLyLuong.common.CompensationItemType;
import com.example.QuanLyLuong.entity.CompensationItem;
import com.example.QuanLyLuong.service.CompensationItemService;
import com.example.QuanLyLuong.service.EmployeeService;

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
@RequestMapping("/compensation-items")
@RequiredArgsConstructor
public class CompensationItemController {

    private final CompensationItemService compensationItemService;
    private final EmployeeService employeeService;

    @GetMapping
    public String list(@RequestParam(required = false) Long employeeId, Model model) {
        List<CompensationItem> items = compensationItemService.findAll(employeeId);
        model.addAttribute("items", items);
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("types", CompensationItemType.values());
        model.addAttribute("defaultDate", LocalDate.now());
        model.addAttribute("pageTitle", "Phu cap va thuong dong");
        model.addAttribute("contentTemplate", "compensation-item/list");
        return "layout/base";
    }

    @PostMapping("/save")
    public String save(@RequestParam Long employeeId,
                       @RequestParam CompensationItemType componentType,
                       @RequestParam String name,
                       @RequestParam Double amount,
                       @RequestParam(required = false) Boolean taxable,
                       @RequestParam(required = false) Boolean recurring,
                       @RequestParam(required = false) Boolean active,
                       @RequestParam(required = false) LocalDate effectiveDate,
                       @RequestParam(required = false) LocalDate endDate,
                       @RequestParam(required = false) String note,
                       RedirectAttributes redirectAttributes) {
        compensationItemService.save(
                employeeId,
                componentType,
                name,
                amount,
                taxable,
                recurring,
                active,
                effectiveDate,
                endDate,
                note
        );
        redirectAttributes.addFlashAttribute("successMsg", "Da luu cau phan thu nhap/khau tru dong.");
        return "redirect:/compensation-items?employeeId=" + employeeId;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) Long employeeId,
                         RedirectAttributes redirectAttributes) {
        compensationItemService.delete(id);
        redirectAttributes.addFlashAttribute("successMsg", "Da xoa cau phan dong.");
        return employeeId == null
                ? "redirect:/compensation-items"
                : "redirect:/compensation-items?employeeId=" + employeeId;
    }
}